package pt.isec.amov.reversi.game

import android.app.AlertDialog
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

class GameViewModel : ViewModel() {

    enum class State {
        STARTING, SETTING_PROFILE_DATA, PLAYING_SERVER, PLAYING_CLIENT, GAME_OVER,LEFT_GAME
    }

    enum class ConnectionState {
        SETTING_PARAMETERS, SERVER_CONNECTING, CLIENT_CONNECTING, CONNECTION_ESTABLISHED,
        CONNECTION_ERROR, CONNECTION_ENDED,CONNECTION_TIMEOUT
    }

    val state = MutableLiveData(State.STARTING)

    val connectionState = MutableLiveData(ConnectionState.SETTING_PARAMETERS)

    private var exchangeArrayList = ArrayList<PieceMoves>()
    private var exchangeCounter = 0

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
    private var isServer = false
    private var endGame = false


    private lateinit var auth : FirebaseAuth
    private lateinit var boardGame: BoardGame
    private lateinit var gameFragment: GameFragment

    fun setData(boardGame: BoardGame,gameFragment: GameFragment) {
        this.boardGame = boardGame
        this.gameFragment = gameFragment
        auth = Firebase.auth
    }


    fun getIsServer():Boolean = isServer

    fun getEndgame() : Boolean = endGame

    fun getConnectionState(): ConnectionState? = connectionState.value

    fun getGameState() : State? = state.value

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
                            gameFragment.setUsersProfileData(jsonObject.get("name").toString(),jsonObject.get("photo").toString())

