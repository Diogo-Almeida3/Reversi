package pt.isec.amov.reversi.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.fragments.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var navDirections: NavDirections
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        auth = Firebase.auth
        setHeaderData()

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

    private fun setHeaderData(){
        if(auth.currentUser != null){
            val v = db.collection("Users").document(auth.currentUser!!.uid)

            v.addSnapshotListener{ docs, e ->
                if(e != null)
                    return@addSnapshotListener
                if(docs != null && docs.exists()){
                    val headerView = navigationView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.userName).text = docs.getString("username")
                    headerView.findViewById<TextView>(R.id.userEmail).text = docs.getString("email")

                    val uri = File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")
                    if(uri.exists()){
                        headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.userImage).setImageURI(Uri.fromFile(uri))
                    }
                }
            }
        }
    }

    fun signOut() {
        if (auth.currentUser != null) {
            auth.signOut()
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }
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
                    R.id.creditsFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_creditsFragment_to_menuFragment)

                }

            }
            R.id.rules -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_menuFragment_to_rulesFragment)
                    R.id.rulesFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_rulesFragment_self)
                    R.id.profileFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_profileFragment_to_rulesFragment)
                    R.id.creditsFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_creditsFragment_to_rulesFragment)
                }
            }
            R.id.credits->{
                when(findNavController(R.id.fragment_base).currentDestination?.id){
                    R.id.menuFragment-> findNavController(R.id.fragment_base).navigate(R.id.action_menuFragment_to_creditsFragment)
                    R.id.rulesFragment-> findNavController(R.id.fragment_base).navigate(R.id.action_rulesFragment_to_creditsFragment)
                    R.id.profileFragment-> findNavController(R.id.fragment_base).navigate(R.id.action_profileFragment_to_creditsFragment)
                    R.id.creditsFragment-> findNavController(R.id.fragment_base).navigate(R.id.action_creditsFragment_self)
                }
            }

            R.id.gameOff2 -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> {
                        navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GameFragment.GAMEOFF2,-1)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.rulesFragment -> {
                        navDirections = RulesFragmentDirections.actionRulesFragmentToGameFragment(GameFragment.GAMEOFF2,-1)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    R.id.profileFragment -> {
                        navDirections = ProfileFragmentDirections.actionProfileFragmentToGameFragment(GameFragment.GAMEOFF2,-1)
                        findNavController(R.id.fragment_base).navigate(navDirections)
                    }
                    else -> Toast.makeText(this,"Não VAIS",Toast.LENGTH_LONG).show()
                }
            }
            R.id.gameOn2 -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> {
                        showAlert(0)
                    }
                    R.id.rulesFragment -> {
                        showAlert(0)
                    }
                    R.id.profileFragment -> {
                        showAlert(0)
                    }
                    else -> Toast.makeText(this,"Não VAIS",Toast.LENGTH_LONG).show()
                }
            }
            R.id.gameOn3 -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> {
                        showAlert(1)
                    }
                    R.id.rulesFragment -> {
                        showAlert(1)
                    }
                    R.id.profileFragment -> {
                        showAlert(1)
                    }
                    else -> Toast.makeText(this,"Não VAIS",Toast.LENGTH_LONG).show()
                }
            }

            R.id.editProfile -> {
                when (findNavController(R.id.fragment_base).currentDestination?.id) {
                    R.id.menuFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_menuFragment_to_profileFragment)
                    R.id.rulesFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_rulesFragment_to_profileFragment)
                    R.id.profileFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_profileFragment_self)
                    R.id.creditsFragment -> findNavController(R.id.fragment_base).navigate(R.id.action_creditsFragment_to_profileFragment)

                }
            }
            R.id.logout -> {
                signOut()
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


    private fun showAlert(buttonID: Int) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
        builder1.setMessage(resources.getString(R.string.connectionType))
        builder1.setCancelable(false)

        builder1.setNegativeButton(resources.getString(R.string.serverMode)) { dialog, id ->
            run {
                dialog.cancel()
                if(buttonID == 0)
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(
                        GameFragment.GAMEON2,
                        GameFragment.SERVER_MODE
                    )
                else
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(
                        GameFragment.GAMEON3,
                        GameFragment.SERVER_MODE
                    )
                findNavController(R.id.fragment_base).navigate(navDirections)
            }
        }

        builder1.setNeutralButton(resources.getString(R.string.cancel)){dialog, id ->
            run{
                dialog.cancel()
            }
        }

        builder1.setPositiveButton(resources.getString(R.string.clientMode)){dialog, id ->
            run {
                dialog.cancel()
                if(buttonID == 0)
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(
                        GameFragment.GAMEON2,
                        GameFragment.CLIENT_MODE
                    )
                else
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(
                        GameFragment.GAMEON3,
                        GameFragment.CLIENT_MODE
                    )
                findNavController(R.id.fragment_base).navigate(navDirections)
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }
}