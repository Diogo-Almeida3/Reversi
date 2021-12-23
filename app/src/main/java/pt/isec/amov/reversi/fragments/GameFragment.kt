package pt.isec.amov.reversi.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
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
    }

    private lateinit var btnAnimation: AlphaAnimation
    private val colorsPlayers = ArrayList<Int>(3)
    private val colorsBoard = ArrayList<Int>(2)
    private lateinit var boardGame: BoardGame
    private lateinit var boardView: BoardView
    private lateinit var gamePerfilView: GamePerfilView
    private var gamemode = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game,container,false)


        for(i in 0..2)
            colorsPlayers.add(resources.getIntArray(R.array.array_of_colors)[i])

        for (i in 0..2)
            colorsBoard.add(resources.getIntArray(R.array.array_of_board_colors)[i])


        gamemode = GameFragmentArgs.fromBundle(requireArguments()).game
        if(savedInstanceState == null)
            boardGame = BoardGame(gamemode,colorsPlayers,colorsBoard)
        else {
            val json = savedInstanceState.getString("CUSTOM_CLASS")
            if (!json!!.isEmpty()) {
                val gson = Gson()
                boardGame = gson.fromJson(json, BoardGame::class.java)
            }
        }

        boardView = view.findViewById(R.id.boardView)
        gamePerfilView = view.findViewById(R.id.gamePerfilView)
        boardView.setData(boardGame,gamePerfilView)
        gamePerfilView.setData(boardGame)

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
        return view
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