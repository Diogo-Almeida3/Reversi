package pt.isec.amov.reversi.fragments

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.FragmentProfileBinding
import java.io.File
import android.widget.LinearLayout
import androidx.core.view.*


class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var email: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater)


        auth = Firebase.auth

        val uri =
            File("/storage/emulated/0/Android/media/pt.isec.amov.reversi/ReversiAmovTP/${auth.currentUser!!.uid}.jpg")
        if (uri.exists())
            binding.profilePic.setImageURI(Uri.fromFile(uri))

        setUserData()

        binding.btnEditPicture.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_cameraFragment)
        }

        binding.btnEditPassword.setOnClickListener {
            forgotPassword(email!!)
        }


        refreshTopScores()

        return binding.root
    }

    private fun refreshTopScores() {
        db.collection("Users")
            .document(auth.currentUser!!.uid)
            .collection("TopScores").get().addOnSuccessListener { data ->
                if(data.size() < 1)
                    return@addOnSuccessListener // There are no top score


                binding.layoutTopScores?.visibility = View.VISIBLE
                binding.tvTopScores?.visibility = View.VISIBLE
                val tb = binding.tableTopScores

                data.forEachIndexed { _, element ->
                    val tr = LinearLayout(context)
                    tr.setPadding(5)



                    val param = TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT,
                    )
                    param.weight = 2.35F
                    val param1 = TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT,

                    )
                    param1.weight = 1F
                    val param2 = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    )
                    param2.weight = 2F

                    val c1 = TextView(context)
                    val gamemode = element.getLong("gamemode")?.plus(1L)
                    c1.text = gamemode.toString()
                    c1.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    c1.layoutParams = param




                    val c2 = TextView(context)
                    var users = element.getString("user") + " vs " + element.getString("opponent")
                    if(!element.getString("opponent2").equals(""))
                        users += " vs " + element.getString("opponent2")
                    c2.text = users
                    c2.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    c2.layoutParams = param1




                    val c3 = TextView(context)
                    var scores = element.getLong("myScore").toString() + " - " + element.getLong("opponentScore")
                    if(element.getLong("opponent2Score") != -1L)
                        scores += " - " + element.getLong("opponent2Score").toString()
                    c3.text = scores
                    c3.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    c3.layoutParams = param2

                    tr.addView(c1)
                    tr.addView(c2)
                    tr.addView(c3)
                    tb.addView(tr)
                }
        }
    }

    private fun showAlertError(name: String) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder1.setMessage(name)
        builder1.setCancelable(true)
        builder1.setPositiveButton(resources.getString(R.string.ok)) { dialog, id ->
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
                showAlertError(resources.getString(R.string.emailSent) +" $email " + resources.getString(R.string.resetPassword))

            }
            .addOnFailureListener(requireActivity()) { e ->
                e.message?.let { showAlertError(it) }
            }
    }

    private fun setUserData() {
        if (auth.currentUser != null) {
            val v = db.collection("Users").document(auth.currentUser!!.uid)
            v.addSnapshotListener { docs, e ->
                if (e != null)
                    return@addSnapshotListener
                if (docs != null && docs.exists()) {
                    binding.profileUsername.text = docs.getString("username")
                    email = docs.getString("email")
                }
            }
        }
    }

}