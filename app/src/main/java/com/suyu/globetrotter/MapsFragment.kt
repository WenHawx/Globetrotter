package com.suyu.globetrotter

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.suyu.globetrotter.databinding.FragmentMapsBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.suyu.globetrotter.database.globetrotter.ChosenLocation
import com.suyu.globetrotter.database.globetrotter.Player
import com.suyu.globetrotter.viewmodels.PlayerViewModel
import com.suyu.globetrotter.viewmodels.PlayerViewModelFactory
import java.util.*

// Map screen to list most likely places for user to choose from
class MapsFragment : Fragment() {
    private val navigationArgs: MapsFragmentArgs by navArgs()

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private val TAG = "MapsFragment"
    private var listPlaces: ListView? = null

    private var locationPermissionGranted: Boolean = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var placesClient: PlacesClient? = null
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private var lastKnownLocation: Location? = null

    // Used for selecting the Current Place
    private val mxEntries = 5
    private val DEFAULT_ZOOM = 15.0F
    private val DEFAULT_LOCATION = LatLng(0.0, 0.0)
    private lateinit var likelyPlaceNames: Array<String?>
    private lateinit var likelyPlaceAddresses: Array<String?>
    private lateinit var likelyPlaceAttributions: Array<String?>
    private lateinit var likelyPlaceLatLngs: Array<LatLng?>
    lateinit var player: Player

    private val viewModel: PlayerViewModel by activityViewModels {
        PlayerViewModelFactory(
            (activity?.application as GlobetrotterApplication).database.playerDao()
        )
    }

    private lateinit var myMap: GoogleMap
    private val callback = OnMapReadyCallback { googleMap ->

        myMap = googleMap
        val marker = LatLng(navigationArgs.playerLat.toDouble(), navigationArgs.playerLng.toDouble())
        googleMap.addMarker(MarkerOptions().position(marker).title("Marker for Player's Origin"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(marker))

        googleMap.uiSettings.isZoomControlsEnabled = true
        locationPermission
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment: SupportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(callback)

        listPlaces = view.findViewById(R.id.listPlaces)

        val apiKey = getString(R.string.MAPS_API_KEY)
        Places.initialize(context, apiKey)
        placesClient = Places.createClient(context)
        fusedLocationProvider =
            LocationServices.getFusedLocationProviderClient(this.requireActivity())

        val id = navigationArgs.playerId
        viewModel.getInfo(id).observe(this.viewLifecycleOwner) { selectedPlayer ->
            player = selectedPlayer
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_locate -> {
                pickCurrentPlace()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
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
            if (ContextCompat.checkSelfPermission(this.requireContext(), ACCESS_FINE_LOCATION)
                == PERMISSION_GRANTED) {
                locationPermissionGranted = true
            } else {
                ActivityCompat.requestPermissions(
                    this.requireActivity(), arrayOf(ACCESS_FINE_LOCATION),
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
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
    }

    private val placeLikelihoods: Unit
        // Get the likely places
        @SuppressLint("MissingPermission") private get() {
            // Use fields to define the data types to return.
            val placeFields: List<Place.Field> = listOf<Field>(
                Field.NAME, Field.ADDRESS,
                Field.LAT_LNG
            )

            // Get the likely places
            val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.builder(placeFields).build()
            val placeResponse: Task<FindCurrentPlaceResponse> = placesClient!!.findCurrentPlace(request)
            placeResponse.addOnCompleteListener(this.requireActivity()) { task ->
                if (task.isSuccessful) {
                    val response: FindCurrentPlaceResponse = task.result
                    // Set the count, handling cases where less than 5 entries are returned.
                    val count: Int = if (response.placeLikelihoods.size < mxEntries) {
                        response.placeLikelihoods.size
                    } else {
                        mxEntries
                    }
                    var i = 0
                    likelyPlaceNames = arrayOfNulls<String>(count)
                    likelyPlaceAddresses = arrayOfNulls<String>(count)
                    likelyPlaceAttributions = arrayOfNulls<String>(count)
                    likelyPlaceLatLngs = arrayOfNulls<LatLng>(count)
                    for (placeLikelihood in response.placeLikelihoods) {
                        val currPlace: Place = placeLikelihood.place
                        likelyPlaceNames[i] = currPlace.name
                        likelyPlaceAddresses[i] = currPlace.address
                        likelyPlaceAttributions[i] =
                            if (currPlace.attributions == null) null else TextUtils.join(
                                " ",
                                currPlace.attributions)
                        likelyPlaceLatLngs[i] = currPlace.latLng
                        val currLatLng =
                            if (likelyPlaceLatLngs[i] == null) "" else likelyPlaceLatLngs[i].toString()
                        Log.i(TAG, String.format("Place " + currPlace!!.name
                                    .toString() + " has likelihood: " + placeLikelihood.likelihood
                                    .toString() + " at " + currLatLng))
                        i++
                        if (i > count - 1) {
                            break
                        }
                    }

                    // Populate the ListView
                    fillPlacesList()
                } else {
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.e(
                            TAG,
                            "Place not found: " + exception.statusCode
                        )
                    }
                }
            }
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
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.result
                            Log.d(TAG, "Latitude: " + lastKnownLocation!!.latitude)
                            Log.d(TAG, "Longitude: " + lastKnownLocation!!.longitude)
                            myMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            Log.e(TAG, "Exception: %s", task.exception)
                            myMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                DEFAULT_LOCATION,
                                DEFAULT_ZOOM.toFloat()))
                        }
                        placeLikelihoods
                    }
                }
            } catch (e: SecurityException) {
                Log.e("Exception: %s", e.message!!)
            }
        }

    private fun pickCurrentPlace() {

        if(locationPermissionGranted){
            deviceLocation
        } else {
            Log.i(TAG, "The user did not grant location permission")

            val myOptions = MarkerOptions()
            myOptions.title(getString(R.string.default_info_title))
            myOptions.position(DEFAULT_LOCATION)
            myOptions.snippet(getString(R.string.default_info_snippet))
            myMap.addMarker(myOptions!!)

            locationPermission
        }
    }

    private val listClickedHandler =
        OnItemClickListener { _, _, position, _ -> // position will give us the index of which place was selected in the array
            viewModel.setLocLatLng(likelyPlaceLatLngs[position]!!)
            viewModel.setLocName(likelyPlaceNames[position]!!)
            viewModel.setAddress(likelyPlaceAddresses[position]!!)
            val action = MapsFragmentDirections.actionMapsFragmentToPlaceFragment(
                playerId = player.id)
            findNavController().navigate(action)

        }

    private fun fillPlacesList() {
        // Set up an ArrayAdapter to convert likely places into TextViews to populate the ListView
        val placesAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this.requireContext(), android.R.layout.simple_list_item_1, likelyPlaceNames)
        listPlaces!!.adapter = placesAdapter
        listPlaces!!.onItemClickListener = listClickedHandler
    }
}