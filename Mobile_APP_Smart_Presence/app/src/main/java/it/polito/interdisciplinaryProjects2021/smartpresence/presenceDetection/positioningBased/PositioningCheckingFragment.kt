package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import java.util.concurrent.TimeUnit

class PositioningCheckingFragment : Fragment() {

    lateinit var geofencingClient: GeofencingClient

    companion object{
        private const val GEOFENCE_LOCATION_REQUEST_CODE = 999
        private const val GEOFENCE_KEY = "myGeofenceKeyForSmartPresence"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_positioning_checking, container, false)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        val positioningStart = view.findViewById<Button>(R.id.positioningStart)
        val positioningStop = view.findViewById<Button>(R.id.positioningStop)
        val positioningRestart = view.findViewById<Button>(R.id.positioningRestart)
        val positioningShowInfo = view.findViewById<Button>(R.id.positioningShowInfo)

        val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false")
        if (positioningCheckingStatus.toBoolean()) {
            positioningStart.isVisible = false
            positioningRestart.isVisible = true
            positioningStop.isEnabled = true
        } else {
            positioningStart.isVisible = true
            positioningRestart.isVisible = false
            positioningStop.isEnabled = false
        }

        val sound = MediaActionSound()

        val addressConfigurationFinished = sharedPreferences.getString("addressConfigurationFinished", "false").toBoolean()
        val maxOccupancyConfigurationFinished = sharedPreferences.getString("maxOccupancyConfigurationFinished", "false").toBoolean()
        val latitudeConfigurationFinished = sharedPreferences.getString("latitudeConfigurationFinished", "false").toBoolean()
        val longitudeConfigurationFinished = sharedPreferences.getString("longitudeConfigurationFinished", "false").toBoolean()

