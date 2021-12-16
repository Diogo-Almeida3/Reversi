package pt.isec.amov.reversi.fragments

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.FragmentProfileBinding
import java.io.File

class ProfileFragment : Fragment(){

    private lateinit var binding: FragmentProfileBinding
    private var db = Firebase.firestore
    private lateinit var auth : FirebaseAuth
    private var email :String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater)


        auth = Firebase.auth

        val uri = File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")
        if(uri.exists())
            binding.profilePic.setImageURI(Uri.fromFile(uri))

        setUserData()

        binding.btnEditPicture.setOnClickListener{
            findNavController().navigate(R.id.action_profileFragment_to_cameraFragment)
        }

        binding.btnEditPassword.setOnClickListener {
            forgotPassword(email!!)
        }


        return binding.root
    }

    private fun showAlertError(name: String) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(requireContext())
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
            .addOnFailureListener(requireActivity()) { e ->
                e.message?.let { showAlertError(it) }
            }
    }

    private fun setUserData(){
        if(auth.currentUser != null){
            val v = db.collection("Users").document(auth.currentUser!!.uid)
            v.addSnapshotListener{ docs, e ->
                if(e != null)
                    return@addSnapshotListener
                if(docs != null && docs.exists()){
                    binding.profileUsername.text = docs.getString("username")
                    email = docs.getString("email")
                }
            }
        }
    }

}