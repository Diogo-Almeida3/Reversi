package pt.isec.amov.reversi.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
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

        binding.btnEditPicture.setOnClickListener{
            findNavController().navigate(R.id.action_profileFragment_to_cameraFragment)

        }


        return binding.root
    }


}