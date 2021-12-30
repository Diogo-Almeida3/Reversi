package pt.isec.amov.reversi.game

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.fragment_game.*
import pt.isec.amov.reversi.fragments.GameFragment
import pt.isec.amov.reversi.game.jsonClasses.CloseConnection
import pt.isec.amov.reversi.game.jsonClasses.OkData
import pt.isec.amov.reversi.game.jsonClasses.alerts.*
import pt.isec.amov.reversi.game.jsonClasses.moves.ClientMoveData
import pt.isec.amov.reversi.game.jsonClasses.moves.ServerMoveData
import pt.isec.amov.reversi.game.jsonClasses.profile.GamePerfilData
import pt.isec.amov.reversi.game.jsonClasses.profile.ProfileData
import pt.isec.amov.reversi.game.jsonClasses.specialPieces.RequestBomb
import pt.isec.amov.reversi.game.jsonClasses.specialPieces.RequestExchange
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.lang.NullPointerException
import java.net.*
import kotlin.concurrent.thread


const val SERVER_PORT_GAMEMODE2 = 9999
const val SERVER_PORT_GAMEMODE3 = 9998
class GameViewModel : ViewModel() {

    enum class State {
        STARTING, PLAYING_SERVER, PLAYING_CLIENT, PLAYING_SECOND_CLIENT, GAME_OVER, LEFT_GAME
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, CLIENT_CONNECTING, CONNECTION_ESTABLISHED,
        CONNECTION_ERROR, CONNECTION_ENDED, CONNECTION_TIMEOUT
    }

    val state = MutableLiveData(State.STARTING)

    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    private var exchangeArrayList = ArrayList<PieceMoves>()
    private var exchangeCounter = 0

    companion object {
        const val NORMAL_PIECE = 0
        const val BOMB_PIECE = 1
        const val EXCHANGE_PIECE = 2
    }

    private var exchangeError = 0
    private var validPlay = false
    private var counter = 0

    private var socket: Socket? = null
    private val socketI: InputStream?
        get() = socket?.getInputStream()
    private val socketO: OutputStream?
        get() = socket?.getOutputStream()

    private var serverSocket: ServerSocket? = null
    private var threadComm: Thread? = null

    private var socketArrayServer = ArrayList<Socket>()
    private var threadCommServer3 = ArrayList<Thread>()
    private var isServer = false
    private var endGame = false


    private lateinit var auth: FirebaseAuth
    private lateinit var boardGame: BoardGame
    private lateinit var gameFragment: GameFragment