                            val currentPlayer = boardGame.getCurrentPlayer()
                            socketO?.run {

                                val gamePerfilData = GamePerfilData(gameFragment.getnClients(),gameFragment.getUsernames(),gameFragment.getBitmaps(),currentPlayer)

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
                                    val moveData : ServerMoveData
                                    when(currentPiece){
                                        BoardView.EXCHANGE_PIECE ->{
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
                                    BoardView.BOMB_PIECE -> {
                                        socketO?.run {
                                            val alertInvalidBomb = AlertInvalidBomb()

                                            val gson = Gson()
                                            val jsonSend = gson.toJson(alertInvalidBomb)

                                            val printStream = PrintStream(this)
                                            printStream.println(jsonSend)
                                            printStream.flush()
                                        }
                                    }
                                    BoardView.EXCHANGE_PIECE -> {
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
                            cleanTimeout()
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


                                gameFragment.gamePerfilView.updateUsers(nClients,usernames,userPhotos)
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
                                    BoardView.NORMAL_PIECE -> {
                                        boardGame.move(posX,posY)
                                        boardGame.checkBoardPieces()
                                        boardGame.switchPlayer()
                                        switchStatePlay()
                                    }
                                    BoardView.BOMB_PIECE -> {
                                        boardGame.pieceBomb(posX, posY)
                                        boardGame.checkBoardPieces()
                                        boardGame.switchPlayer()
                                        switchStatePlay()
                                    }
                                    BoardView.EXCHANGE_PIECE -> {
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
                                    gameFragment.showAlertPassPlay("")
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
                                    true -> boardGame.setPieceType(BoardView.BOMB_PIECE)
                                    false -> {
                                        Handler(Looper.getMainLooper()).post{
                                            gameFragment.showAlertGeneral(boardGame.getName() + " has no available bomb pieces!")
                                        }
                                    }
                                }

                            }
                            type.toString().equals("\"REQUEST_EXCHANGE\"") -> {
                                when(jsonObject.get("switchToExchange").asInt){
                                    0 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            gameFragment.showAlertGeneral(boardGame.getName() + gameFragment.getExchangeNoBoardPieces())
                                        }
                                    }
                                    1 -> boardGame.setPieceType(BoardView.EXCHANGE_PIECE)
                                    2 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            gameFragment.showAlertGeneral(boardGame.getName() + gameFragment.getExchangeNoAvailablePieces())
                                        }
                                    }
                                }
                            }
                            type.toString().equals("\"ALERT_INVALID_BOMB\"") -> {
                                Handler(Looper.getMainLooper()).post{
                                    gameFragment.showAlertGeneral(gameFragment.getBombPieceSelect())
                                }
                            }
                            type.toString().equals("\"ALERT_INVALID_EXCHANGE\"") -> {
                                when(jsonObject.get("error").asInt) {
                                    -1 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            gameFragment.showAlertGeneral(gameFragment.getExchangeBoardError())
                                        }
                                    }
                                    -2 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            gameFragment.showAlertGeneral(gameFragment.getExchangeWrongPiece())
                                        }
                                    }
                                    -3 -> {
                                        Handler(Looper.getMainLooper()).post{
                                            gameFragment.showAlertGeneral(gameFragment.getExchangeSelectTwice())
                                        }
                                    }
                                }
                            }
                            type.toString().equals("\"CLOSE_CONNECTION\"") -> {
                                cleanTimeout()
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
            gameFragment.setUsersProfileData(name,convertToBase64(uri))
        else
            gameFragment.setUsersProfileData(name,"null")

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

        state.postValue(State.LEFT_GAME)

    }

    fun alertEndGame() {
        val aux = boardGame.checkWinner()
        if(connectionState.value == ConnectionState.CONNECTION_ESTABLISHED)
            Handler(Looper.getMainLooper()).post{
                gameFragment.showAlertEndGame(aux)
            }
        else
            gameFragment.showAlertEndGame(aux)
        gameFragment.updateUI()
        endGame = true
    }

    fun checkAlertNoPlays(): Boolean {
        //Muda o jogador a jogar
        boardGame.switchPlayer()

        //Verifica se ele tem jogadas disponiveis se sim saimos daqui e continuamos
        if (boardGame.checkNoValidPlays()) {
            gameFragment.updateUI()
            return true
        }
        if (counter == 0)
            gameFragment.updateUI()

        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED)
            gameFragment.showAlertPassPlay(boardGame.getName())
        else{
            if(state.value == State.PLAYING_CLIENT){
                Handler(Looper.getMainLooper()).post{
                    gameFragment.showAlertPassPlay("")
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



    fun resetCounter() {
        counter = 0
    }

    fun check2OnlineMove(x: Int?, y: Int?) {
        if (connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value  != State.PLAYING_SERVER && state.value  != State.PLAYING_CLIENT){
            return
        }

        if(state.value  == State.GAME_OVER)
            return

        if (!isServer && state.value  == State.PLAYING_CLIENT) {

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
        else if(isServer && state.value  == State.PLAYING_SERVER){

            val auxPiece = boardGame.getCurrentPiece()

            movePiece(x!!,y!!,auxPiece)

            if(validPlay){
                state.postValue(State.PLAYING_CLIENT)
                val move = PieceMoves(x,y)

                socketO?.run {

                    thread{
                        val moveData : ServerMoveData
                        when(auxPiece){
                            BoardView.EXCHANGE_PIECE -> {
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

    fun movePiece(x: Int, y:Int,piece: Int){
        when (piece) {
            BoardView.NORMAL_PIECE -> {
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

            BoardView.BOMB_PIECE -> {
                if (boardGame.confirmBombMove(x, y)) {
                    validPlay = true
                    boardGame.pieceBomb(x, y)
                    boardGame.checkBoardPieces()
                    checkAlertNoPlays()
                } else{
                    validPlay = false
                    if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value  == State.PLAYING_SERVER)
                        gameFragment.showAlertGeneral(gameFragment.getBombPieceSelect())
                }
            }

            BoardView.EXCHANGE_PIECE -> {
                when (boardGame.confirmExchangeMove(x, y, exchangeCounter, exchangeArrayList)) {
                    -3 ->{
                        exchangeError = -3
                        validPlay = false
                        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value  == State.PLAYING_SERVER)
                            gameFragment.showAlertGeneral(gameFragment.getExchangeSelectTwice())
                    }
                    -2 -> {
                        exchangeError = -2
                        validPlay = false
                        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value  == State.PLAYING_SERVER)
                            gameFragment.showAlertGeneral(gameFragment.getExchangeWrongPiece())
                    }
                    -1 -> {
                        exchangeError = -1
                        validPlay = false
                        if(connectionState.value != ConnectionState.CONNECTION_ESTABLISHED || state.value == State.PLAYING_SERVER)
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


    fun passPlayAux(){
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