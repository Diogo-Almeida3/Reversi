package pt.isec.amov.reversi.activities

import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.ActivityGameBinding
import pt.isec.amov.reversi.game.BoardView

class GameActivity : AppCompatActivity() {

    companion object {
        const val GAMEOFF2 = 0
        const val GAMEON2 = 1
        const val GAMEON3 = 2

        const val MODO_JOGO = "modo"

    }

    private lateinit var boardView: BoardView
    private lateinit var binding: ActivityGameBinding
    private var gamemode = GAMEOFF2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gamemode = intent.getIntExtra(MODO_JOGO, GAMEOFF2)
        boardView = findViewById(R.id.boardView)
        boardView.setData(this, gamemode)


    }
}