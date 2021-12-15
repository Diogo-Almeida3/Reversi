package pt.isec.amov.reversi.activities

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.NonNull
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        isLogged()

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() -> showAlertError("Fields must not be empty")

                else -> signInWithEmail(email, password)
            }
        }

        binding.tvLoginRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }


        binding.tvLoginForgotPass.setOnClickListener {


            var email = binding.etLoginEmail.text.toString().trim()

            when {
                email.isEmpty() -> showAlertError("Fields must not be empty")

                else -> forgotPassword(email)
            }
        }
    }

    private fun isLogged() {
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
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

    private fun forgotPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener {
                showAlertError("An email was sent to $email reset the password.")

            }
            .addOnFailureListener(this) { e ->
                e.message?.let { showAlertError(it) }
            }
        clearFields()
    }

    private fun clearFields(){
        binding.etLoginEmail.text.clear()
        binding.etLoginPassword.text.clear()
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(this) { result ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener(this) { e ->
                e.message?.let { showAlertError(it) }
            }
    }
}