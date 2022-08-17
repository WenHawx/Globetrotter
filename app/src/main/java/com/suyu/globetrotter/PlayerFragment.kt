package com.suyu.globetrotter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.suyu.globetrotter.database.globetrotter.Player
import com.suyu.globetrotter.databinding.FragmentPlayerBinding
import com.suyu.globetrotter.viewmodels.PlayerViewModel
import com.suyu.globetrotter.viewmodels.PlayerViewModelFactory

// Player screen that shows current stats
class PlayerFragment : Fragment() {
    private val navigationArgs: PlayerFragmentArgs by navArgs()
    private var _binding : FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    lateinit var player: Player
    private var locationPermissionGranted: Boolean = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private var lastKnownLocation: Location? = null
    private val DEFAULT_LOCATION = LatLng(0.0, 0.0)
    private val TAG = "PlayerFragment"

    private val viewModel: PlayerViewModel by activityViewModels{
        PlayerViewModelFactory(
            (activity?.application as GlobetrotterApplication).database.playerDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Animations for transitioning between fragments
        val options = navOptions {
            anim{
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        fusedLocationProvider =
            LocationServices.getFusedLocationProviderClient(this.requireActivity())

        val id = navigationArgs.playerId
        viewModel.getInfo(id).observe(this.viewLifecycleOwner) { selectedPlayer ->
            player = selectedPlayer
            bind(player)
        }


        binding.chooseLocationBtn.setOnClickListener{
            val action = PlayerFragmentDirections.actionPlayerFragmentToMapsFragment(
                player.id,
                player.oriLat.toFloat(),
                player.oriLong.toFloat())
            this.findNavController().navigate(action)
        }

        binding.useCurrentLocationBtn.setOnClickListener {
            updateOrigin()
        }
    }

    private fun bind(player: Player){
        binding.apply {
            nameLabel.text = player.playerName
            score.text = "%.2f".format(player.currentScore)
            originLatLongLabel.text = "${player.oriLat}, ${player.oriLong}"
            deleteBtn.setOnClickListener { showConfirmationDialog() }
        }
    }

    private fun updateOrigin() {
        locationPermission
        deviceLocation
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage(getString(R.string.delete_question))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.no)) { _, _ -> }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deletePlayer()
            }
            .show()
    }
    private fun deletePlayer() {
        viewModel.deletePlayer(player)
        findNavController().navigateUp()
    }

    private val deviceLocation: Unit
        @SuppressLint("MissingPermission") private get() {
            /*
                * Get the best and most recent location of the device, which may be null in rare
                * cases when a location is not available.
                */
            try {
                if (locationPermissionGranted) {
                    val locationResult: Task<Location> = fusedLocationProvider!!.lastLocation
                    locationResult.addOnCompleteListener(this.requireActivity()) { task ->
                        if (task.isSuccessful) {
                            lastKnownLocation = task.result
                            Log.d(TAG, "Latitude: " + lastKnownLocation!!.latitude)
                            Log.d(TAG, "Longitude: " + lastKnownLocation!!.longitude)
                            viewModel.updateOriLatLng(player, lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            Log.e(TAG, "Exception: %s", task.exception)
                            viewModel.updateOriLatLng(player, DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude)
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("Exception: %s", e.message!!)
            }
        }

    private val locationPermission: Unit
        private get() {
            /*
                * Request location permission, so that we can get the location of the
                * device. The result of the permission request is handled by a callback,
                * onRequestPermissionsResult.
                */
            locationPermissionGranted = false
            if (ContextCompat.checkSelfPermission(this.requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                locationPermissionGranted = true
            } else {
                ActivityCompat.requestPermissions(
                    this.requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}