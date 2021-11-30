package pt.isec.amov.reversi.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.ActivityGameBinding
import pt.isec.amov.reversi.game.BoardGame
import pt.isec.amov.reversi.game.BoardView

class GameActivity : AppCompatActivity() {

    companion object {
        const val GAMEOFF2 = 0
        const val GAMEON2 = 1
        const val GAMEON3 = 2

        const val MODO_JOGO = "modo"

    }
    private val colors = ArrayList<Int>(3)
    private lateinit var boardGame: BoardGame
    private lateinit var boardView: BoardView
    private lateinit var binding: ActivityGameBinding
    private var gamemode = GAMEOFF2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        for(i in 0..2)
            colors.add(resources.getIntArray(R.array.array_of_colors)[i])

        gamemode = intent.getIntExtra(MODO_JOGO, -1)
        boardGame = BoardGame(gamemode,colors)
        boardView = findViewById(R.id.boardView)
        boardView.setData(this, gamemode,boardGame)


    }
}