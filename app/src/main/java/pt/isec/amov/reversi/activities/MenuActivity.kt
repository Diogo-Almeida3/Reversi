package pt.isec.amov.reversi.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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
import pt.isec.amov.reversi.databinding.ActivityMenuBinding
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class MenuActivity : AppCompatActivity() {

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var Toolbar: Toolbar
    private lateinit var btnAnimation : AlphaAnimation

    private lateinit var binding : ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
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

                /* Botões da navigation */
                R.id.rules -> startActivity(Intent(this,RulesActivity::class.java))
            }
            true
        }


        btnAnimation = AlphaAnimation(0.9F,0.5F).apply {
            duration = 1000
            interpolator = AccelerateInterpolator(0.1F)
        }

        binding.btnRules.setOnClickListener {
            binding.btnRules.startAnimation(btnAnimation)
            startActivity(Intent(this,RulesActivity::class.java))
        }

        binding.btnExit.setOnClickListener{
            binding.btnExit.startAnimation(btnAnimation)
            finish()
        }
        binding.btnMode1.setOnClickListener{
            binding.btnMode1.startAnimation(btnAnimation)
        }

        binding.btnMode2.setOnClickListener{
            binding.btnMode2.startAnimation(btnAnimation)

        }
        binding.btnMode3.setOnClickListener{
            binding.btnMode3.startAnimation(btnAnimation)

        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}