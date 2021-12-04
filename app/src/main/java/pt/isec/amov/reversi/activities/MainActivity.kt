package pt.isec.amov.reversi.activities

import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.fragments.*

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var navDirections: NavDirections
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        /* ToolBar Related */
        toolbar = findViewById(R.id.navToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        drawerLayout = findViewById(R.id.drawerLayout)

        navigationView = findViewById(R.id.nav_view)
        setupDrawerContent(navigationView)

        toggle = setupDrawerToggle()

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


    }

    private fun setupDrawerToggle(): ActionBarDrawerToggle = ActionBarDrawerToggle(
        this,
        drawerLayout,
        R.string.open,
        R.string.close
    )

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
    }

    private fun selectDrawerItem(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.menu -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_menuFragment_self)
                    R.id.rulesFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_rulesFragment_to_menuFragment)
                    R.id.profileFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_profileFragment_to_menuFragment2)
                    R.id.gameFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_gameFragment_to_menuFragment)
                }

            }
            R.id.rules -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_menuActivity_to_rulesActivity)
                    R.id.rulesFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_rulesFragment_self)
                    R.id.profileFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_profileFragment_to_rulesFragment)
                }
            }

            R.id.gameOff2 -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> {
                        navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GameFragment.GAMEOFF2)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.rulesFragment -> {
                        navDirections = RulesFragmentDirections.actionRulesFragmentToGameFragment(GameFragment.GAMEOFF2)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.profileFragment -> {
                        navDirections = ProfileFragmentDirections.actionProfileFragmentToGameFragment(GameFragment.GAMEOFF2)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    else -> Toast.makeText(this,"Não VAIS",Toast.LENGTH_LONG).show()
                }
            }
            R.id.gameOn2 -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> {
                        navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GameFragment.GAMEON2)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.rulesFragment -> {
                        navDirections = RulesFragmentDirections.actionRulesFragmentToGameFragment(GameFragment.GAMEON2)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.profileFragment -> {
                        navDirections = ProfileFragmentDirections.actionProfileFragmentToGameFragment(GameFragment.GAMEON2)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    else -> Toast.makeText(this,"Não VAIS",Toast.LENGTH_LONG).show()
                }
            }
            R.id.gameOn3 -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> {
                        navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GameFragment.GAMEON3)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.rulesFragment -> {
                        navDirections = RulesFragmentDirections.actionRulesFragmentToGameFragment(GameFragment.GAMEON3)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.profileFragment -> {
                        navDirections = ProfileFragmentDirections.actionProfileFragmentToGameFragment(GameFragment.GAMEON3)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    else -> Toast.makeText(this,"Não VAIS",Toast.LENGTH_LONG).show()
                }
            }

            R.id.editProfile -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_menuFragment_to_profileFragment)
                    R.id.rulesFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_rulesFragment_to_profileFragment)
                    R.id.profileFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_profileFragment_self)
                }
            }
            R.id.logout -> {
                finish()
            }
        }
        drawerLayout.closeDrawers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }
}