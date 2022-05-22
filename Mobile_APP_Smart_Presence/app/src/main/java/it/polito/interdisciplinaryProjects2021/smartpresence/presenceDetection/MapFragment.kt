package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.util.*

class MapFragment : Fragment() {

    private lateinit var map : MapView
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val PERMISSION_ID = 1010

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val ctx = requireContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        requestPermission()
        getLastLocation()

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().load(activity, PreferenceManager.getDefaultSharedPreferences(activity))
        Configuration.getInstance().userAgentValue = requireContext().packageName
        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.controller.setZoom(18.0)
        val rotationGestureOverlay = RotationGestureOverlay(context, map)
        rotationGestureOverlay.isEnabled
        map.setMultiTouchControls(true)
        map.overlays.add(rotationGestureOverlay)
        val compassOverlay =  CompassOverlay(requireContext(), map)
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)
        val startMarker = Marker(map)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { currentLocation : Location? ->
            val point = currentLocation?.let { GeoPoint(it.latitude, it.longitude) }
            map.controller.setCenter(point)
        }

        map.overlays.add(object: Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt())
                val setPoint = GeoPoint(geoPoint.latitude, geoPoint.longitude)
                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                Log.d("address!!!!!!", "${Geocoder.isPresent()}")

                try {
                    val latitude = setPoint.latitude
                    val longitude = setPoint.longitude
                    val address = geoCoder.getFromLocation(latitude, longitude,1)
                    startMarker.position = setPoint
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    map.overlays.add(startMarker)

//                    val queue = Volley.newRequestQueue(requireContext())
//                    val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=37.4221,-122.0841&key=..."
//                    val stringRequest = StringRequest(Request.Method.GET, url,
//                        { response ->
//                            Log.d("address!!!!!!", "Response: %s".format(response.toString()))
//                        },
//                        {_ ->
//                            //Handle error
//                        })
//                    queue.add(stringRequest)

                    val addressInput: String = if (Geocoder.isPresent()) {
                        address[0].getAddressLine(0).replace(" ", "_")
                    } else {
                        getString(R.string.geocoderNotWorkingMessage)
                    }

                    startMarker.setOnMarkerClickListener { _, _ ->
                        val toast = Toast.makeText(requireContext(), getString(R.string.onMapMarkerClickMessage) + addressInput, Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        toast.show()
                        return@setOnMarkerClickListener true
                    }

                    val buttonSaveLocation = view.findViewById<Button>(R.id.buttonSaveLocation)
                    buttonSaveLocation.setOnClickListener{
                        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putString("address", addressInput)
                            putString("latitude", latitude.toString())
                            putString("longitude", longitude.toString())
                            putString("targetBuildingForPro", addressInput)
                            putString("newAddressSaved", "true")
                            apply()
                        }

                        findNavController().popBackStack()
                        Toast.makeText(requireContext(), getString(R.string.success_location_save), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(view, getString(R.string.map_error_message), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                        .show()
                    Log.d("MapFragmentInternet", "$e")
                    return true
                }
                return true
            }
        })
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getLastLocation(){
        if(checkPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {task->
                    try {
                        val location: Location? = task.result
                        if(location == null){
                            newLocationData()
                        }else{
                            Log.d("Debug:" ,"Your Location:"+ location.longitude)
                        }

                        Toast.makeText(requireContext(), getString(R.string.notInMyPositionMessage), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        findNavController().popBackStack()
                        Toast.makeText(requireContext(),getString(R.string.no_google_service_msg), Toast.LENGTH_SHORT).show()
                    }
                }
            }else{
                Toast.makeText(requireContext(),getString(R.string.turn_on_location_message), Toast.LENGTH_SHORT).show()
            }
        }else{
            requestPermission()
        }
    }

    private fun checkPermission():Boolean{
        if(
            ActivityCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ){
            return true
        }
        return false
    }

    private fun isLocationEnabled():Boolean{
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation: Location = locationResult.lastLocation
            Log.d("Debug:","your last last location: "+ lastLocation.longitude.toString())
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun newLocationData(){
        val locationRequest =  LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == PERMISSION_ID){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("Debug:","You have the Permission")
            }
        }
    }

}