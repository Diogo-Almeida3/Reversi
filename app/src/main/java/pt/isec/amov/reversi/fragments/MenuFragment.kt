package pt.isec.amov.reversi.fragments

import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.*
import android.view.animation.AccelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.FragmentMenuBinding
import pt.isec.amov.reversi.fragments.GameFragment.Companion.GAMEOFF2
import pt.isec.amov.reversi.fragments.GameFragment.Companion.GAMEON2
import pt.isec.amov.reversi.fragments.GameFragment.Companion.GAMEON3

class MenuFragment : Fragment() {

    private lateinit var btnAnimation: AlphaAnimation
    private lateinit var binding: FragmentMenuBinding
    private lateinit var navDirections : NavDirections
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMenuBinding.inflate(inflater)

        btnAnimation = AlphaAnimation(0.9F,0.5F).apply {
            duration = 1000
            interpolator = AccelerateInterpolator(0.1F)
        }

        binding.btnMode1.setOnClickListener{
            binding.btnMode1.startAnimation(btnAnimation)
            navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEOFF2)
            findNavController().navigate(navDirections)
        }
        binding.btnMode2.setOnClickListener{
            binding.btnMode2.startAnimation(btnAnimation)
            navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEON2)
            findNavController().navigate(navDirections)
        }
        binding.btnMode3.setOnClickListener{
            binding.btnMode3.startAnimation(btnAnimation)
            navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEON3)
            findNavController().navigate(navDirections)
        }
        binding.btnRules.setOnClickListener {
            binding.btnRules.startAnimation(btnAnimation)
            findNavController().navigate(R.id.action_menuActivity_to_rulesActivity)
        }
        binding.btnExit.setOnClickListener{
            binding.btnExit.startAnimation(btnAnimation)
            activity?.finish()
        }
        return binding.root
    }
}
