package pt.isec.amov.reversi.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.widget.Button
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import pt.isec.amov.reversi.R
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class MenuActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var Toolbar: Toolbar
    private lateinit var btnRules: Button
    private lateinit var btnExit : Button
    private lateinit var btn2Off: Button
    private lateinit var btn2On : Button
    private lateinit var btn3On: Button
    private lateinit var btnAnimation : AlphaAnimation


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

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

                /* Botões da navigation */
                R.id.rules -> startActivity(Intent(this,RulesActivity::class.java))
            }
            true
        }


        btnAnimation = AlphaAnimation(0.9F,0.5F).apply {
            duration = 1000
            interpolator = AccelerateInterpolator(0.1F)
        }

        btnRules = findViewById(R.id.btnRules)
        btnExit = findViewById(R.id.btnExit)
        btn2Off = findViewById(R.id.btnModo1)
        btn2On = findViewById(R.id.btnModo2)
        btn3On = findViewById(R.id.btnModo3)

        btnRules.setOnClickListener{
            btnRules.startAnimation(btnAnimation)
            startActivity(Intent(this,RulesActivity::class.java))
        }

        btnExit.setOnClickListener{
            btnExit.startAnimation(btnAnimation)
            finish()
        }
        btn2Off.setOnClickListener{
            btn2Off.startAnimation(btnAnimation)

        }
        btn2On.setOnClickListener{
            btn2On.startAnimation(btnAnimation)

        }
        btn3On.setOnClickListener{
            btn3On.startAnimation(btnAnimation)

        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}