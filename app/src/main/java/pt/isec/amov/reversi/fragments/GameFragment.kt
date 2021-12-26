package pt.isec.amov.reversi.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
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
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.game.BoardGame
import pt.isec.amov.reversi.game.BoardView
import pt.isec.amov.reversi.game.GamePerfilView

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
    private val colorsPlayers = ArrayList<Int>(3)
    private val colorsBoard = ArrayList<Int>(2)

    private lateinit var btnAnimation: AlphaAnimation

    private lateinit var auth : FirebaseAuth

    private lateinit var boardGame: BoardGame
    private lateinit var boardView: BoardView
    private lateinit var gamePerfilView: GamePerfilView

    private var dlg: androidx.appcompat.app.AlertDialog? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game,container,false)
        auth = Firebase.auth

        gamemode = GameFragmentArgs.fromBundle(requireArguments()).game
        webMode = GameFragmentArgs.fromBundle(requireArguments()).online



        getColors()
        restoreData(savedInstanceState)
        writeData(view)

        when(gamemode){
            0-> {

                setButtons(view)
                boardGame.setUsername(0,getName())
                updateUI()
            }
            1 -> {

                boardView.state.observe(viewLifecycleOwner){state ->
                    if(state == BoardView.State.PLAYING_SERVER || state == BoardView.State.PLAYING_CLIENT)
                        updateUI()

                    if(state == BoardView.State.GAME_OVER)
                        updateUI()
                }

                boardView.connectionState.observe(viewLifecycleOwner){ state ->
                    if (state != BoardView.ConnectionState.SETTING_PARAMETERS &&
                        state != BoardView.ConnectionState.SERVER_CONNECTING &&
                        dlg?.isShowing == true) {
                        dlg?.dismiss()
                        dlg = null
                    }
                    if (state == BoardView.ConnectionState.CONNECTION_ERROR) {

                        //Toast.makeText(context,"ERRO",Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
                    }
                    if (state == BoardView.ConnectionState.CONNECTION_ENDED){
                        //Toast.makeText(context,"ENDED",Toast.LENGTH_LONG).show()
                    }

                }
                if(boardView.connectionState.value != BoardView.ConnectionState.CONNECTION_ESTABLISHED){
                    when(webMode){
                        SERVER_MODE -> startAsServer()
                        CLIENT_MODE -> startAsClient()
                    }
                }

            }
        }

        return view
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
            .setTitle("Client mode")
            .setMessage("Server IP address:")
            .setPositiveButton("Connect") { _: DialogInterface, _: Int ->
                val strIP = edtBox.text.toString()
                if (strIP.isEmpty() || !Patterns.IP_ADDRESS.matcher(strIP).matches()) {
                    Toast.makeText(context, "Address not recognized", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
                } else {
                    boardView.startClient(getName(),strIP)
                }
            }
            .setNeutralButton("Connect to emulator") { _: DialogInterface, _: Int ->
                boardView.startClient(getName(),"10.0.2.2", 9998)
            }
            .setNegativeButton("Cancel") { _: DialogInterface, _: Int ->

                findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
            }
            .setCancelable(false)
            .setView(edtBox)
            .create()

        dlg.show()
    }

    private fun startAsServer() {
        val wifiManager = context?.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress // Deprecated in API Level 31. Suggestion NetworkCallback
        val strIPAddress = String.format("%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )

        val ll = LinearLayout(context).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            this.setPadding(50, 50, 50, 50)
            layoutParams = params
            setBackgroundColor(Color.rgb(240, 224, 208))
            orientation = LinearLayout.HORIZONTAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
                val paramsPB = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                paramsPB.gravity = Gravity.CENTER_VERTICAL
                layoutParams = paramsPB
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(96, 96, 32))
            })
            addView(TextView(context).apply {
                val paramsTV = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams = paramsTV
                text = String.format("IP do servidor: %s\nÀ espera de um cliente...",strIPAddress)
                textSize = 20f
                setTextColor(Color.rgb(96, 96, 32))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            })
        }

        dlg = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Server Mode")
            .setView(ll)
            .setOnCancelListener {
                boardView.stopServer()
            }
            .create()

        boardView.startServer(getName())

        dlg?.show()

    }

    private fun getName() : String{
        val navigationView = requireActivity().findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        return navigationView.findViewById<TextView>(R.id.userName).text as String
    }

    private fun updateUI(){
        boardView.invalidate()
        gamePerfilView.invalidate()
    }



























    private fun restoreData(savedInstanceState: Bundle?) {
        if(savedInstanceState == null)
            boardGame = BoardGame(gamemode,colorsPlayers,colorsBoard)
        else {
            val json = savedInstanceState.getString("CUSTOM_CLASS")
            if (!json!!.isEmpty()) {
                val gson = Gson()
                boardGame = gson.fromJson(json, BoardGame::class.java)
            }
        }
    }

    private fun setButtons(view: View) {
        btnAnimation = AlphaAnimation(1F,0.8F).apply {
            duration = 500
            interpolator = AccelerateInterpolator(0.05F)
        }
        val buttonBomb = view.findViewById<Button>(R.id.btnBombPiece)
        val buttonExchange = view.findViewById<Button>(R.id.btnTradePiece)

        buttonBomb.setOnClickListener {

            buttonBomb.startAnimation(btnAnimation)
            if(boardGame.getBombPiece() > 0)
                boardGame.setPieceType(1)
            else
                showAlert(boardGame.getName() + " has no avaible bomb pieces!")
        }


        buttonExchange.setOnClickListener {
            buttonExchange.startAnimation(btnAnimation)
            when {
                boardGame.getTotalPieces(boardGame.getCurrentPlayer() - 1) <= 1 -> showAlert(boardGame.getName() + " don't got enough pieces!")
                boardGame.getExchangePiece() > 0 -> boardGame.setPieceType(2)
                else -> showAlert(boardGame.getName() + " has no available exchange pieces!")
            }
        }
    }

    private fun writeData(view: View) {
        boardView = view.findViewById(R.id.boardView)
        gamePerfilView = view.findViewById(R.id.gamePerfilView)
        boardView.setData(boardGame,gamePerfilView)
        gamePerfilView.setData(boardGame)
    }

    private fun getColors(){
        for(i in 0..2)
            colorsPlayers.add(resources.getIntArray(R.array.array_of_colors)[i])

        for (i in 0..2)
            colorsBoard.add(resources.getIntArray(R.array.array_of_board_colors)[i])
    }

    private fun showAlert(phrase: String) {

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val gson = Gson()
        val json = gson.toJson(boardGame)
        outState.putString("CUSTOM_CLASS", json)
    }


}