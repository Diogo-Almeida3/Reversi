package pt.isec.amov.reversi.game

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.game.jsonClasses.ProfileData
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread


private const val LINE_SIZE = 5
private const val MARGIN_PIECE = 8
private const val MARGIN_HIGHLIGHT = 32

const val SERVER_PORT = 9999

class BoardView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        const val NORMAL_PIECE = 0
        const val BOMB_PIECE = 1
        const val EXCHANGE_PIECE = 2
    }

    private var windowHeight = 0
    private var windowWidth = 0
    private var pieceHeight = 0
    private var pieceWidth = 0
    private var boardSIZE = 0

    private var endGame = false
    private var counter = 0

    private var exchangeArrayList = ArrayList<PieceMoves>()
    private var exchangeCounter = 0

    private lateinit var boardGame: BoardGame
    private lateinit var gamePerfilView: GamePerfilView
    private lateinit var auth: FirebaseAuth


    enum class State {
        STARTING, SETTING_PROFILE_DATA, PLAYING_ME, PLAYING_OTHER, GAME_OVER
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, CLIENT_CONNECTING, CONNECTION_ESTABLISHED,
        CONNECTION_ERROR, CONNECTION_ENDED
    }

    val state = MutableLiveData(State.STARTING)

    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    var move: PieceMoves? = null

    private var socket: Socket? = null
    private val socketI: InputStream?
        get() = socket?.getInputStream()
    private val socketO: OutputStream?
        get() = socket?.getOutputStream()

    private var db = Firebase.firestore
    private var serverSocket: ServerSocket? = null
    private var threadComm: Thread? = null
    private var isServer = false

    fun setData(boardGame: BoardGame, gameProfileView: GamePerfilView) {
        this.boardGame = boardGame
        this.gamePerfilView = gameProfileView
        boardSIZE = boardGame.getBoardSize()
        exchangeCounter = 0

        /* Multiplayer */
        move = null
        isServer = false
        auth = Firebase.auth
        //state.postValue(State.SETTING_PROFILE_DATA)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = (event?.x?.div(pieceWidth))?.toInt()
        val y = (event?.y?.div(pieceHeight))?.toInt()
        counter = 0
        when (boardGame.getGameMode()) {
            0 -> {
                if (!endGame) {
                    when (boardGame.getCurrentPiece()) {
                        NORMAL_PIECE -> {
                            if (boardGame.confirmMove(x!!, y!!)) {
                                boardGame.move(x, y)
                                boardGame.checkBoardPieces()

                                if (boardGame.checkEndGame())
                                    alertEndGame("Acabou o jogo. Tabuleiro Preenchido")
                                else
                                    checkAlertNoPlays()
                            }
                        }

                        BOMB_PIECE -> {
                            if (boardGame.confirmBombMove(x!!, y!!)) {
                                boardGame.pieceBomb(x, y)
                                boardGame.checkBoardPieces()
                                checkAlertNoPlays()
                            } else
                                showAlertExchange("You can only select your piece color")
                        }

                        EXCHANGE_PIECE -> {
                            when (boardGame.confirmExchangeMove(
                                x!!,
                                y!!,
                                exchangeCounter,
                                exchangeArrayList
                            )) {
                                -3 -> showAlertExchange("You can't choose the same pieces twice")
                                -2 -> showAlertExchange("Wrong Piece selected")
                                -1 -> showAlertExchange("You can't choose a position outside the board")
                                1 -> {
                                    exchangeArrayList.add(PieceMoves(x, y))
                                    exchangeCounter++

                                    if (exchangeCounter >= 3) {
                                        boardGame.exchangePiece(exchangeArrayList)
                                        exchangeCounter = 0
                                        boardGame.checkBoardPieces()
                                        checkAlertNoPlays()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {

                if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED)
                    return super.onTouchEvent(event)

                //Cliente
                if (!isServer) {

                    move = PieceMoves(x!!, y!!)
                    socketO?.run {
                        thread {
                            //todo Passa a jogada para um formato json e envia ao servidor para ver se está tudo ok
                            val jsonObj = JsonObject()
                            jsonObj.addProperty("x", x)
                            jsonObj.addProperty("y", y)
                            //write(jsonObj.toString().toByteArray())
                            val printStream = PrintStream(this)
                            printStream.println(jsonObj)
                            printStream.flush()
                        }

                    }

                }

                //Se for o servidor
                //Verificar se é a sua vez de jogar
                //Verifica logo a jogada
                //E devolve ao cliente caso nao ocorra nenhum erro

                //Se for o cliente
                //Verificar se é a sua vez de jogar
                //Quando joga envia ao server o movimento e o tipo de peça?
                //O server faz a verificaçao
            }
        }


        return super.onTouchEvent(event)
    }

    fun startClient(name: String , serverIP: String, serverPort: Int = SERVER_PORT) {

        //Não interessa se já há server ou não pq nós somos clientes vamos abrir o socket e ficar a espera
        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return

        thread {
            try {
                val clientSocket = Socket()
                clientSocket.connect(InetSocketAddress(serverIP, serverPort), 5000)
                startComs(clientSocket)

                Log.v("COMMS", "A thread cooms foi iniciada para o cliente")
                socketO?.run {
                    thread {
                        val uri = File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")

                        val profileData = ProfileData(name,convertToBase64(uri))

                        val gson = Gson()
                        val jsonSend:String = gson.toJson(profileData)


                        val printStream = PrintStream(this)
                        printStream.println(jsonSend)
                        printStream.flush()
                    }
                }
            } catch (_: Exception) {
                stopGame()
            }
        }
    }

    fun startServer(name: String) {
        if (serverSocket != null || socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return

        connectionState.postValue(ConnectionState.SERVER_CONNECTING)

        isServer = true
        val uri = File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")
        gamePerfilView.setUsersProfileData(name,convertToBase64(uri))

        thread {
            serverSocket = ServerSocket(SERVER_PORT)
            serverSocket?.run{
                try {
                    val newServerSocket = serverSocket!!.accept()
                    startComs(newServerSocket)
                    Log.v("COMMS", "A thread cooms foi iniciada para o $isServer")
                } catch (_: Exception) {

                    connectionState.postValue(ConnectionState.CONNECTION_ERROR)
                } finally {
                    serverSocket?.close()
                    serverSocket = null
                }
            }
        }
    }

    fun convertToBase64(attachment: File): String {
        return Base64.encodeToString(attachment.readBytes(), Base64.NO_WRAP)
    }



    private fun startComs(newSocket: Socket?) {
        //Aqui vamos receber um socket que será usado para toda a comunicação e atribuiremo lo à propriedade socket

        if(threadComm != null)
            return

        socket = newSocket

        //Esta thread vai ficar a correr permantemente ate o jogo acabar e for necessario reiniciar
        threadComm = thread {
            try{
                if(socketI == null)
                    return@thread

                //Estabelece-se a conexão
                connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)

                val bufInput = socketI!!.bufferedReader()

                while(state.value != State.GAME_OVER){
                    val message = bufInput.readLine()
                    val jsonObject = JsonParser().parse(message).asJsonObject
                    val type = jsonObject.get("type")


                    if(isServer){
                        if(type.toString().equals("\"PROFILE\"")) {
                            gamePerfilView.setUsersProfileData(jsonObject.get("name").toString(),jsonObject.get("photo").toString())
                            gamePerfilView.invalidate()
                        } else{
                            isServer = false
                        }
                    } else{

                    }
                }
            } catch (_: Exception) {
            } finally {
                stopGame()
            }
        }

    }

    fun stopGame() {
        try {
            state.postValue(State.GAME_OVER)

            Toast.makeText(context,"PARAR JOGO",Toast.LENGTH_LONG).show()
            connectionState.postValue(ConnectionState.CONNECTION_ERROR)
            socket?.close()
            socket = null
            threadComm?.interrupt()
            threadComm = null
        } catch (_: Exception) { }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        serverSocket = null
    }


















    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* Tamanho da janela */
        windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        windowWidth = MeasureSpec.getSize(widthMeasureSpec)

        /* Tamanho das peças */
        pieceHeight = (windowHeight - LINE_SIZE) / boardSIZE
        pieceWidth = (windowWidth - LINE_SIZE) / boardSIZE

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun drawBoard(canvas: Canvas?) {
        for (i in 0 until boardSIZE)
            for (j in 0 until boardSIZE) {
                val pieceType = boardGame.getPiece(i, j)
                if (pieceType != 0)
                    drawPiece(canvas, i, j, pieceType)
            }

    }

    private fun drawGrid(canvas: Canvas?) {
        val gridPaint = getGridPaint()

        drawCellColor(canvas)
        for (i in -1..boardSIZE) {
            /* Horizontal */
            var low = pieceHeight * (i + 1)
            canvas?.drawRect(
                0f,
                low.toFloat(),
                width.toFloat() - MARGIN_PIECE,
                (low + LINE_SIZE).toFloat(),
                gridPaint
            )

            /* Vertical */
            low = pieceWidth * (i + 1)
            canvas?.drawRect(
                low.toFloat(),
                0f,
                ((low + LINE_SIZE).toFloat()),
                height.toFloat() - MARGIN_PIECE,
                gridPaint
            )
        }
    }

    private fun drawCellColor(canvas: Canvas?) {
        for (i in 0 until boardSIZE) {
            for (j in 0 until boardSIZE) {
                val left = (pieceWidth * i)
                val top = (pieceHeight * j)
                val right = (pieceWidth * i) + pieceWidth
                val bottom = (pieceHeight * j) + pieceHeight
                canvas?.drawRect(
                    left.toFloat(),
                    top.toFloat(),
                    right.toFloat(),
                    bottom.toFloat(),
                    getCellPaint()
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {

        drawGrid(canvas) //Constrói o grid
        drawBoard(canvas) // Constrói as peças iniciais
        drawHighlightValidPlays(
            canvas,
            boardGame.highlightValidPlays()
        ) // constroi possiveis jogadas

    }

    private fun updateView() {
        gamePerfilView.invalidate()
        invalidate()
    }

    private fun alertEndGame(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        showAlertEndGame(boardGame.checkWinner())
        updateView()
        endGame = true
    }

    private fun checkAlertNoPlays(): Boolean {
        //Muda o jogador a jogar
        boardGame.switchPlayer()

        //Verifica se ele tem jogadas disponiveis se sim saimos daqui e continuamos
        if (boardGame.checkNoValidPlays()) {
            updateView()
            return true
        }

        if (counter == 0)
            updateView()

        //Senao mostra o alert e resolve o resto lá dentro
        showAlertPassPlay(boardGame.getName())
        return false

    }

    private fun showAlertEndGame(player: Player?) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)

        if (player != null)
            builder1.setMessage("${player.getUsername()} win the game.")
        else
            builder1.setMessage("Draw")
        builder1.setCancelable(false)

        builder1.setPositiveButton("Check Board") { dialog, id ->
            run {
                dialog.cancel()
            }
        }

        builder1.setNegativeButton("Back to menu") { dialog, id ->
            run {
                dialog.cancel()
                findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun showAlertExchange(phrase: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(phrase)
        builder1.setCancelable(false)

        builder1.setPositiveButton("Pass") { dialog, id ->
            run {
                dialog.cancel()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun showAlertPassPlay(name: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(name + " has no available plays..")
        builder1.setCancelable(false)

        builder1.setPositiveButton("Pass") { dialog, id ->
            run {
                dialog.cancel()
                ++counter
                if (counter < boardGame.getPlayers()) //Quando for igual já sabemos que percorreu os jogadores todos portanto não vale apena fazer o igual
                    checkAlertNoPlays()
                else
                    alertEndGame("Acabou o jogo. Não existem jogadas disponiveis")
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun drawHighlightValidPlays(
        canvas: Canvas?,
        highlightValidPlays: ArrayList<PieceMoves>
    ) {
        for (i in 0 until highlightValidPlays.size) {
            val centerX = (pieceWidth * highlightValidPlays[i].getX()) + pieceWidth / 2
            val centerY = (pieceHeight * highlightValidPlays[i].getY()) + pieceHeight / 2
            val radius = Math.min(pieceWidth, pieceHeight) / 2 - MARGIN_HIGHLIGHT
            canvas?.drawCircle(
                centerX.toFloat(),
                centerY.toFloat(),
                radius.toFloat(),
                getHighlightPlayPaint()
            )
            canvas?.drawCircle(
                centerX.toFloat(),
                centerY.toFloat(),
                radius.toFloat(),
                getHighlightPlayStrokePaint()
            )
        }
    }

    private fun getGridPaint(): Paint {
        return Paint(Paint.DITHER_FLAG and Paint.ANTI_ALIAS_FLAG).apply {
            color = boardGame.getBoardColor(1)
        }
    }

    private fun getCellPaint(): Paint {
        return Paint().apply { color = boardGame.getBoardColor(0) }
    }

    private fun getHighlightPlayStrokePaint(): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG and Paint.DITHER_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6.0f
        }
    }

    private fun getHighlightPlayPaint(): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = boardGame.getBoardColor(2)
        }
    }

    private fun drawPiece(canvas: Canvas?, x: Int, y: Int, pieceType: Int) {

        val centerX = (pieceWidth * x) + pieceWidth / 2
        val centerY = (pieceHeight * y) + pieceHeight / 2

        val radius = Math.min(pieceWidth, pieceHeight) / 2 - MARGIN_PIECE * 2
        val paint = Paint().apply { color = Color.WHITE }

        if (boardGame.getGameMode() != 2) {
            when (pieceType) {
                1 -> paint.color = boardGame.getColor(0)
                2 -> paint.color = boardGame.getColor(1)
            }
        } else {
            when (pieceType) {
                1 -> paint.color = boardGame.getColor(0)
                2 -> paint.color = boardGame.getColor(1)
                3 -> paint.color = boardGame.getColor(2)
            }
        }

        canvas?.drawCircle(
            centerX.toFloat(), centerY.toFloat(),
            radius.toFloat(), paint
        )
    }


}