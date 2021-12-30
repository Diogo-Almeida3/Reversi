package pt.isec.amov.reversi.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Base64
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.game.*
import kotlin.concurrent.thread

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import java.io.ByteArrayOutputStream


class GameFragment : Fragment() {

    companion object {
        const val GAMEOFF2 = 0
        const val GAMEON2 = 1
        const val GAMEON3 = 2

        const val SERVER_MODE = 0
        const val CLIENT_MODE = 1
    }

    private var gamemode = -1
    private var webMode = -1
    private val model: GameViewModel by viewModels()
    private var db = Firebase.firestore
    private var dlg: androidx.appcompat.app.AlertDialog? = null


    private lateinit var btnAnimation: AlphaAnimation
    private lateinit var auth: FirebaseAuth
    lateinit var boardGame: BoardGame
    private lateinit var boardView: BoardView
    private lateinit var gamePerfilView: GamePerfilView

    private val colorsPlayers = ArrayList<Int>(3)
    private val colorsBoard = ArrayList<Int>(2)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)
        auth = Firebase.auth

        if (savedInstanceState == null) {
            gamemode = GameFragmentArgs.fromBundle(requireArguments()).game
            webMode = GameFragmentArgs.fromBundle(requireArguments()).online
        }

        startGame(savedInstanceState, view)
        model.setData(boardGame, this)

        when (gamemode) {
            0 -> {
                boardGame.setUsername(0, getName())
                updateUI()
            }
            1 -> {
                model.state.observe(viewLifecycleOwner) { state ->
                    if (state == GameViewModel.State.PLAYING_SERVER || state == GameViewModel.State.PLAYING_CLIENT)
                        updateUI()

                    if (state == GameViewModel.State.GAME_OVER && model.connectionState.value == GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
                        updateUI()
                        when (model.getIsServer()) {
                            true -> UploadTopScorePlayers(getName(), boardGame.getUsername(1), "",
                                boardGame.getTotalPieces(0), boardGame.getTotalPieces(1), -1)
                            false -> UploadTopScorePlayers(getName(), boardGame.getUsername(0), "",
                                boardGame.getTotalPieces(1), boardGame.getTotalPieces(0), -1)
                        }
                    } else if (state == GameViewModel.State.LEFT_GAME && model.connectionState.value != GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
                        moveToOff(savedInstanceState, view)
                    }
                }

                model.connectionState.observe(viewLifecycleOwner) { state ->
                    if (state != GameViewModel.ConnectionState.SETTING_PARAMETERS &&
                        state != GameViewModel.ConnectionState.SERVER_CONNECTING &&
                        dlg?.isShowing == true
                    ) {
                        dlg?.dismiss()
                        dlg = null
                    }
                    if (state == GameViewModel.ConnectionState.CONNECTION_ERROR) {

                        findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
                    }
                    if (state == GameViewModel.ConnectionState.CONNECTION_ENDED) {
                        Toast.makeText(context, resources.getString(R.string.lostConnection), Toast.LENGTH_LONG)
                            .show()
                    }

                }
                if (model.connectionState.value != GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
                    when (webMode) {
                        SERVER_MODE -> startAsServer()
                        CLIENT_MODE -> startAsClient()
                    }
                }

            }
            2 -> {
                model.state.observe(viewLifecycleOwner) { state ->
                    if (state == GameViewModel.State.PLAYING_SERVER || state == GameViewModel.State.PLAYING_CLIENT || state == GameViewModel.State.PLAYING_SECOND_CLIENT)
                        updateUI()
                    if (state == GameViewModel.State.GAME_OVER && model.connectionState.value == GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
                        val name = getName()
                        var aux = 0
                        for (i in 0..2){
                            if (name.equals(boardGame.getUsername(i))){
                                aux = i
                                break
                            }
                        }
                        when(aux){
                            0 ->  {
                                UploadTopScorePlayers(
                                    name,
                                    boardGame.getUsername(1),
                                    boardGame.getUsername(2),
                                    boardGame.getTotalPieces(0),
                                    boardGame.getTotalPieces(1),
                                    boardGame.getTotalPieces(2)
                                )
                            }
                            1 -> {
                                UploadTopScorePlayers(
                                    name,
                                    boardGame.getUsername(0),
                                    boardGame.getUsername(2),
                                    boardGame.getTotalPieces(1),
                                    boardGame.getTotalPieces(0),
                                    boardGame.getTotalPieces(2)
                                )
                            }
                            2 -> {
                                UploadTopScorePlayers(
                                    name,
                                    boardGame.getUsername(0),
                                    boardGame.getUsername(1),
                                    boardGame.getTotalPieces(2),
                                    boardGame.getTotalPieces(0),
                                    boardGame.getTotalPieces(1)
                                )
                            }
                        }
                    } else if (state == GameViewModel.State.LEFT_GAME && model.connectionState.value != GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
                        moveToOff(savedInstanceState, view)
                    }

                }

                model.connectionState.observe(viewLifecycleOwner) { state ->
                    if (state != GameViewModel.ConnectionState.SETTING_PARAMETERS &&
                        state != GameViewModel.ConnectionState.SERVER_CONNECTING &&
                        dlg?.isShowing == true
                    ) {
                        dlg?.dismiss()
                        dlg = null
                    }
                    if (state == GameViewModel.ConnectionState.CONNECTION_ERROR) {
                        findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
                    }
                    if (state == GameViewModel.ConnectionState.CONNECTION_ENDED) {
                        Toast.makeText(context, resources.getString(R.string.lostConnection), Toast.LENGTH_LONG)
                            .show()
                    }
                }

                if (model.connectionState.value != GameViewModel.ConnectionState.CONNECTION_ESTABLISHED) {
                    when (webMode) {
                        CLIENT_MODE -> startAsClient()
                        SERVER_MODE -> startAsServer()
                    }
                }
            }
        }
        return view
    }

    private fun startGame(savedInstanceState: Bundle?, view: View) {
        getColors()
        restoreData(savedInstanceState)
        writeData(view)
        setButtons(view)
    }

    private fun getColors() {
        for (i in 0..2)
            colorsPlayers.add(resources.getIntArray(R.array.array_of_colors)[i])

        for (i in 0..2)
            colorsBoard.add(resources.getIntArray(R.array.array_of_board_colors)[i])
    }

    private fun restoreData(savedInstanceState: Bundle?) {
        if (savedInstanceState == null)
            boardGame = BoardGame(gamemode, colorsPlayers, colorsBoard)
        else {
            val threadCom = thread {
                val json = savedInstanceState.getString("CUSTOM_CLASS")
                if (!json!!.isEmpty()) {
                    val gson = Gson()
                    boardGame = gson.fromJson(json, BoardGame::class.java)
                    gamemode = boardGame.getGameMode()
                }
            }
            threadCom.join()
        }
    }

    private fun writeData(view: View) {
        boardView = view.findViewById(R.id.boardView)
        gamePerfilView = view.findViewById(R.id.gamePerfilView)
        boardView.setData(this)
        gamePerfilView.setData(this)
    }

    private fun setButtons(view: View) {
        btnAnimation = AlphaAnimation(1F, 0.8F).apply {
            duration = 450
            interpolator = AccelerateInterpolator(0.05F)
        }
        val buttonBomb = view.findViewById<Button>(R.id.btnBombPiece)
        val buttonExchange = view.findViewById<Button>(R.id.btnTradePiece)

        buttonBomb.setOnClickListener {

            buttonBomb.startAnimation(btnAnimation)
            if(boardGame.getCurrentPiece() == GameViewModel.NORMAL_PIECE){
                if (GameViewModel.ConnectionState.CONNECTION_ESTABLISHED != model.connectionState.value)
                    bombFunc()
                else {
                    when (model.getIsServer()) {
                        true -> {
                            if (model.getGameState() == GameViewModel.State.PLAYING_SERVER)
                                bombFunc()
                        } //Server
                        false -> {
                            if (model.getGameState() == GameViewModel.State.PLAYING_CLIENT || model.getGameState() == GameViewModel.State.PLAYING_SECOND_CLIENT)
                                model.switchBombPiece()
                        } //Cliente
                    }
                }
            }
        }


        buttonExchange.setOnClickListener {
            buttonExchange.startAnimation(btnAnimation)
            if(boardGame.getCurrentPiece() == GameViewModel.NORMAL_PIECE){
                if (GameViewModel.ConnectionState.CONNECTION_ESTABLISHED != model.connectionState.value)
                    ExchangeFunc()
                else
                    when (model.getIsServer()) {
                        true -> {
                            if (model.getGameState() == GameViewModel.State.PLAYING_SERVER)
                                ExchangeFunc()
                        } //Server
                        false -> {
                            if (model.getGameState() == GameViewModel.State.PLAYING_CLIENT || model.getGameState() == GameViewModel.State.PLAYING_SECOND_CLIENT) {
                                model.switchExchangePiece()
                            }
                        } //Cliente
                    }
            }
        }
    }

    fun updateUI() {
        boardView.invalidate()
        gamePerfilView.invalidate()
    }

    private fun moveToOff(savedInstanceState: Bundle?, view: View) {
        gamemode = 0
        colorsBoard.clear()
        colorsPlayers.clear()

        startGame(savedInstanceState, view)
        boardView.measure(boardView.measuredWidth,boardView.measuredHeight)
        boardGame.setUsername(0, getName())

        model.setData(boardGame, this)
        updateUI()
    }


    private fun bombFunc() {
        if (boardGame.getBombPiece() > 0)
            boardGame.setPieceType(1)
        else
            showAlertPieces(boardGame.getName() + " has no available bomb pieces!")
    }

    private fun ExchangeFunc() {
        when {
            boardGame.getTotalPieces(boardGame.getCurrentPlayer() - 1) <= 1 -> showAlertPieces(
                boardGame.getName() + resources.getString(
                    R.string.exchangeNoBoardPieces
                )
            )
            boardGame.getExchangePiece() > 0 -> boardGame.setPieceType(2)
            else -> showAlertPieces(boardGame.getName() + resources.getString(R.string.exchangeNoAvailablePieces))
        }
    }

    fun resetCounter() {
        model.resetCounter()
    }

    fun movePiece(x: Int, y: Int, currentPiece: Int) {
        model.movePiece(x, y, currentPiece)
    }

    fun checkOnlineMove(x: Int?, y: Int?) {
        model.checkOnlineMove(x, y)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(boardGame)
        outState.putString("CUSTOM_CLASS", json)

    }


    private fun showAlertPieces(phrase: String) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(phrase)
        builder1.setCancelable(false)

        builder1.setPositiveButton("Ok") { dialog, id ->
            run {
                dialog.cancel()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    fun showAlertEndGame(player: Player?) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)

        if (player != null)
            builder1.setMessage(player.getUsername() + resources.getString(R.string.winner))
        else
            builder1.setMessage(resources.getString(R.string.draw))
        builder1.setCancelable(false)

        builder1.setPositiveButton(resources.getString(R.string.checkBoard)) { dialog, id ->
            run {
                dialog.cancel()

                if (model.connectionState.value == GameViewModel.ConnectionState.CONNECTION_ESTABLISHED)
                    model.connectionState.postValue(GameViewModel.ConnectionState.CONNECTION_ENDED)
            }
        }

        builder1.setNegativeButton(resources.getString(R.string.backToMenu)) { dialog, id ->
            run {
                dialog.cancel()
                if (model.connectionState.value == GameViewModel.ConnectionState.CONNECTION_ESTABLISHED)
                    model.connectionState.postValue(GameViewModel.ConnectionState.CONNECTION_ERROR)
                else
                    findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }

    fun showAlertGeneral(phrase: String) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(phrase)
        builder1.setCancelable(false)

        builder1.setPositiveButton(resources.getString(R.string.pass)) { dialog, id ->
            run {
                dialog.cancel()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }

    fun showAlertPassPlay(name: String) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        if (name.equals(""))
            builder1.setMessage(resources.getString(R.string.noAvailablePlays))
        else
            builder1.setMessage(name + resources.getString(R.string.noAvailablePlaysName))
        builder1.setCancelable(false)

        builder1.setPositiveButton(resources.getString(R.string.pass)) { dialog, id ->
            run {
                dialog.cancel()
                model.passPlayAux()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }


    fun setUsersProfileData(name: String, convertToBase64: String) {
        gamePerfilView.setUsersProfileData(name, convertToBase64)
    }


    private fun getName(): String {
        //val navigationView = requireActivity().findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        //return navigationView.findViewById<TextView>(R.id.userName).text as String
        val aux = db.collection("Users").document(auth.currentUser!!.uid).get()

        val threadCom = thread {
            Tasks.await(aux)
        }
        threadCom.join()

        return aux.result.data!!["username"].toString()
    }

    fun getBitmaps(): ArrayList<String> {
        val auxBase64 = ArrayList<String>()
        for (i in 0 until getnClients()) {
            val baos = ByteArrayOutputStream()
            boardGame.getPhoto(i)?.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            auxBase64.add(Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT))
            baos.close()
        }

        return auxBase64
    }

    fun getnClients(): Int = boardGame.getNClients()

    fun getUsernames(): ArrayList<String> {
        val auxNames = ArrayList<String>()
        for (i in 0 until getnClients())
            auxNames.add(boardGame.getUsername(i))

        return auxNames
    }

    fun getExchangeWrongPiece(): String = resources.getString(R.string.exchangeWrongPiece)

    fun getExchangeBoardError(): String = resources.getString(R.string.exchangeBoardError)

    fun getExchangeSelectTwice(): String = resources.getString(R.string.exchangeSelectTwice)

    fun getBombPieceSelect(): String = resources.getString(R.string.bombPieceSelect)

    fun getExchangeNoAvailablePieces(): String = resources.getString(R.string.exchangeNoAvailablePieces)

    fun getExchangeNoBoardPieces(): String = resources.getString(R.string.exchangeNoBoardPieces)

    fun getEndgame(): Boolean = model.getEndgame()


    private fun UploadTopScorePlayers(user: String, opponent: String, opponent2: String,myScore: Int, opponentScore: Int,opponent2Score : Int){
        val db = Firebase.firestore
        val game = hashMapOf(
            "gamemode" to gamemode,
            "user" to user,
            "opponent" to opponent,
            "opponent2" to opponent2,
            "myScore" to myScore,
            "opponentScore" to opponentScore,
            "opponent2Score" to opponent2Score,
        )

        val path = db.collection("Users").document(auth.currentUser!!.uid)
        var numberGames = 0
        //Update que quantos jogos já realizou
        db.runTransaction { transition ->
            val doc = transition.get(path)
            numberGames = doc.getLong("nrgames")!!.toInt()
            numberGames++
            transition.update(path, "nrgames", numberGames)
            null
        }

        var min = -1L
        var id = 1
        var aux = -1L
        var count = 0
        for (i in 5 downTo 1) {
            val query = db.collection("Users")
                .document(auth.currentUser!!.uid)
                .collection("TopScores")
                .document(i.toString()).get(Source.SERVER)
            val threadAux = thread {
                Tasks.await(query)
            }
            threadAux.join()

            if (query.result.data != null) {
                count++
                aux = query.result.data!!["myScore"] as Long
                if (min == -1L) min = aux

                if (min > aux) {
                    id = i
                    min = aux
                }
            }
        }



        if (count < 5) {
            count++
            path.collection("TopScores")
                .document(count.toString())
                .set(game)
        } else if (min < myScore) {
            val pathTopScore = db.collection("Users").document(auth.currentUser!!.uid)
                .collection("TopScores").document(id.toString())

            db.runTransaction { transition ->
                transition.update(pathTopScore, "gamemode", gamemode)
                transition.update(pathTopScore, "user", user)
                transition.update(pathTopScore, "opponent", opponent)
                transition.update(pathTopScore, "myScore", myScore)
                transition.update(pathTopScore, "opponentScore", opponentScore)
                transition.update(pathTopScore, "opponent2", opponent2)
                transition.update(pathTopScore, "opponent2Score", opponent2Score)
                null
            }
        }
    }

    private fun startAsClient() {

        val edtBox = EditText(context).apply {
            maxLines = 1
            filters = arrayOf(object : InputFilter {
                override fun filter(
                    source: CharSequence?,
                    start: Int,
                    end: Int,
                    dest: Spanned?,
                    dstart: Int,
                    dend: Int
                ): CharSequence? {
                    source?.run {
                        var ret = ""
                        forEach {
                            if (it.isDigit() || it.equals('.'))
                                ret += it
                        }
                        return ret
                    }
                    return null
                }

            })
        }
        val dlg = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.clientMode))
            .setMessage(resources.getString(R.string.serverIp))
            .setPositiveButton(resources.getString(R.string.connect)) { _: DialogInterface, _: Int ->
                val strIp = edtBox.text.toString()
                if (strIp.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIp).matches()) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.adressNotRecognized),
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
                } else {
                    model.startClient(getName(), strIp)
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _: DialogInterface, _: Int ->

                findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
            }
            .setCancelable(false)
            .setView(edtBox)
            .create()

        dlg.show()
    }

    private fun startAsServer() {
        val wifiManager = context?.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val ip =
            wifiManager.connectionInfo.ipAddress // Deprecated in API Level 31. Suggestion NetworkCallback
        val strIPAddress = String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )

        val ll = LinearLayout(context).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = paramsTV
                text = String.format(
                    resources.getString(R.string.serverIp) + ": %s\n" + resources.getString(R.string.waitingForClient),
                    strIPAddress
                )
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        dlg = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.serverMode))
            .setView(ll)
            .setOnCancelListener {
                model.stopServer()
            }
            .create()

        val aux = getName()
        model.startServer(aux)

        dlg?.show()

    }
}