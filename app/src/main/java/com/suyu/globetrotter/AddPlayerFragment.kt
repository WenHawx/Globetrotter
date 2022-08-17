package com.suyu.globetrotter

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.suyu.globetrotter.database.globetrotter.Player
import com.suyu.globetrotter.databinding.FragmentAddPlayerBinding
import com.suyu.globetrotter.viewmodels.PlayerViewModelFactory
import com.suyu.globetrotter.viewmodels.PlayerViewModel

//Add a player screen
class AddPlayerFragment : Fragment() {
    private val viewModel: PlayerViewModel by activityViewModels{
        PlayerViewModelFactory(
            (activity?.application as GlobetrotterApplication).database.playerDao()
        )
    }
    lateinit var player: Player
    private var _binding: FragmentAddPlayerBinding? = null
    private val binding get() = _binding!!
    private val navigationArgs: PlayerFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun isEntryValid(): Boolean {
        return viewModel.isEntryValid(
            binding.playerName.text.toString()
        )
    }
    private fun addNewPlayer() {
        if(isEntryValid()){
            viewModel.addNewPlayer(
                binding.playerName.text.toString(),
                oriLat = 0.0,
                oriLong = 0.0
            )
            val action = AddPlayerFragmentDirections.actionAddPlayerFragmentToPlayerListFragment()
            findNavController().navigate(action)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.saveAction.setOnClickListener {
            addNewPlayer()
        }

        // Animations for transitioning between fragments
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val inputMethodManager = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        _binding = null
    }

}