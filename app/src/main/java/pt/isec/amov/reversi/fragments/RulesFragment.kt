package pt.isec.amov.reversi.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pt.isec.amov.reversi.databinding.FragmentRulesBinding

class RulesFragment : Fragment() {

    private lateinit var binding : FragmentRulesBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRulesBinding.inflate(inflater)

        return binding.root
    }
}