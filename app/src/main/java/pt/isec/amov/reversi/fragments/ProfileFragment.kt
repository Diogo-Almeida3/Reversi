package pt.isec.amov.reversi.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import pt.isec.amov.reversi.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(){

    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater)

        return binding.root
    }
}