    fun startClient(name: String, serverIP: String) {

        if (socket != null || connectionState.value != ConnectionState.SETTING_PARAMETERS)
            return

        connectionState.postValue(ConnectionState.CLIENT_CONNECTING)

        thread {
            try {
                val clientSocket = Socket()
                when (boardGame.getGameMode()) {
                    1 -> {
                        clientSocket.connect(InetSocketAddress(serverIP, SERVER_PORT_GAMEMODE2), 1000 * 5)
                    }
                    2 -> {
                        clientSocket.connect(InetSocketAddress(serverIP, SERVER_PORT_GAMEMODE3), 1000 * 20)
                    }
                }
                startComs(clientSocket)
                Log.v("COMMS", "A thread cooms foi iniciada para o cliente")
                socketO?.run {

                    val profileData: ProfileData
                    val uri =
                        File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")

                    profileData = if (uri.exists()) {
                        ProfileData(name, convertToBase64(uri))
                    } else {
                        ProfileData(name, "null")
                    }
                    val gson = Gson()
                    val jsonSend: String = gson.toJson(profileData)


                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()

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
        val uri =
            File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")
        if (uri.exists())
            gameFragment.setUsersProfileData(name, convertToBase64(uri))
        else
            gameFragment.setUsersProfileData(name, "null")

        thread {
            when (boardGame.getGameMode()) {
                1 -> {
                    serverSocket = ServerSocket(SERVER_PORT_GAMEMODE2)
                }
                2 ->{
                    serverSocket = ServerSocket(SERVER_PORT_GAMEMODE3)
                }
            }
            serverSocket?.run {
                try {
                    when (boardGame.getGameMode()) {
                        1 -> {
                            val newServerSocket = serverSocket!!.accept()
                            startComs(newServerSocket)
                        }
                        2 -> {
                            var nConnections = 0
                            while (nConnections != 2) {
                                startComsServer3(serverSocket!!.accept())
                                nConnections++
                            }
                        }
                    }
                    Log.v("COMMS", "A thread cooms foi iniciada para o $isServer")

                } catch (_: Exception) {
                    connectionState.postValue(ConnectionState.CONNECTION_ERROR)
                } finally {
                    connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)
                    serverSocket?.close()
                    serverSocket = null
                }
            }
        }
    }

    private fun startComsServer3(socket: Socket) {

        socketArrayServer.add(socket)

        val threadAux = thread {
            val socketInput = socketArrayServer[socketArrayServer.size - 1].getInputStream()

            //todo se um deles for null podemos acabar logo com o Jogo
            if (socketInput == null)
                return@thread

            val bufInput = socketInput.bufferedReader()
            try {

                //Aqui iniciamos a leitura para os dois clientes
                while (state.value != State.GAME_OVER) {
                    val message = bufInput.readLine()
                    val jsonObject = JsonParser().parse(message).asJsonObject
                    val type = jsonObject.get("type")

                    when {
                        type.toString().equals("\"PROFILE\"") -> {

                            gameFragment.setUsersProfileData(
                                jsonObject.get("name").toString(),
                                jsonObject.get("photo").toString()
                            )


                            //Quando ambos os utilizadores enviarem o nome e a foto ele vai mandar a resposta aos dois
                            if (boardGame.getNClients() >= 3) {

                                val currentPlayer = boardGame.getCurrentPlayer()
                                val gamePerfilData = GamePerfilData(
                                    gameFragment.getnClients(),
                                    gameFragment.getUsernames(),
                                    gameFragment.getBitmaps(),
                                    currentPlayer
                                )
                                when (currentPlayer - 1) {
                                    0 -> state.postValue(State.PLAYING_SERVER)
                                    1 -> state.postValue(State.PLAYING_CLIENT)
                                    2 -> state.postValue(State.PLAYING_SECOND_CLIENT)
                                }
                                for (j in 0 until socketArrayServer.size) {
                                    socketArrayServer[j].getOutputStream().run {

                                        val gson = Gson()
                                        val jsonSend = gson.toJson(gamePerfilData)

                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }

                            }

                        }
                        type.toString().equals("\"CLIENT_MOVE\"") -> {
                            val posX = (jsonObject.get("move") as JsonObject).get("posX").asInt
                            val posY = (jsonObject.get("move") as JsonObject).get("posY").asInt
                            val currentPiece = jsonObject.get("currentPiece").asInt

                            var checkPlayerMessage = true
                            var number = -1
                            when (boardGame.getCurrentPlayer() - 1) {
                                1 -> {
                                    if (socketArrayServer[0].getInputStream() == socketInput){
                                        movePiece(posX, posY, currentPiece)
                                        number = 0
                                    }
                                    else {
                                        validPlay = false
                                        checkPlayerMessage = false
                                    }
                                }
                                2 -> {
                                    if (socketArrayServer[1].getInputStream() == socketInput){
                                        movePiece(posX, posY, currentPiece)
                                        number = 1
                                    }
                                    else {
                                        validPlay = false
                                        checkPlayerMessage = false
                                    }
                                }
                            }

                            if (validPlay) {
                                val move = PieceMoves(posX, posY)
                                for (j in 0 until socketArrayServer.size) {
                                    socketArrayServer[j].getOutputStream().run {
                                        val moveData: ServerMoveData
                                        when (currentPiece) {
                                            EXCHANGE_PIECE -> {
                                                moveData = ServerMoveData(move, currentPiece, exchangeArrayList)
                                                    if(j >= socketArrayServer.size - 1)
                                                        exchangeArrayList.clear()
                                            }
                                            else -> moveData = ServerMoveData(move, currentPiece)
                                        }


                                        val gson = Gson()
                                        val jsonSend = gson.toJson(moveData)

                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }
                                when (boardGame.getCurrentPlayer() - 1) {
                                    0 -> state.postValue(State.PLAYING_SERVER)
                                    1 -> state.postValue(State.PLAYING_CLIENT)
                                    2 -> state.postValue(State.PLAYING_SECOND_CLIENT)
                                }
                                validPlay = false
                            }
                            else if (checkPlayerMessage) {
                                exchangeBomb3Players(number, currentPiece)

                            }
                        }
                        type.toString().equals("\"OK\"") -> {
                            if (connectionState.value == ConnectionState.CONNECTION_ESTABLISHED && endGame) {


                                if (socketArrayServer[0].getInputStream() == socketInput){
                                    socketArrayServer[0].getOutputStream().run {
                                        val alertData = AlertEndgameData()
                                        val gson = Gson()
                                        val jsonSend = gson.toJson(alertData)

                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                } else if (socketArrayServer[1].getInputStream() == socketInput){
                                    socketArrayServer[1].getOutputStream().run {
                                        val alertData = AlertEndgameData()
                                        val gson = Gson()
                                        val jsonSend = gson.toJson(alertData)

                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }
                            }
                        }
                        type.toString().equals("\"ALERT_PASS\"") -> {
                            ++counter

                            if (counter < boardGame.getPlayers()) {

                                when (state.value) {
                                    State.PLAYING_SECOND_CLIENT -> state.postValue(State.PLAYING_SERVER)
                                    State.PLAYING_SERVER -> state.postValue(State.PLAYING_CLIENT)
                                    State.PLAYING_CLIENT -> state.postValue(State.PLAYING_SECOND_CLIENT)
                                    else -> {}
                                }
                                //aqui no if ve se nao tiver jogadas informa o jogador
                                if (checkAlertNoPlays()) {
                                    //Senao Manda aos utilizadores para se atualizarem
                                    for (j in 0 until socketArrayServer.size) {
                                        socketArrayServer[j].getOutputStream().run {
                                            val okData = OkData(false)
                                            val gson = Gson()
                                            val jsonSend = gson.toJson(okData)

                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                }
                            } else {
                                //acaba o jogo manda para todos os clientes
                                alertEndGame()
                                state.postValue(State.GAME_OVER)
                                for (j in 0 until socketArrayServer.size) {
                                    socketArrayServer[j].getOutputStream().run {
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
                        type.toString().equals("\"REQUEST_BOMB\"") -> {
                            when (boardGame.getCurrentPlayer() - 1) {
                                1 -> {
                                    if (socketArrayServer[0].getInputStream() == socketInput)
                                        bombCheck3Players(0)
                                    else
                                        validPlay = false
                                }
                                2 -> {
                                    if (socketArrayServer[1].getInputStream() == socketInput)
                                        bombCheck3Players(1)
                                    else
                                        validPlay = false
                                }
                            }

                        }
                        type.toString().equals("\"REQUEST_EXCHANGE\"") -> {
                            when (boardGame.getCurrentPlayer() - 1) {
                                1 -> {
                                    if (socketArrayServer[0].getInputStream() == socketInput)
                                        exchangeCheck3Players(0)
                                    else
                                        validPlay = false
                                }
                                2 -> {
                                    if (socketArrayServer[1].getInputStream() == socketInput)
                                        exchangeCheck3Players(1)
                                    else
                                        validPlay = false
                                }
                            }

                        }
                        type.toString().equals("\"CLOSE_CONNECTION\"") -> {
                            if (socketArrayServer[0].getInputStream() == socketInput){
                                socketArrayServer[1].getOutputStream().run {
                                    val closeConnection = CloseConnection()
                                    val gson = Gson()
                                    val jsonSend = gson.toJson(closeConnection)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }
                            } else if (socketArrayServer[1].getInputStream() == socketInput){
                                socketArrayServer[0].getOutputStream().run {
                                    val closeConnection = CloseConnection()
                                    val gson = Gson()
                                    val jsonSend = gson.toJson(closeConnection)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }
                            }
                            cleanOnExit3Players()
                        }
                    }

                }
            } catch(exc : NullPointerException){
                cleanOnExit()
            }
            catch(exc : SocketException){
                cleanOnExit()
            }
            catch (exc: Exception) {
                Log.d("BUG", exc.toString())
            }
            finally {
                if (connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
                    stopGame()
            }
        }

        threadCommServer3.add(threadAux)
    }

    private fun startComs(newSocket: Socket?) {
        //Aqui vamos receber um socket que será usado para toda a comunicação e atribuiremo lo à propriedade socket

        if (threadComm != null)
            return

        socket = newSocket

        //Esta thread vai ficar a correr permantemente ate o jogo acabar e for necessario reiniciar
        threadComm = thread {
            try {
                if (socketI == null)
                    return@thread

                //Estabelece-se a conexão
                connectionState.postValue(ConnectionState.CONNECTION_ESTABLISHED)

                val bufInput = socketI!!.bufferedReader()

                while (state.value != State.GAME_OVER) {
                    val message = bufInput.readLine()
                    val jsonObject = JsonParser().parse(message).asJsonObject
                    val type = jsonObject.get("type")


                    if (isServer) {
                        if (type.toString().equals("\"PROFILE\"")) {
                            gameFragment.setUsersProfileData(
                                jsonObject.get("name").toString(),
                                jsonObject.get("photo").toString()
                            )

                            val currentPlayer = boardGame.getCurrentPlayer()
                            socketO?.run {

                                val gamePerfilData = GamePerfilData(
                                    gameFragment.getnClients(),
                                    gameFragment.getUsernames(),
                                    gameFragment.getBitmaps(),
                                    currentPlayer
                                )

                                val gson = Gson()
                                val jsonSend = gson.toJson(gamePerfilData)

                                val printStream = PrintStream(this)
                                printStream.println(jsonSend)
                                printStream.flush()
                            }
                            when (currentPlayer - 1) {
                                0 -> state.postValue(State.PLAYING_SERVER)
                                1 -> state.postValue(State.PLAYING_CLIENT)
                            }
                        }
                        else if (type.toString().equals("\"CLIENT_MOVE\"")) {
                            val posX = (jsonObject.get("move") as JsonObject).get("posX").asInt
                            val posY = (jsonObject.get("move") as JsonObject).get("posY").asInt
                            val currentPiece = jsonObject.get("currentPiece").asInt

                            movePiece(posX, posY, currentPiece)

                            if (validPlay) {
                                val move = PieceMoves(posX, posY)
                                socketO?.run {
                                    val moveData: ServerMoveData
                                    when (currentPiece) {
                                        EXCHANGE_PIECE -> {
                                            moveData = ServerMoveData(
                                                move,
                                                currentPiece,
                                                exchangeArrayList
                                            )
                                            exchangeArrayList.clear()
                                        }
                                        else -> moveData = ServerMoveData(move, currentPiece)
                                    }


                                    val gson = Gson()
                                    val jsonSend = gson.toJson(moveData)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }

                                state.postValue(State.PLAYING_SERVER)
                                validPlay = false
                            } else {
                                when (currentPiece) {
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
                                            val alertInvalidExchange =
                                                AlertInvalidExchange(exchangeError)

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
                        else if (type.toString().equals("\"OK\"")) {
                            if (connectionState.value == ConnectionState.CONNECTION_ESTABLISHED && endGame) {

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
                        else if (type.toString().equals("\"ALERT_PASS\"")) {
                            ++counter
                            if (counter < boardGame.getPlayers()) {

                                if (checkAlertNoPlays()) {

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
                            } else {
                                alertEndGame()
                                state.postValue(State.GAME_OVER)
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
                        else if (type.toString().equals("\"REQUEST_BOMB\"")) {
                            if (boardGame.getBombPiece() > 0) {
                                socketO?.run {
                                    thread {
                                        val requestBomb = RequestBomb(true)

                                        val gson = Gson()
                                        val jsonSend: String = gson.toJson(requestBomb)


                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }
                            } else {
                                socketO?.run {
                                    thread {
                                        val requestBomb = RequestBomb(false)

                                        val gson = Gson()
                                        val jsonSend: String = gson.toJson(requestBomb)


                                        val printStream = PrintStream(this)
                                        printStream.println(jsonSend)
                                        printStream.flush()
                                    }
                                }
                            }
                        }
                        else if (type.toString().equals("\"REQUEST_EXCHANGE\"")) {
                            when {
                                boardGame.getTotalPieces(boardGame.getCurrentPlayer() - 1) <= 1 -> {
                                    socketO?.run {
                                        thread {
                                            val requestExchange = RequestExchange(0)

                                            val gson = Gson()
                                            val jsonSend: String = gson.toJson(requestExchange)


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
                                            val jsonSend: String = gson.toJson(requestExchange)


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
                                            val jsonSend: String = gson.toJson(requestExchange)


                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                }
                            }
                        }
                        else if (type.toString().equals("\"CLOSE_CONNECTION\"")) {
                            cleanOnExit()
                        }
                    } else {
                        when {
                            type.toString().equals("\"PROFILE_VIEW\"") -> {
                                val currentPlayer = jsonObject.get("currentPlayer").asInt
                                boardGame.setCurrentPlayer(currentPlayer)


                                val nClients = jsonObject.get("nClients").asInt

                                val users = jsonObject.get("usernames").asJsonArray
                                val usernames = ArrayList<String>()
                                for (i in 0 until nClients) {
                                    val name = users[i].toString().replace("\"", "")
                                    usernames.add(name)
                                    boardGame.setUsername(i, name)
                                }


                                val photos = jsonObject.get("photos").asJsonArray
                                val userPhotos = ArrayList<String>()
                                for (i in 0 until nClients)
                                    userPhotos.add(
                                        photos[i].toString().replace("\\n", "").replace("\"", "")
                                    )


                                gameFragment.gamePerfilView.updateUsers(
                                    nClients,
                                    usernames,
                                    userPhotos
                                )

                                when (currentPlayer - 1) {
                                    0 -> state.postValue(State.PLAYING_SERVER)
                                    1 -> state.postValue(State.PLAYING_CLIENT)
                                    2 -> state.postValue(State.PLAYING_SECOND_CLIENT)
                                }

                            }
                            type.toString().equals("\"SERVER_MOVE\"") -> {
                                //Ele só recebe esta estrutura após o servidor já ter validado

                                val posX = (jsonObject.get("move") as JsonObject).get("posX").asInt
                                val posY = (jsonObject.get("move") as JsonObject).get("posY").asInt
                                val currentPiece = jsonObject.get("currentPiece").asInt

                                when (currentPiece) {
                                    NORMAL_PIECE -> {
                                        boardGame.move(posX, posY)
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
                                        val movesX =
                                            jsonObject.get("exchangeArrayListX").asJsonArray
                                        val movesY =
                                            jsonObject.get("exchangeArrayListY").asJsonArray
                                        for (i in 0 until 3) {
                                            exchangeArrayList.add(
                                                PieceMoves(
                                                    movesX[i].asInt,
                                                    movesY[i].asInt
                                                )
                                            )
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
                                Handler(Looper.getMainLooper()).post {
                                    gameFragment.showAlertPassPlay("")
                                }
                            }
                            type.toString().equals("\"ALERT_PASS\"") -> {
                                alertEndGame()
                                state.postValue(State.GAME_OVER)
                            }
                            type.toString().equals("\"OK\"") -> {
                                if (jsonObject.get("aux").asBoolean) {
                                    boardGame.switchPlayer()
                                    boardGame.checkNoValidPlays()
                                    boardGame.checkBoardPieces()
                                    switchStatePlay()
                                }
                            }
                            type.toString().equals("\"REQUEST_BOMB\"") -> {
                                when (jsonObject.get("switchToBomb").asBoolean) {
                                    true -> boardGame.setPieceType(BOMB_PIECE)
                                    false -> {
                                        Handler(Looper.getMainLooper()).post {
                                            gameFragment.showAlertGeneral(boardGame.getName() + " has no available bomb pieces!")
                                        }
                                    }
                                }

                            }
                            type.toString().equals("\"REQUEST_EXCHANGE\"") -> {
                                when (jsonObject.get("switchToExchange").asInt) {
                                    0 -> {
                                        Handler(Looper.getMainLooper()).post {
                                            gameFragment.showAlertGeneral(boardGame.getName() + gameFragment.getExchangeNoBoardPieces())
                                        }
                                    }
                                    1 -> boardGame.setPieceType(EXCHANGE_PIECE)
                                    2 -> {
                                        Handler(Looper.getMainLooper()).post {
                                            gameFragment.showAlertGeneral(boardGame.getName() + gameFragment.getExchangeNoAvailablePieces())
                                        }
                                    }
                                }
                            }
                            type.toString().equals("\"ALERT_INVALID_BOMB\"") -> {
                                Handler(Looper.getMainLooper()).post {
                                    gameFragment.showAlertGeneral(gameFragment.getBombPieceSelect())
                                }
                            }
                            type.toString().equals("\"ALERT_INVALID_EXCHANGE\"") -> {
                                when (jsonObject.get("error").asInt) {
                                    -1 -> {
                                        Handler(Looper.getMainLooper()).post {
                                            gameFragment.showAlertGeneral(gameFragment.getExchangeBoardError())
                                        }
                                    }
                                    -2 -> {
                                        Handler(Looper.getMainLooper()).post {
                                            gameFragment.showAlertGeneral(gameFragment.getExchangeWrongPiece())
                                        }
                                    }
                                    -3 -> {
                                        Handler(Looper.getMainLooper()).post {
                                            gameFragment.showAlertGeneral(gameFragment.getExchangeSelectTwice())
                                        }
                                    }
                                }
                            }
                            type.toString().equals("\"CLOSE_CONNECTION\"") -> {
                                cleanOnExit()
                            }
                        }
                    }
                }
            } catch(exc : NullPointerException){
                cleanOnExit()
            } catch(exc : SocketException){
                cleanOnExit()
            }
            finally {
                if (connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
                    stopGame()
            }
        }
    }

    private fun sendSingleOkTrue(){
        socketO?.run {
            thread {
                val okData = OkData(true)
                val gson = Gson()
                val jsonSend = gson.toJson(okData)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()
            }
        }
    }

    private fun switchStatePlay() {
        //Quando é o cliente não à problema de estar assim
        val gamemode = boardGame.getGameMode()
        if(isServer){
            when(gamemode){
                1 -> {
                    sendSingleOkTrue()
                }
                2 -> {
                    for(j in 0 until socketArrayServer.size){
                        socketArrayServer[j].getOutputStream().run {
                            thread {
                                val okData = OkData(true)
                                val gson = Gson()
                                val jsonSend = gson.toJson(okData)

                                val printStream = PrintStream(this)
                                printStream.println(jsonSend)
                                printStream.flush()
                            }
                        }
                    }
                }
            }
        }
        else {
            sendSingleOkTrue()
        }
        when (gamemode) {
            1 -> {
                when (state.value) {
                    State.PLAYING_CLIENT -> state.postValue(State.PLAYING_SERVER)
                    State.PLAYING_SERVER -> state.postValue(State.PLAYING_CLIENT)
                    else -> {}
                }
            }
            2 -> {
                //Aqui como ja foi feita a jogada vao ver quem é a jogar e atualiza o estado
                when (boardGame.getCurrentPlayer() - 1) {
                    0 -> state.postValue(State.PLAYING_SERVER)
                    1 -> state.postValue(State.PLAYING_CLIENT)
                    2 -> state.postValue(State.PLAYING_SECOND_CLIENT)
                    else -> {}
                }
            }
        }


    }

    fun movePiece(x: Int, y: Int, piece: Int) {
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
                } else {
                    validPlay = false
                    if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                        gameFragment.showAlertGeneral(gameFragment.getBombPieceSelect())
                }
            }

            EXCHANGE_PIECE -> {
                when (boardGame.confirmExchangeMove(x, y, exchangeCounter, exchangeArrayList)) {
                    -3 -> {
                        exchangeError = -3
                        validPlay = false
                        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                            gameFragment.showAlertGeneral(gameFragment.getExchangeSelectTwice())
                    }
                    -2 -> {
                        exchangeError = -2
                        validPlay = false
                        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                            gameFragment.showAlertGeneral(gameFragment.getExchangeWrongPiece())
                    }
                    -1 -> {
                        exchangeError = -1
                        validPlay = false
                        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
                            gameFragment.showAlertGeneral(gameFragment.getExchangeBoardError())
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

    fun checkOnlineMove(x: Int?, y: Int?) {
        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED ||
            state.value != State.PLAYING_SERVER && state.value != State.PLAYING_CLIENT && state.value != State.PLAYING_SECOND_CLIENT
        ) {
            return
        }

        if (state.value == State.GAME_OVER)
            return

        if (!isServer && (state.value == State.PLAYING_CLIENT || state.value == State.PLAYING_SECOND_CLIENT)) {

            val move = PieceMoves(x!!, y!!)

            socketO?.run {
                thread {

                    val moveData = ClientMoveData(move, boardGame.getCurrentPiece())

                    val gson = Gson()
                    val jsonSend = gson.toJson(moveData)

                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()

                }

            }
        } else if (isServer && state.value == State.PLAYING_SERVER) {

            val auxPiece = boardGame.getCurrentPiece()

            movePiece(x!!, y!!, auxPiece)

            if (validPlay) {

                state.postValue(State.PLAYING_CLIENT)
                val move = PieceMoves(x, y)

                when (boardGame.getGameMode()) {
                    1 -> {
                        socketO?.run {
                            thread {
                                val moveData: ServerMoveData
                                when (auxPiece) {
                                    EXCHANGE_PIECE -> {
                                        moveData = ServerMoveData(move, auxPiece, exchangeArrayList)
                                        exchangeArrayList.clear()
                                    }
                                    else -> moveData = ServerMoveData(move, auxPiece)
                                }

                                val gson = Gson()
                                val jsonSend = gson.toJson(moveData)

                                val printStream = PrintStream(this)
                                printStream.println(jsonSend)
                                printStream.flush()

                            }
                        }
                    }
                    2 -> {
                        for (j in 0 until socketArrayServer.size) {
                            socketArrayServer[j].getOutputStream().run {
                                thread {
                                    val moveData: ServerMoveData
                                    when (auxPiece) {
                                        EXCHANGE_PIECE -> {
                                            moveData = ServerMoveData(move, auxPiece, exchangeArrayList)
                                            if(j >= socketArrayServer.size - 1)
                                                exchangeArrayList.clear()
                                        }
                                        else -> moveData = ServerMoveData(move, auxPiece)
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
        }
    }

    private fun exchangeCheck3Players(number: Int){
        when {
            boardGame.getTotalPieces(boardGame.getCurrentPlayer() - 1) <= 1 -> {
                socketArrayServer[number].getOutputStream().run {
                    thread {
                        val requestExchange = RequestExchange(0)

                        val gson = Gson()
                        val jsonSend: String = gson.toJson(requestExchange)


                        val printStream = PrintStream(this)
                        printStream.println(jsonSend)
                        printStream.flush()
                    }
                }
            }
            boardGame.getExchangePiece() > 0 -> {
                socketArrayServer[number].getOutputStream().run {
                    thread {
                        val requestExchange = RequestExchange(1)

                        val gson = Gson()
                        val jsonSend: String = gson.toJson(requestExchange)


                        val printStream = PrintStream(this)
                        printStream.println(jsonSend)
                        printStream.flush()
                    }
                }
            }
            else -> {
                socketArrayServer[number].getOutputStream().run {
                    thread {
                        val requestExchange = RequestExchange(2)

                        val gson = Gson()
                        val jsonSend: String = gson.toJson(requestExchange)


                        val printStream = PrintStream(this)
                        printStream.println(jsonSend)
                        printStream.flush()
                    }
                }
            }
        }
    }

    private fun exchangeBomb3Players(number : Int,currentPiece : Int){
        when (currentPiece) {
            BOMB_PIECE -> {
                socketArrayServer[number].getOutputStream().run {
                    val alertInvalidBomb = AlertInvalidBomb()

                    val gson = Gson()
                    val jsonSend = gson.toJson(alertInvalidBomb)

                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()
                }
            }
            EXCHANGE_PIECE -> {
                socketArrayServer[number].getOutputStream().run {
                    val alertInvalidExchange =
                        AlertInvalidExchange(exchangeError)

                    val gson = Gson()
                    val jsonSend = gson.toJson(alertInvalidExchange)

                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()
                }
            }
        }
    }

    private fun bombCheck3Players(number : Int){
        if (boardGame.getBombPiece() > 0) {
            socketArrayServer[number].getOutputStream().run  {
                thread {
                    val requestBomb = RequestBomb(true)

                    val gson = Gson()
                    val jsonSend: String = gson.toJson(requestBomb)


                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()
                }
            }
        } else {
            socketArrayServer[number].getOutputStream().run {
                thread {
                    val requestBomb = RequestBomb(false)

                    val gson = Gson()
                    val jsonSend: String = gson.toJson(requestBomb)


                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()
                }
            }
        }
    }

    fun switchBombPiece() {
        socketO?.run {
            thread {
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
            thread {
                val requestExchange = RequestExchange()

                val gson = Gson()
                val jsonSend = gson.toJson(requestExchange)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()
            }
        }
    }

    private fun alertEndGame() {
        Handler(Looper.getMainLooper()).post {
            gameFragment.showAlertEndGame(boardGame.checkWinner())
        }
        gameFragment.updateUI()
        endGame = true
    }

    private fun checkAlertNoPlays(): Boolean {
        //Muda o jogador a jogar
        boardGame.switchPlayer()

        //Verifica se ele tem jogadas disponiveis se sim saimos daqui e continuamos
        if (boardGame.checkNoValidPlays()) {
            gameFragment.updateUI()
            return true
        }
        if (counter == 0)
            gameFragment.updateUI()

        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED) //No modo offline
            gameFragment.showAlertPassPlay(boardGame.getName())
        else {
            when (boardGame.getGameMode()) {
                1 -> {
                    //Se era o cliente a jogar significa que eu nao tenho jogadas validas
                    if (state.value == State.PLAYING_CLIENT) {
                        Handler(Looper.getMainLooper()).post {
                            gameFragment.showAlertPassPlay("")
                        }
                    } else {
                        socketO?.run {
                            thread {
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
                2 -> {
                    when (state.value) {
                        State.PLAYING_SERVER -> {
                            //Vai mandar o alerta para o jogador 1
                            socketArrayServer[0].getOutputStream().run {
                                thread {
                                    val alertPlays = AlertNoValidPlayData()

                                    val gson = Gson()
                                    val jsonSend = gson.toJson(alertPlays)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }
                            }
                        }
                        State.PLAYING_CLIENT -> {
                            //Vai mandar o alerta para o jogador 2
                            socketArrayServer[1].getOutputStream().run {
                                thread {
                                    val alertPlays = AlertNoValidPlayData()

                                    val gson = Gson()
                                    val jsonSend = gson.toJson(alertPlays)

                                    val printStream = PrintStream(this)
                                    printStream.println(jsonSend)
                                    printStream.flush()
                                }
                            }
                        }
                        State.PLAYING_SECOND_CLIENT -> {
                            //Vai apresentar a mensagem no ecra do servidor
                            Handler(Looper.getMainLooper()).post {
                                gameFragment.showAlertPassPlay("")
                            }
                        }
                        else -> {}
                    }
                }
            }

        }


        //Servidor pode apresentar aqui
        //Se for cliente a jogar abre o socket e diz lhe

        return false

    }

    fun passPlayAux() {
        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED) {
            //Se for modo offline
            ++counter
            if (counter < boardGame.getPlayers()) //Quando for igual já sabemos que percorreu os jogadores todos portanto não vale apena fazer o igual
                checkAlertNoPlays()
            else
                alertEndGame()
        } else {

            if (isServer) {
                //se for o sv a receber o alerta faz a verificação
                ++counter
                if (counter < boardGame.getPlayers()) {
                    when(boardGame.getGameMode()){
                        1 -> {
                            switchStatePlay() //servidor manda ok data true
                        }
                        2 -> {
                            when (state.value) {
                                State.PLAYING_SECOND_CLIENT -> state.postValue(State.PLAYING_SERVER)
                                State.PLAYING_SERVER -> state.postValue(State.PLAYING_CLIENT)
                                State.PLAYING_CLIENT -> state.postValue(State.PLAYING_SECOND_CLIENT)
                                else -> {}
                            }
                        }
                    }
                    checkAlertNoPlays() //Se for preciso manda o alerta para outro gajo
                }
                else {
                    alertEndGame()
                    when (boardGame.getGameMode()) {
                        1 -> {
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
                        2 -> {
                            //Como ninguem tem jogadas manda o alerta de final de jogo
                            for (j in 0 until socketArrayServer.size) {
                                socketArrayServer[j].getOutputStream().run {
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
                        }
                    }

                }
            } else {
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
        }

    }

    private fun stopGame() {
        try {
            //state.postValue(State.GAME_OVER)

            socket?.close()
            socket = null
            threadComm?.interrupt()
            threadComm = null
        } catch (_: Exception) {
        }
    }

    fun stopServer() {
        serverSocket?.close()
        connectionState.postValue(ConnectionState.CONNECTION_ERROR)
        serverSocket = null
    }

    private fun cleanUp3PlayersServer(){
        //Dá exceção no server vai enviar aos clientes um notificação a dizer para sairem

        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        state.postValue(State.LEFT_GAME)
        val threadSendClose = thread {
            for(j in 0 until socketArrayServer.size){
                socketArrayServer[j].getOutputStream().run {
                    val closeConnection = CloseConnection()
                    val gson = Gson()
                    val jsonSend = gson.toJson(closeConnection)

                    val printStream = PrintStream(this)
                    printStream.println(jsonSend)
                    printStream.flush()
                }
            }
        }
        threadSendClose.join()


        endGame = false
        isServer = false

        socket?.close()
        socket = null
        threadComm?.interrupt()
        threadComm = null
    }

    private fun cleanUp() {
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        state.postValue(State.LEFT_GAME)


        val threadSendClose = thread {
            socketO?.run {

                val closeConnection = CloseConnection()
                val gson = Gson()
                val jsonSend = gson.toJson(closeConnection)

                val printStream = PrintStream(this)
                printStream.println(jsonSend)
                printStream.flush()
            }
        }
        threadSendClose.join()

        endGame = false
        isServer = false

        socket?.close()
        socket = null
        threadComm?.interrupt()
        threadComm = null
    }

    private fun cleanOnExit3Players(){
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        state.postValue(State.LEFT_GAME)

        val nSockets = socketArrayServer.size
        for(j in 0 until nSockets)
            socketArrayServer[j].close()
        socketArrayServer.clear()
        socket?.close()
        socket = null

        threadCommServer3.clear()
    }

    private fun cleanOnExit() {
        connectionState.postValue(ConnectionState.CONNECTION_ENDED)
        state.postValue(State.LEFT_GAME)

        socket?.close()
        socket = null
        threadComm?.interrupt()
        threadComm = null
    }

    fun setData(boardGame: BoardGame, gameFragment: GameFragment) {
        this.boardGame = boardGame
        this.gameFragment = gameFragment
        auth = Firebase.auth
    }

    fun getIsServer(): Boolean = isServer

    fun getEndgame(): Boolean = endGame

    fun getGameState(): State? = state.value

    fun resetCounter() {
        counter = 0
    }

    fun convertToBase64(attachment: File): String {
        return Base64.encodeToString(attachment.readBytes(), Base64.NO_WRAP)
    }

    override fun onCleared() {
        super.onCleared()
        if(!endGame){
            when(boardGame.getGameMode()){
                1 -> {
                    cleanUp()
                }
                2 -> {
                    when(isServer){
                        true -> {
                            cleanUp3PlayersServer()
                        }
                        false -> {
                            cleanUp()
                        }
                    }
                }
            }
        }
    }
}