        val address = sharedPreferences.getString("address", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")
        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")
        val start_timestamp = sharedPreferences.getString("setting_start_time", "07:00")
        val end_timestamp = sharedPreferences.getString("setting_stop_time", "23:00")
        val working_interval_list = resources.getStringArray(R.array.working_interval)
        val working_interval_list_position = sharedPreferences.getString("workingIntervalSpinnerPosition", "0")
        val working_interval = working_interval_list[working_interval_list_position!!.toInt()].toInt()
        val energySavingMode = sharedPreferences.getString("energySavingMode", "off")
        val radiusSpinnerPosition = sharedPreferences.getString("radiusSpinnerPosition", "2")?.toInt()
        val radius = resources.getStringArray(R.array.radius_list)[radiusSpinnerPosition!!].toFloat()
        val latitudeString = sharedPreferences.getString("latitude", "nothing")
        val longitudeString = sharedPreferences.getString("longitude", "nothing")
        val latitude = if (latitudeString == "nothing") {
            0.toDouble()
        } else {
            latitudeString!!.toDouble()
        }
        val longitude = if (longitudeString == "nothing") {
            0.toDouble()
        } else {
            longitudeString!!.toDouble()
        }

        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        geofencingClient = LocationServices.getGeofencingClient(activity)

        positioningStart.setOnClickListener {
            if (addressConfigurationFinished &&
                maxOccupancyConfigurationFinished &&
                latitudeConfigurationFinished &&
                longitudeConfigurationFinished) {

                positioningStart.isVisible = false
                positioningRestart.isVisible = true
                positioningStop.isEnabled = true
                Toast.makeText(requireContext(), getString(R.string.startServiceMessage), Toast.LENGTH_SHORT).show()
                with(sharedPreferences.edit()) {
                    putString("positioningCheckingStatus", "true")
                    apply()
                }

                val notificationOnOffCondition = sharedPreferences.getString("notificationOnOffCondition", "true").toBoolean()
                if (notificationOnOffCondition) {
                    Firebase.messaging.subscribeToTopic("RemindingManuallyRestartService")
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(requireContext(), getString(R.string.subscribeNotSuccess), Toast.LENGTH_LONG).show()
                            }
                        }

                    val frequentNotificationOnOffCondition = sharedPreferences.getString("frequentNotificationOnOffCondition", "false").toBoolean()
                    if (frequentNotificationOnOffCondition) {
                        Firebase.messaging.subscribeToTopic("RemindingManuallyRestartServiceAdditionalMorning")
                        Firebase.messaging.subscribeToTopic("RemindingManuallyRestartServiceAdditionalEvening")
                    }
                }

                sound.play(MediaActionSound.START_VIDEO_RECORDING)

                val db = Firebase.firestore

                val inputBuildingInfo = hashMapOf(
                    "Address" to address,
                    "Maximum_expected_number" to maxOccupancy,
                    "latitude" to latitude.toString(),
                    "longitude" to longitude.toString()
                )
                val inputUserInfo = hashMapOf("UserName" to user, "startTime" to start_timestamp, "stopTime" to end_timestamp, "working_interval" to working_interval)
                db.collection(address!!).document("Building_Information").set(inputBuildingInfo, SetOptions.merge())
                db.collection(address).document(user!!).set(inputUserInfo, SetOptions.merge())

                if (energySavingMode == "on") {
                    createAndStartGeofence(latitude, longitude, radius, pendingIntent)
                } else if (energySavingMode == "off") {
                    startPeriodicWork(address, user, start_timestamp, end_timestamp, latitude, longitude, radius, working_interval)
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_LONG).show()
            }
        }

        positioningStop.setOnClickListener {
            positioningStart.isVisible = true
            positioningRestart.isVisible = false
            positioningStop.isEnabled = false
            Toast.makeText(requireContext(), getString(R.string.stopServiceMessage), Toast.LENGTH_SHORT).show()
            with(sharedPreferences.edit()) {
                putString("positioningCheckingStatus", "false")
                apply()
            }

            Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartService")
            Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartServiceAdditionalMorning")
            Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartServiceAdditionalEvening")

            sound.play(MediaActionSound.STOP_VIDEO_RECORDING)

            if (energySavingMode == "on") {
                removeAndStopGeofence(pendingIntent)
            } else if (energySavingMode == "off") {
                stopPeriodicWork()
            }
        }

        positioningRestart.setOnClickListener {
            sound.play(MediaActionSound.START_VIDEO_RECORDING)
            Toast.makeText(requireContext(), getString(R.string.restartServiceMessage), Toast.LENGTH_SHORT).show()

            if (energySavingMode == "on") {
                removeAndStopGeofence(pendingIntent)
                createAndStartGeofence(latitude, longitude, radius, pendingIntent)
            } else if (energySavingMode == "off") {
                stopPeriodicWork()
                startPeriodicWork(address, user, start_timestamp, end_timestamp, latitude, longitude, radius, working_interval)
            }
        }

        positioningShowInfo.setOnClickListener {
            val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false").toBoolean()

            if (energySavingMode == "on") {
                if (positioningCheckingStatus) {
                    Snackbar.make(view, getString(R.string.workStatus) + " " + getString(R.string.geofenceWorkingStatus), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                        .show()
                } else {
                    Snackbar.make(view, getString(R.string.workStatus) + " " + getString(R.string.geofenceNotWorkingStatus), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                        .show()
                }
            } else if (energySavingMode == "off") {
                val workInfo = WorkManager.getInstance(requireContext()).getWorkInfosByTag("backgroundPeriodicLocationUpdate")
                val listInfo = workInfo.get()
                if (listInfo == null || listInfo.size == 0) {
                    Snackbar.make(view, getString(R.string.workStatus) + " " + getString(R.string.workStatusString_NOTHING), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                        .show()
                } else {
                    for (info in listInfo) {
                        val workState = info.state.toString()
                        val workStateString = when (workState) {
                            "ENQUEUED" -> {
                                getString(R.string.workStatusString_ENQUEUED)
                            }
                            "RUNNING" -> {
                                getString(R.string.workStatusString_RUNNING)
                            }
                            else -> {
                                getString(R.string.workStatusString_CANCELLED)
                            }
                        }
                        Snackbar.make(view, getString(R.string.workStatus) + " $workStateString", Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                            .show()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAndStartGeofence(lat: Double,
                                       lon: Double,
                                       radius: Float,
                                       pendingIntent: PendingIntent) {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_KEY)
            .setCircularRegion(lat, lon, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofenceRequest = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geofence).build()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                GEOFENCE_LOCATION_REQUEST_CODE)
        } else {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
        }
    }

    private fun removeAndStopGeofence(pendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(pendingIntent)
    }

    private fun startPeriodicWork(address: String?,
                                  user: String?,
                                  start_timestamp: String?,
                                  end_timestamp: String?,
                                  lat: Double,
                                  lon: Double,
                                  radius: Float,
                                  working_interval: Int) {

        val constraints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresDeviceIdle(false)
                .build()
        } else {
            Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .build()
        }

        val myWorker = PeriodicWorkRequestBuilder<MyPeriodicBackgroundPositioningCheckingWork>(working_interval.toLong(), TimeUnit.MINUTES)
            .setInputData(
                workDataOf("collection" to address,
                "document" to user,
                "time_Start" to start_timestamp,
                "time_End" to end_timestamp,
                "latitude" to lat.toString(),
                "longitude" to lon.toString(),
                "radius" to radius.toString())
            )
            .addTag("backgroundPeriodicLocationUpdate")
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "myBackgroundPeriodicLocationUpdate",
            ExistingPeriodicWorkPolicy.REPLACE,
            myWorker
        )

    }

    private fun stopPeriodicWork() {
        WorkManager.getInstance().cancelAllWorkByTag("backgroundPeriodicLocationUpdate")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.configuration_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.configuration_button -> {
                findNavController().navigate(R.id.positioningConfigurationFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}