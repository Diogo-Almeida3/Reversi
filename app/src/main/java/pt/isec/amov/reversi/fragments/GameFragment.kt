package pt.isec.amov.reversi.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.game.BoardGame
import pt.isec.amov.reversi.game.BoardView
import pt.isec.amov.reversi.game.GamePerfilView

class GameFragment : Fragment() {

    companion object {
        const val GAMEOFF2 = 0
        const val GAMEON2 = 1
        const val GAMEON3 = 2

        const val MODO_JOGO = "modo"

    }
    private val colorsPlayers = ArrayList<Int>(3)
    private val colorsBoard = ArrayList<Int>(2)
    private lateinit var boardGame: BoardGame
    private lateinit var boardView: BoardView
    private lateinit var gamePerfilView: GamePerfilView
    private var gamemode = -1
    private lateinit var bundle : Bundle

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
        boardGame = BoardGame(gamemode,colorsPlayers,colorsBoard)
        boardView = view.findViewById(R.id.boardView)
        gamePerfilView = view.findViewById(R.id.gamePerfilView)
        boardView.setData(boardGame)
        gamePerfilView.setData(boardGame)

        return view
    }
}