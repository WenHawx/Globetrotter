package com.suyu.globetrotter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.suyu.globetrotter.viewmodels.PlayerViewModel
import com.suyu.globetrotter.viewmodels.PlayerViewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.suyu.globetrotter.databinding.FragmentPlayerListBinding

// List of players that are saved in database
class PlayerListFragment : Fragment() {

    private val viewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory(
            (activity?.application as GlobetrotterApplication).database.playerDao()
        )
    }
    private var _binding: FragmentPlayerListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayerListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PlayerListAdapter{
            val action = PlayerListFragmentDirections.actionPlayerListFragmentToPlayerFragment(it.id)
            this.findNavController().navigate(action)
        }

        // Access database and list all players that are currently saved
        binding.recyclerView.adapter = adapter
        viewModel.allPlayers.observe(this.viewLifecycleOwner) { items ->
            items.let {
                adapter.submitList(it)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)
        binding.floatingActionButton.setOnClickListener {
            val action = PlayerListFragmentDirections.actionPlayerListFragmentToAddPlayerFragment()
            this.findNavController().navigate(action)
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

}