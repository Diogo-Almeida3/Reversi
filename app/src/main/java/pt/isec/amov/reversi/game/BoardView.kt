package pt.isec.amov.reversi.game

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
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
import kotlinx.android.parcel.Parcelize
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.game.jsonClasses.*
import pt.isec.amov.reversi.game.jsonClasses.alerts.*
import pt.isec.amov.reversi.game.jsonClasses.moves.ClientMoveData
import pt.isec.amov.reversi.game.jsonClasses.moves.ServerMoveData
import pt.isec.amov.reversi.game.jsonClasses.profile.GamePerfilData
import pt.isec.amov.reversi.game.jsonClasses.profile.ProfileData
import pt.isec.amov.reversi.game.jsonClasses.specialPieces.RequestBomb
import pt.isec.amov.reversi.game.jsonClasses.specialPieces.RequestExchange
import java.io.*
import java.lang.NullPointerException
import java.net.*
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
    private var validPlay = false
    private var counter = 0

    private var exchangeArrayList = ArrayList<PieceMoves>()
    private var exchangeCounter = 0
    private var loading = false

    private lateinit var boardGame: BoardGame
    private  lateinit var gamePerfilView: GamePerfilView
    private lateinit var auth: FirebaseAuth



    enum class State {
        STARTING, SETTING_PROFILE_DATA, PLAYING_SERVER, PLAYING_CLIENT, GAME_OVER
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, CLIENT_CONNECTING, CONNECTION_ESTABLISHED,
        CONNECTION_ERROR, CONNECTION_ENDED,CONNECTION_TIMEOUT
    }

    val state = MutableLiveData(State.STARTING)

    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)


    private var socket: Socket? = null
    private val socketI: InputStream?
        get() = socket?.getInputStream()
    private val socketO: OutputStream?
        get() = socket?.getOutputStream()

    private var serverSocket: ServerSocket? = null
    private var threadComm: Thread? = null
    private var isServer = false
    private var exchangeError = 0

    fun setData(boardGame: BoardGame, gameProfileView: GamePerfilView) {
        this.boardGame = boardGame
        this.gamePerfilView = gameProfileView
        boardSIZE = boardGame.getBoardSize()
        exchangeCounter = 0
        exchangeError = 0

        /* Multiplayer */
        isServer = false
        validPlay = false
        auth = Firebase.auth
        //state.postValue(State.SETTING_PROFILE_DATA)
    }

    fun movePiece(x: Int, y:Int,piece: Int){
        when (piece) {
            NORMAL_PIECE -> {
                if (boardGame.confirmMove(x, y)) {
                    validPlay = true
                    boardGame.move(x, y)
                    boardGame.checkBoardPieces()

                    if (boardGame.checkEndGame())
                        alertEndGame()
                    else
                        checkAlertNoPlays()
                } else
                    validPlay = false
            }

            BOMB_PIECE -> {
                if (boardGame.confirmBombMove(x, y)) {
                    validPlay = true
                    boardGame.pieceBomb(x, y)
                    boardGame.checkBoardPieces()
                    checkAlertNoPlays()
                } else{
                    validPlay = false
                    if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                        showAlertGeneral(context.resources.getString(R.string.bombPieceSelect))
                }
            }

            EXCHANGE_PIECE -> {
                when (boardGame.confirmExchangeMove(x, y, exchangeCounter, exchangeArrayList)) {
                    -3 ->{
                        exchangeError = -3
                        validPlay = false
                        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                            showAlertGeneral(context.resources.getString(R.string.exchangeSelectTwice))
                    }
                    -2 -> {
                        exchangeError = -2
                        validPlay = false
                        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                            showAlertGeneral(context.resources.getString(R.string.exchangeWrongPiece))
                    }
                    -1 -> {
                        exchangeError = -1
                        validPlay = false
                        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                            showAlertGeneral(context.resources.getString(R.string.exchangeBoardError))
                    }
                    1 -> {
                        exchangeError = 0
                        validPlay = false
                        exchangeArrayList.add(PieceMoves(x, y))
                        exchangeCounter++

                        if (exchangeCounter >= 3) {
                            validPlay = true
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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val x = (event?.x?.div(pieceWidth))?.toInt()
        val y = (event?.y?.div(pieceHeight))?.toInt()
        counter = 0
        when (boardGame.getGameMode()) {
            0 -> {
                if (!endGame) {
                    movePiece(x!!,y!!,boardGame.getCurrentPiece())
                }
            }
            1 -> {

                if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value != State.PLAYING_SERVER && state.value != State.PLAYING_CLIENT){
                    return super.onTouchEvent(event)
                }

                if(state.value == State.GAME_OVER)
                    return super.onTouchEvent(event)

                if (!isServer && state.value == State.PLAYING_CLIENT) {

                    val move = PieceMoves(x!!, y!!)

                        socketO?.run {
                            thread {

                                    val moveData = ClientMoveData(move,boardGame.getCurrentPiece())

                                    val gson = Gson()
                                    val jsonSend = gson.toJson(moveData)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()

                            }

                        }
                }
                else if(isServer && state.value == State.PLAYING_SERVER){

                    val auxPiece = boardGame.getCurrentPiece()

                    movePiece(x!!,y!!,auxPiece)

                    if(validPlay){
                        state.postValue(State.PLAYING_CLIENT)
                        val move = PieceMoves(x,y)

                            socketO?.run {

                                thread{
                                        val moveData : ServerMoveData
                                        when(auxPiece){
                                            EXCHANGE_PIECE -> {
                                                moveData = ServerMoveData(move,auxPiece,exchangeArrayList)
                                                exchangeArrayList.clear()
                                            }
                                            else -> moveData = ServerMoveData(move,auxPiece)
                                        }

                                        val gson = Gson()
                                        val jsonSend = gson.toJson(moveData)

                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()

                                }
                            }


                    }
                }
            }
        }


        return super.onTouchEvent(event)
    }


    private fun startComs(newSocket: Socket?) {
        //Aqui vamos receber um socket que será usado para toda a comunicação e atribuiremo lo à propriedade socket

        if(threadComm != null)
            return

        socket = newSocket
        socket?.soTimeout = 60 * 1000


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
                        if(type.toString().equals("\"PROFILE\"")){
                            gamePerfilView.setUsersProfileData(jsonObject.get("name").toString(),jsonObject.get("photo").toString())

                            val currentPlayer = boardGame.getCurrentPlayer()
                            socketO?.run {

                                val gamePerfilData = GamePerfilData(gamePerfilView.getnClients(),gamePerfilView.getUsernames(),gamePerfilView.getBitmaps(),currentPlayer)

                                val gson = Gson()
                                val jsonSend = gson.toJson(gamePerfilData)

                                val printStream = PrintStream(this)
                                printStream.println(jsonSend)
                                printStream.flush()
                            }
                            when(currentPlayer - 1){
                                0 -> state.postValue(State.PLAYING_SERVER)
                                1 -> state.postValue(State.PLAYING_CLIENT)
                            }
                        }
                        else if(type.toString().equals("\"CLIENT_MOVE\"")){
                            val posX = (jsonObject.get("move") as JsonObject).get("posX").asInt
                            val posY = (jsonObject.get("move") as JsonObject).get("posY").asInt
                            val currentPiece = jsonObject.get("currentPiece").asInt

                            movePiece(posX,posY,currentPiece)

                            if(validPlay){
                                val move = PieceMoves(posX,posY)
                                socketO?.run {
                                    val moveData :ServerMoveData
                                    when(currentPiece){
                                        EXCHANGE_PIECE ->{
                                            moveData = ServerMoveData(move,currentPiece,exchangeArrayList)
                                            exchangeArrayList.clear()
                                        }
                                        else -> moveData = ServerMoveData(move,currentPiece)
                                    }


                                    val gson = Gson()
                                    val jsonSend = gson.toJson(moveData)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }

                                state.postValue(State.PLAYING_SERVER)
                                validPlay = false
                            }
                            else {
                                when(currentPiece){
                                    BOMB_PIECE -> {
                                        socketO?.run {
                                            val alertInvalidBomb = AlertInvalidBomb()

                                            val gson = Gson()
                                            val jsonSend = gson.toJson(alertInvalidBomb)

                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                    EXCHANGE_PIECE -> {
                                        socketO?.run {
                                            val alertInvalidExchange = AlertInvalidExchange(exchangeError)

                                            val gson = Gson()
                                            val jsonSend = gson.toJson(alertInvalidExchange)

                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                }
                            }
                        }
                        else if(type.toString().equals("\"OK\"")){
                            if(connectionState.value == ConnectionState.CONNECTION_ESTABLISHED && endGame) {

                                socketO?.run {
                                    val alertData = AlertEndgameData()
                                    val gson = Gson()
                                    val jsonSend = gson.toJson(alertData)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                    state.postValue(State.GAME_OVER)
                                }
                            }
                        }
                        else if(type.toString().equals("\"ALERT_PASS\"")){
                            ++counter
                            if (counter < boardGame.getPlayers()){

                                if(checkAlertNoPlays()){
                                    socketO?.run {
                                        val okData = OkData(false)

                                        val gson = Gson()
                                        val jsonSend = gson.toJson(okData)

                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()

                                    }
                                }
                                switchStatePlay() // Manda ok
                            }
                            else{
                                alertEndGame()
                                socketO?.run {
                                    val passPlayData = PassPlayData()

                                    val gson = Gson()
                                    val jsonSend = gson.toJson(passPlayData)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()

                                }
                            }
                        }
                        else if(type.toString().equals("\"REQUEST_BOMB\"")){
                            if(boardGame.getBombPiece() > 0){
                                socketO?.run {
                                    thread {
                                        val requestBomb = RequestBomb(true)

                                        val gson = Gson()
                                        val jsonSend:String = gson.toJson(requestBomb)


                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }
                            } else{
                                socketO?.run {
                                    thread {
                                        val requestBomb = RequestBomb(false)

                                        val gson = Gson()
                                        val jsonSend:String = gson.toJson(requestBomb)


                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }
                            }
                        }
                        else if(type.toString().equals("\"REQUEST_EXCHANGE\"")){
                            when {
                                boardGame.getTotalPieces(boardGame.getCurrentPlayer() - 1) <= 1 -> {
                                    socketO?.run {
                                        thread {
                                            val requestExchange = RequestExchange(0)

                                            val gson = Gson()
                                            val jsonSend:String = gson.toJson(requestExchange)


                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                }
                                boardGame.getExchangePiece() > 0 -> {
                                    socketO?.run {
                                        thread {
                                            val requestExchange = RequestExchange(1)

                                            val gson = Gson()
                                            val jsonSend:String = gson.toJson(requestExchange)


                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                }
                                else -> {
                                    socketO?.run {
                                        thread {
                                            val requestExchange = RequestExchange(2)

                                            val gson = Gson()
                                            val jsonSend:String = gson.toJson(requestExchange)


                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                }
                            }
                        }
                        else if(type.toString().equals("\"CLOSE_CONNECTION\"")){
                            connectionState.postValue(ConnectionState.CONNECTION_ENDED)
                            state.postValue(State.GAME_OVER)
                        }
                    } else{
                        when {
                            type.toString().equals("\"PROFILE_VIEW\"") -> {
                                val currentPlayer = jsonObject.get("currentPlayer").asInt
                                boardGame.setCurrentPlayer(currentPlayer)


                                val nClients = jsonObject.get("nClients").asInt

                                val users = jsonObject.get("usernames").asJsonArray
                                val usernames = ArrayList<String>()
                                for(i in 0 until nClients){
                                    val name = users[i].toString().replace("\"","")
                                    usernames.add(name)
                                    boardGame.setUsername(i,name)
                                }


                                val photos = jsonObject.get("photos").asJsonArray
                                val userPhotos = ArrayList<String>()
                                for(i in 0 until nClients)
                                    userPhotos.add(photos[i].toString().replace("\\n","").replace("\"",""))


                                gamePerfilView.updateUsers(nClients,usernames,userPhotos)
                                when(currentPlayer - 1){
                                    0 -> state.postValue(State.PLAYING_SERVER)
                                    1 -> state.postValue(State.PLAYING_CLIENT)
                                }

                            }
                            type.toString().equals("\"SERVER_MOVE\"") -> {
                                //Ele só recebe esta estrutura após o servidor já ter validado

                                val posX = (jsonObject.get("move") as JsonObject).get("posX").asInt
                                val posY = (jsonObject.get("move") as JsonObject).get("posY").asInt
                                val currentPiece = jsonObject.get("currentPiece").asInt

                                when(currentPiece){
                                    NORMAL_PIECE -> {
                                        boardGame.move(posX,posY)
                                        boardGame.checkBoardPieces()
                                        boardGame.switchPlayer()
                                        switchStatePlay()
                                    }
                                    BOMB_PIECE -> {
                                        boardGame.pieceBomb(posX, posY)
                                        boardGame.checkBoardPieces()
                                        boardGame.switchPlayer()
                                        switchStatePlay()
                                    }
                                    EXCHANGE_PIECE -> {
                                        val movesX = jsonObject.get("exchangeArrayListX").asJsonArray
                                        val movesY = jsonObject.get("exchangeArrayListY").asJsonArray
                                        for(i in 0 until 3){
                                            exchangeArrayList.add(PieceMoves(movesX[i].asInt,movesY[i].asInt))
                                        }
                                        boardGame.exchangePiece(exchangeArrayList)
                                        boardGame.checkBoardPieces()
                                        exchangeArrayList.clear()
                                        boardGame.switchPlayer()
                                        switchStatePlay()
                                    }
                                }

                            }
                            type.toString().equals("\"ALERT_ENDGAME\"") -> {
                                alertEndGame()
                                state.postValue(State.GAME_OVER)
                            }
                            type.toString().equals("\"ALERT_NO_VALID_PLAYS\"") -> {
                                Handler(Looper.getMainLooper()).post{
                                    showAlertPassPlay("")
                                }
                            }
                            type.toString().equals("\"ALERT_PASS\"") -> {
                                alertEndGame()
                                state.postValue(State.GAME_OVER)
                            }
                            type.toString().equals("\"OK\"") -> {
                                if(jsonObject.get("aux").asBoolean){
                                    boardGame.switchPlayer()
                                    boardGame.checkNoValidPlays()
                                    boardGame.checkBoardPieces()
                                    switchStatePlay()
                                }
                            }
                            type.toString().equals("\"REQUEST_BOMB\"") ->{
                                when(jsonObject.get("switchToBomb").asBoolean){
                                    true -> boardGame.setPieceType(BOMB_PIECE)
                                    false -> {
                                        Handler(Looper.getMainLooper()).post{
                                            showAlertGeneral(boardGame.getName() + " has no available bomb pieces!")
                                        }
                                    }
                                }

                            }
                            type.toString().equals("\"REQUEST_EXCHANGE\"") -> {
                                when(jsonObject.get("switchToExchange").asInt){
                                    0 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            showAlertGeneral(boardGame.getName() + context.resources.getString(R.string.exchangeNoBoardPieces))
                                        }
                                    }
                                    1 -> boardGame.setPieceType(EXCHANGE_PIECE)
                                    2 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            showAlertGeneral(boardGame.getName() + context.resources.getString(R.string.exchangeNoAvailablePieces))
                                        }
                                    }
                                }
                            }
                            type.toString().equals("\"ALERT_INVALID_BOMB\"") -> {
                                Handler(Looper.getMainLooper()).post{
                                    showAlertGeneral(context.resources.getString(R.string.bombPieceSelect))
                                }
                            }
                            type.toString().equals("\"ALERT_INVALID_EXCHANGE\"") -> {
                                when(jsonObject.get("error").asInt) {
                                    -1 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            showAlertGeneral(context.resources.getString(R.string.exchangeBoardError))
                                        }
                                    }
                                    -2 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            showAlertGeneral(context.resources.getString(R.string.exchangeWrongPiece))
                                        }
                                    }
                                    -3 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            showAlertGeneral(context.resources.getString(R.string.exchangeSelectTwice))
                                        }
                                    }
                                }
                            }
                            type.toString().equals("\"CLOSE_CONNECTION\"") -> {
                                connectionState.postValue(ConnectionState.CONNECTION_ENDED)
                                state.postValue(State.GAME_OVER)
                            }
                        }
                    }
                }
            }catch (socketE : SocketTimeoutException){
                Log.d("BUG", socketE.toString())
                //Quando dá timeout
                cleanTimeout()
            }
            catch (nullEx : NullPointerException){
                //Quando dá uma exceção e ja nao existe o outro socket
                Log.d("BUG", nullEx.toString())
                cleanTimeout()
            }
            catch(softwareE: SocketException){
                //Quando desligo a net e perco a conexao vai para o modo 1
                Log.d("BUG", softwareE.toString())
                cleanTimeout()
            }
            catch (exc: Exception) {
                Log.d("BUG", exc.toString())
            } finally {
                if(connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
                    stopGame()
                else{
                    socket?.close()
                    isServer = false
                    validPlay = false
                }

            }
        }
    }

    fun switchStatePlay(){
        socketO?.run {
            thread {
                val OkData = OkData(true)
                val gson = Gson()
                val jsonSend = gson.toJson(OkData)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()
            }
        }
        when(state.value){
            State.PLAYING_CLIENT -> state.postValue(State.PLAYING_SERVER)
            State.PLAYING_SERVER -> state.postValue(State.PLAYING_CLIENT)

            else -> {}
        }


    }

    fun stopGame() {
        try {
            state.postValue(State.GAME_OVER)

            socket?.close()
            socket = null
            threadComm?.interrupt()
            threadComm = null
        } catch (_: Exception) { }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ERROR)
        serverSocket = null
    }

    fun startClient(name: String , serverIP: String, serverPort: Int = SERVER_PORT) {

        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return

        connectionState.postValue(ConnectionState.CLIENT_CONNECTING)

        thread {
            try {
                val clientSocket = Socket()
                clientSocket.connect(InetSocketAddress(serverIP, serverPort), 5000)
                startComs(clientSocket)

                Log.v("COMMS", "A thread cooms foi iniciada para o cliente")
                socketO?.run {
                    thread {
                        val profileData: ProfileData
                        var uri = File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")
                        if(uri.exists()) {
                            profileData= ProfileData(name, convertToBase64(uri))
                        }
                        else{
                            profileData= ProfileData(name, "null")
                        }
                        val gson = Gson()
                        val jsonSend:String = gson.toJson(profileData)


                        val printStream = PrintStream(this)
                        printStream.println(jsonSend)
                        printStream.flush()
                    }
                }
            } catch (_: Exception) {
                connectionState.postValue(ConnectionState.CONNECTION_ERROR)
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
        if(uri.exists())
            gamePerfilView.setUsersProfileData(name,convertToBase64(uri))
        else
            gamePerfilView.setUsersProfileData(name,"null")

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


    fun switchBombPiece() {
        socketO?.run {
            thread{
                val requestBomb = RequestBomb()

                val gson = Gson()
                val jsonSend = gson.toJson(requestBomb)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()
            }
        }
    }

    fun switchExchangePiece() {
        socketO?.run {
            thread{
                val requestExchange = RequestExchange()

                val gson = Gson()
                val jsonSend = gson.toJson(requestExchange)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()
            }
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if(state.value != State.GAME_OVER)
            cleanUp()

        //Quando eu sair e der pop do fragmento anterior o garbage collector elimina as ligações
    }

    fun cleanUp(){
        socketO?.run {
            thread {
                val closeConnection = CloseConnection()
                val gson = Gson()
                val jsonSend = gson.toJson(closeConnection)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()

            }
        }
    }

    fun cleanTimeout(){

        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        state.postValue(State.GAME_OVER)
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

    private fun alertEndGame() {
        val aux = boardGame.checkWinner()
        if(connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
            Handler(Looper.getMainLooper()).post{
                showAlertEndGame(aux)
            }
        else
            showAlertEndGame(aux)
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

        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED)
            showAlertPassPlay(boardGame.getName())
        else{
            if(state.value == State.PLAYING_CLIENT){
                Handler(Looper.getMainLooper()).post{
                    showAlertPassPlay("")
                }
            }
            else{
                socketO?.run {
                    thread{
                        val alertPlays = AlertNoValidPlayData()

                        val gson = Gson()
                        val jsonSend = gson.toJson(alertPlays)

                        val printStream = PrintStream(this)
                        printStream.println(jsonSend)
                        printStream.flush()
                    }
                }
            }
        }




        //Servidor pode apresentar aqui
        //Se for cliente a jogar abre o socket e diz lhe

        return false

    }

    private fun showAlertEndGame(player: Player?) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)

        if (player != null)
            builder1.setMessage(player.getUsername() + context.resources.getString(R.string.winner))
        else
            builder1.setMessage(context.resources.getString(R.string.draw))
        builder1.setCancelable(false)

        builder1.setPositiveButton(context.resources.getString(R.string.checkBoard)) { dialog, id ->
            run {
                dialog.cancel()

                if(connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
                    connectionState.postValue(ConnectionState.CONNECTION_ENDED)
            }
        }

        builder1.setNegativeButton(context.resources.getString(R.string.backToMenu)) { dialog, id ->
            run {
                dialog.cancel()
                if(connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
                    connectionState.postValue(ConnectionState.CONNECTION_ERROR)
                else
                    findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun showAlertGeneral(phrase: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(phrase)
        builder1.setCancelable(false)

        builder1.setPositiveButton(context.resources.getString(R.string.pass)) { dialog, id ->
            run {
                dialog.cancel()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    private fun showAlertPassPlay(name: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        if (name.equals(""))
            builder1.setMessage(context.resources.getString(R.string.noAvailablePlays))
        else
            builder1.setMessage(name + context.resources.getString(R.string.noAvailablePlaysName))
        builder1.setCancelable(false)

        builder1.setPositiveButton(context.resources.getString(R.string.pass)) { dialog, id ->
            run {
                dialog.cancel()
                if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED){
                    ++counter
                    if (counter < boardGame.getPlayers()) //Quando for igual já sabemos que percorreu os jogadores todos portanto não vale apena fazer o igual
                        checkAlertNoPlays()
                    else
                        alertEndGame()
                } else{

                    if(isServer){
                        ++counter
                        if (counter < boardGame.getPlayers()){
                            switchStatePlay()
                            checkAlertNoPlays()
                        }
                        else{
                            alertEndGame()
                            socketO?.run {
                                thread{
                                    val passPlayData = PassPlayData()

                                    val gson = Gson()
                                    val jsonSend = gson.toJson(passPlayData)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }
                            }
                        }
                    }
                    else {
                        //Informa que passou a jogada
                        socketO?.run {
                            thread {
                                val passPlayData = PassPlayData()

                                val gson = Gson()
                                val jsonSend = gson.toJson(passPlayData)

                                val printStream = PrintStream(this)
                                printStream.println(jsonSend)
                                printStream.flush()
                            }
                        }
                    }
                    //Há jogadas válidas

                    //Foi o servidor a apresentar o primeiro erro
                    //Foi o cliente a apresentar o primeiro
                }

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


    fun getConnectionState(): ConnectionState? = connectionState.value

    fun getGameState() : State? = state.value

    fun getIsServer():Boolean = isServer



}