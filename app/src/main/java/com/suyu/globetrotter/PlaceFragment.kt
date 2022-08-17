package com.suyu.globetrotter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.suyu.globetrotter.database.globetrotter.Player
import com.suyu.globetrotter.databinding.FragmentPlaceBinding
import com.suyu.globetrotter.viewmodels.PlayerViewModel
import com.suyu.globetrotter.viewmodels.PlayerViewModelFactory
import javax.net.ssl.SSLEngineResult

// Place screen that shows selected location's information
class PlaceFragment : Fragment() {
    private val navigationArgs: PlaceFragmentArgs by navArgs()
    private var _binding : FragmentPlaceBinding? = null
    private val binding get() = _binding!!
    lateinit var player: Player

    private val viewModel: PlayerViewModel by activityViewModels{
        PlayerViewModelFactory(
            (activity?.application as GlobetrotterApplication).database.playerDao()
        )
    }

    private lateinit var myMap: GoogleMap
    private val callback = OnMapReadyCallback { googleMap ->
        myMap = googleMap
        myMap.setMinZoomPreference(15.0f)
        myMap.setMaxZoomPreference(17.0f)
        val marker = viewModel.getLocLatLng()
        googleMap.addMarker(MarkerOptions().position(marker).title("Marker in chosen location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(marker))

        googleMap.uiSettings.isZoomControlsEnabled = true

    }

    companion object {
        const val SEARCH_PREFIX = "https://www.google.com/search?q="
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment: SupportMapFragment = childFragmentManager.findFragmentById(R.id.map)
                as SupportMapFragment
        mapFragment?.getMapAsync(callback)
        val id = navigationArgs.playerId
        viewModel.getInfo(id).observe(this.viewLifecycleOwner) { selectedPlayer ->
            player = selectedPlayer
            bind(player)
        }
        binding.plantFlagHereBtn.setOnClickListener {
            updatePlayerScore(viewModel.getLocLatLng())
        }
        binding.moreInfoBtn.setOnClickListener {
            val queryURL: Uri = Uri.parse(
                "${SEARCH_PREFIX}${viewModel.getLocName()} + ${viewModel.getLocAddress()}")
            val intent = Intent(Intent.ACTION_VIEW, queryURL)
            context?.startActivity(intent)
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

    private fun bind(player: Player){
        binding.apply {
            placeName.text = viewModel.getLocName()
            address.text = viewModel.getLocAddress()
            pointLabel.text ="Point Value: %.2f".format(viewModel.getPointValue(
                player,
                oriLat = viewModel.getLocLatLng().latitude,
                oriLong = viewModel.getLocLatLng().longitude))
        }
    }

    private fun updatePlayerScore(latLng: LatLng) {
        viewModel.updateScore(player, latLng.latitude, latLng.longitude)
        val action = PlaceFragmentDirections.actionPlaceFragmentToPlayerFragment(player.id)
        this.findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
