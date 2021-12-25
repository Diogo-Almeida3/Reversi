package pt.isec.amov.reversi.fragments

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import pt.isec.amov.reversi.R
import pt.isec.amov.reversi.databinding.FragmentMenuBinding
import pt.isec.amov.reversi.fragments.GameFragment.Companion.CLIENT_MODE
import pt.isec.amov.reversi.fragments.GameFragment.Companion.GAMEOFF2
import pt.isec.amov.reversi.fragments.GameFragment.Companion.GAMEON2
import pt.isec.amov.reversi.fragments.GameFragment.Companion.GAMEON3
import pt.isec.amov.reversi.fragments.GameFragment.Companion.SERVER_MODE

class MenuFragment : Fragment() {

    private lateinit var btnAnimation: AlphaAnimation
    private lateinit var binding: FragmentMenuBinding
    private lateinit var navDirections : NavDirections
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /*
        *   Dois botoes ele abre aquele alerta
        */


        binding = FragmentMenuBinding.inflate(inflater)

        btnAnimation = AlphaAnimation(1F,0.8F).apply {
            duration = 500
            interpolator = AccelerateInterpolator(0.05F)
        }

        binding.btnMode1.setOnClickListener{
            binding.btnMode1.startAnimation(btnAnimation)
            navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEOFF2,-1)
            findNavController().navigate(navDirections)
        }
        binding.btnMode2.setOnClickListener{
            binding.btnMode2.startAnimation(btnAnimation)
            if(checkConnectivity())
                showAlert(0)
        }
        binding.btnMode3.setOnClickListener{
            binding.btnMode3.startAnimation(btnAnimation)
            if(checkConnectivity())
                showAlert(1)
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

    private fun checkConnectivity() : Boolean{
        val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
        if(!isConnected){
            Toast.makeText(requireContext(),"Check the wifi connection",Toast.LENGTH_LONG).show()
            return false
        }
        return  true
    }

    private fun showAlert(buttonID: Int) {

        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage("Choose the connection type...")
        builder1.setCancelable(false)

        builder1.setNegativeButton("Server Mode") { dialog, id ->
            run {
                dialog.cancel()
                if(buttonID == 0)
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEON2, SERVER_MODE)
                else
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEON3, SERVER_MODE)
                findNavController().navigate(navDirections)
            }
        }

        builder1.setNeutralButton("Cancel"){dialog, id ->
            run{
                dialog.cancel()
            }
        }

        builder1.setPositiveButton("Client Mode"){dialog, id ->
            run {
                dialog.cancel()
                if(buttonID == 0)
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEON2, CLIENT_MODE)
                else
                    navDirections = MenuFragmentDirections.actionMenuFragmentToGameFragment(GAMEON3, CLIENT_MODE)
                findNavController().navigate(navDirections)
            }
        }
        val alert11: AlertDialog = builder1.create()
        alert11.show()

    }
}
