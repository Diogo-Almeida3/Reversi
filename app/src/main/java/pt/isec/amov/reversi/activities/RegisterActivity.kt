package pt.isec.amov.reversi.activities

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        auth = Firebase.auth

        isLogged()

        binding.btnSignUp.setOnClickListener {
            val email = binding.etRegisterEmail.text.toString().trim()
            val username = binding.etRegisterUsername.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()
            val confirmPassword = binding.etRegisterConfirmPassword.text.toString().trim()

            when {
                email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> showAlertError(resources.getString(R.string.emptyFields))

                password != confirmPassword -> showAlertError(resources.getString(R.string.passwordNoMatch))

                username.length < 3 -> showAlertError(resources.getString(R.string.usernameLetters))

                else -> createUserWithEmail(email, password,username)
            }
        }

        binding.tvLoginRegister.setOnClickListener {
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }
    }

    private fun isLogged(){
        if(auth.currentUser != null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }

    private fun showAlertError(name: String) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
        builder1.setMessage(name)
        builder1.setCancelable(true)
        builder1.setPositiveButton("Ok") { dialog, id ->
            run {
                dialog.cancel()
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }

    fun createUserWithEmail(email: String, password: String, username : String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(this) { result ->
                val data = hashMapOf(
                    "email" to auth.currentUser?.email,
                    "username" to username,
                    "nrgames" to 0
                )

                db.collection("Users").
                document(auth.uid.toString()).set(data)
                startActivity(Intent(this,MainActivity::class.java))
            }
            .addOnFailureListener(this) { e ->
                e.message?.let { showAlertError(it) }
            }
    }
}