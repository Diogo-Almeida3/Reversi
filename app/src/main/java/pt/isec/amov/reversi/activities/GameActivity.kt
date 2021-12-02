package pt.isec.amov.reversi.activities

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.ActivityGameBinding
import pt.isec.amov.reversi.game.BoardGame
import pt.isec.amov.reversi.game.BoardView
import pt.isec.amov.reversi.game.GamePerfilView
import pt.isec.amov.reversi.game.Players

class GameActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var Toolbar: Toolbar
    private lateinit var btnAnimation : AlphaAnimation

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
    private lateinit var binding : ActivityGameBinding
    private var gamemode = GAMEOFF2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)


        Toolbar = findViewById(R.id.navToolbar)
        setSupportActionBar(Toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu -> startActivity(Intent(this,MenuActivity::class.java))
                R.id.rules -> startActivity(Intent(this,RulesActivity::class.java))
                R.id.editProfile -> startActivity(Intent(this,ProfileActivity::class.java))
                R.id.gameOn2 -> {}
                R.id.gameOn3 -> {}
            }
            true
        }

        //todo tornar este get colors dinamico
        for(i in 0..2)
            colorsPlayers.add(resources.getIntArray(R.array.array_of_colors)[i])

        for (i in 0..2)
            colorsBoard.add(resources.getIntArray(R.array.array_of_board_colors)[i])

        gamemode = intent.getIntExtra(MODO_JOGO, -1)
        boardGame = BoardGame(gamemode,colorsPlayers,colorsBoard)
        boardView = findViewById(R.id.boardView)
        gamePerfilView = findViewById(R.id.gamePerfilView)
        boardView.setData(boardGame)
        gamePerfilView.setData(boardGame)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}