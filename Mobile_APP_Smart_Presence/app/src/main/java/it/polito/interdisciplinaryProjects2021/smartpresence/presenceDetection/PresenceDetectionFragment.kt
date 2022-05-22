package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased.GeofenceBroadcastReceiver
import it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased.MyPeriodicBackgroundPositioningCheckingWork
import it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.wifiBased.MyPeriodicWifiCheckingWork
import it.polito.interdisciplinaryProjects2021.smartpresence.utility.AlarmReceiver
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PresenceDetectionFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var blurView: BlurView
    private lateinit var menu: Menu

    companion object{
        private const val GEOFENCE_LOCATION_REQUEST_CODE = 999
        private const val GEOFENCE_KEY = "myGeofenceKeyForSmartPresence"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_presence_detection, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "NAME_SHADOWING", "DEPRECATION")
    @SuppressLint("UnspecifiedImmutableFlag", "SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        val sound = MediaActionSound()

        sharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val detectionMethodSelection = sharedPreferences.getString("detectionMethodSelection", "nothing")

        blurView = view.findViewById(R.id.blurView)
        blurBackground(blurView)

        val wifiCheckingGuidanceCard = view.findViewById<MaterialCardView>(R.id.wifiCheckingGuidanceCard)
        val positioningCheckingGuidanceCard = view.findViewById<MaterialCardView>(R.id.positioningCheckingGuidanceCard)
        val manualCheckingGuidanceCard = view.findViewById<MaterialCardView>(R.id.manualCheckingGuidanceCard)

        val wifiCheckingGuidance = view.findViewById<LinearLayout>(R.id.wifiCheckingGuidance)
        val positioningCheckingGuidance = view.findViewById<LinearLayout>(R.id.positioningCheckingGuidance)
        val manualCheckingGuidance = view.findViewById<LinearLayout>(R.id.manualCheckingGuidance)

        val wifiChecking = view.findViewById<MaterialCardView>(R.id.wifiChecking)
        val positioningChecking = view.findViewById<MaterialCardView>(R.id.positioningChecking)
        val manualChecking = view.findViewById<MaterialCardView>(R.id.manualChecking)

        when (detectionMethodSelection) {
            "wifiChecking" -> {
                wifiChecking.isChecked = true
            }
            "positioningChecking" -> {
                positioningChecking.isChecked = true
            }
            "manualChecking" -> {
                manualChecking.isChecked = true
            }
            else -> {
                wifiChecking.isChecked = false
                positioningChecking.isChecked = false
                manualChecking.isChecked = false
            }
        }

        val wifiCheckingGuidanceCancelButton = view.findViewById<ImageButton>(R.id.wifiCheckingGuidanceCancelButton)
        val positioningCheckingGuidanceCancelButton = view.findViewById<ImageButton>(R.id.positioningCheckingGuidanceCancelButton)
        val manualCheckingGuidanceCancelButton = view.findViewById<ImageButton>(R.id.manualCheckingGuidanceCancelButton)

        val specificTimeCheckCard = view.findViewById<MaterialCardView>(R.id.specificTimeCheckCard)

        fun onClickActionsForHiddenViewRemovingOthers() = cancelBlurEffectAndMakeInvisible(
            wifiCheckingGuidanceCard,
            positioningCheckingGuidanceCard,
            manualCheckingGuidanceCard,
            blurView,
            wifiCheckingGuidance,
            positioningCheckingGuidance,
            manualCheckingGuidance,
            specificTimeCheckCard
        )

        blurView.setOnClickListener { onClickActionsForHiddenViewRemovingOthers() }
        wifiCheckingGuidanceCancelButton.setOnClickListener { onClickActionsForHiddenViewRemovingOthers() }
        positioningCheckingGuidanceCancelButton.setOnClickListener { onClickActionsForHiddenViewRemovingOthers() }
        manualCheckingGuidanceCancelButton.setOnClickListener { onClickActionsForHiddenViewRemovingOthers() }

        wifiChecking.setOnClickListener { showMethodGuidance(wifiCheckingGuidanceCard, blurView, wifiCheckingGuidance) }
        wifiChecking.setOnLongClickListener { longClickToSelectMethod(it as MaterialCardView, "wifiChecking") }

        positioningChecking.setOnClickListener { showMethodGuidance(positioningCheckingGuidanceCard, blurView, positioningCheckingGuidance) }
        positioningChecking.setOnLongClickListener { longClickToSelectMethod(it as MaterialCardView, "positioningChecking") }

        manualChecking.setOnClickListener { showMethodGuidance(manualCheckingGuidanceCard, blurView, manualCheckingGuidance) }
        manualChecking.setOnLongClickListener { longClickToSelectMethod(it as MaterialCardView, "manualChecking") }

        val wifiCheckingStatus = sharedPreferences.getString("wifiCheckingStatus", "false")
        val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false")

        val startButton = view.findViewById<Button>(R.id.startButton)
        val restartButton = view.findViewById<Button>(R.id.restartButton)
        val stopButton = view.findViewById<Button>(R.id.stopButton)
        val showStatusButton = view.findViewById<Button>(R.id.showStatusButton)
        val checkInButton = view.findViewById<Button>(R.id.checkInButton)
        val checkOutButton = view.findViewById<Button>(R.id.checkOutButton)

        if (wifiCheckingStatus.toBoolean() or positioningCheckingStatus.toBoolean()) {
            buttonStatusWhenWorking(startButton, restartButton, stopButton)
        } else {
            buttonStatusWhenNoWorking(startButton, restartButton, stopButton)
        }

        val ssidConfigurationFinished = sharedPreferences.getString("ssidConfigurationFinished", "false").toBoolean()
        val bssidConfigurationFinished = sharedPreferences.getString("bssidConfigurationFinished", "false").toBoolean()
        val addressConfigurationFinished = sharedPreferences.getString("addressConfigurationFinished", "false").toBoolean()
        val maxOccupancyConfigurationFinished = sharedPreferences.getString("maxOccupancyConfigurationFinished", "false").toBoolean()
        val latitudeConfigurationFinished = sharedPreferences.getString("latitudeConfigurationFinished", "false").toBoolean()
        val longitudeConfigurationFinished = sharedPreferences.getString("longitudeConfigurationFinished", "false").toBoolean()

        val energySavingMode = sharedPreferences.getString("energySavingMode", "off")
        val address = sharedPreferences.getString("address", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")
        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")
        val startTimestamp = sharedPreferences.getString("setting_start_time", "07:00")
        val endTimestamp = sharedPreferences.getString("setting_stop_time", "23:00")
        val workingIntervalList = resources.getStringArray(R.array.working_interval)
        val workingIntervalListPosition = sharedPreferences.getString("workingIntervalSpinnerPosition", "0")
        val workingInterval = workingIntervalList[workingIntervalListPosition!!.toInt()].toInt()
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

        startButton.setOnClickListener {
            val detectionMethodSelectionTemp = sharedPreferences.getString("detectionMethodSelection", "nothing")
            if (detectionMethodSelectionTemp == "nothing" || detectionMethodSelectionTemp == "manualChecking") {
                Toast.makeText(requireContext(), getString(R.string.no_selection_click_start), Toast.LENGTH_SHORT).show()
            } else {
                if (detectionMethodSelectionTemp == "wifiChecking") {
                    if (ssidConfigurationFinished &&
                        bssidConfigurationFinished &&
                        addressConfigurationFinished &&
                        maxOccupancyConfigurationFinished) {
                        buttonStatusWhenWorking(startButton, restartButton, stopButton)
//                        Toast.makeText(requireContext(), getString(R.string.startServiceMessage), Toast.LENGTH_SHORT).show()
                        with(sharedPreferences.edit()) {
                            putString("wifiCheckingStatus", "true")
                            apply()
                        }

                        manageMyPeriodicWorkForWiFiBasedMethod()

                        startNotificationAndSubscribeTopic()
                        startAlarmNotification()
                        sound.play(MediaActionSound.START_VIDEO_RECORDING)

                        popUpDialogueForStartService(requireContext())
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_SHORT).show()
                    }
                } else if (detectionMethodSelectionTemp == "positioningChecking"){
                    if (addressConfigurationFinished &&
                        maxOccupancyConfigurationFinished &&
                        latitudeConfigurationFinished &&
                        longitudeConfigurationFinished) {
                        buttonStatusWhenWorking(startButton, restartButton, stopButton)
//                        Toast.makeText(requireContext(), getString(R.string.startServiceMessage), Toast.LENGTH_SHORT).show()
                        with(sharedPreferences.edit()) {
                            putString("positioningCheckingStatus", "true")
                            apply()
                        }
                        startNotificationAndSubscribeTopic()
                        startAlarmNotification()
                        sound.play(MediaActionSound.START_VIDEO_RECORDING)

                        val db = Firebase.firestore
                        val sensitivityOnOrOff = sharedPreferences.getString("sensitivityOnOrOff", "on")

                        val inputBuildingInfo = hashMapOf(
                            "Address" to address,
                            "Maximum_expected_number" to maxOccupancy,
                            "latitude" to latitude.toString(),
                            "longitude" to longitude.toString()
                        )
                        val inputUserInfo = hashMapOf(
                            "UserName" to user,
                            "startTime" to startTimestamp,
                            "stopTime" to endTimestamp,
                            "working_interval" to workingInterval,
                            "sensitivity" to sensitivityOnOrOff,
                            "serviceStatus" to "working"
                        )
                        db.collection(address!!).document("Building_Information").set(inputBuildingInfo, SetOptions.merge())
                        db.collection(address).document(user!!).set(inputUserInfo, SetOptions.merge())
                        db.collection("BuildingNameList").document(address).set(hashMapOf("BuildingName" to address), SetOptions.merge())
                        db.collection("RegisteredUser").document(user).set(hashMapOf(
                            "startTime" to startTimestamp,
                            "stopTime" to endTimestamp,
                            "working_interval" to workingInterval,
                            "sensitivity" to sensitivityOnOrOff,
                            "serviceStatus" to "working"
                        ), SetOptions.merge())

                        if (energySavingMode == "on") {
                            createAndStartGeofence(latitude, longitude, radius, pendingIntent)
                        } else if (energySavingMode == "off") {
                            startPeriodicWorkForBackgroundLocationTracking(address, user, startTimestamp, endTimestamp, latitude, longitude, radius, workingInterval)
                        }

                        popUpDialogueForStartService(requireContext())
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        stopButton.setOnClickListener{
            sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
            Toast.makeText(requireContext(), getString(R.string.stopServiceMessage), Toast.LENGTH_SHORT).show()
            buttonStatusWhenNoWorking(startButton, restartButton, stopButton)
            stopNotificationAndUnsubscribeTopic()
            stopAlarmNotification()
            when (sharedPreferences.getString("detectionMethodSelection", "nothing")) {
                "wifiChecking" -> {
                    with(sharedPreferences.edit()) {
                        putString("wifiCheckingStatus", "false")
                        apply()
                    }
                    stopWork()
                }
                "positioningChecking" -> {
                    with(sharedPreferences.edit()) {
                        putString("positioningCheckingStatus", "false")
                        apply()
                    }

                    if (energySavingMode == "on") {
                        removeAndStopGeofence(pendingIntent)
                    } else if (energySavingMode == "off") {
                        stopWork()
                    }
                }
            }

            val db = Firebase.firestore
            db.collection(address!!).document(user!!).set(hashMapOf("serviceStatus" to "stopped"), SetOptions.merge())
            db.collection("RegisteredUser").document(user).set(hashMapOf("serviceStatus" to "stopped"), SetOptions.merge())
        }

        restartButton.setOnClickListener{
            Toast.makeText(requireContext(), getString(R.string.restartServiceMessage), Toast.LENGTH_SHORT).show()
            sound.play(MediaActionSound.START_VIDEO_RECORDING)

            when (sharedPreferences.getString("detectionMethodSelection", "nothing")) {
                "wifiChecking" -> {
                    stopWork()
                    manageMyPeriodicWorkForWiFiBasedMethod()
                }
                "positioningChecking" -> {
                    if (energySavingMode == "on") {
                        removeAndStopGeofence(pendingIntent)
                        createAndStartGeofence(latitude, longitude, radius, pendingIntent)
                    } else if (energySavingMode == "off") {
                        stopWork()
                        startPeriodicWorkForBackgroundLocationTracking(address, user, startTimestamp, endTimestamp, latitude, longitude, radius, workingInterval)
                    }
                }
            }
        }

        showStatusButton.setOnClickListener {
            when (sharedPreferences.getString("detectionMethodSelection", "nothing")) {
                "wifiChecking" -> {
                    val workInfo = WorkManager.getInstance(requireContext()).getWorkInfosByTag("wifiCheckingPeriodicWork")
                    val listInfo = workInfo.get()
                    if (listInfo == null || listInfo.size == 0) {
                        Snackbar.make(view, getString(R.string.workStatus) + " " + getString(R.string.workStatusString_NOTHING), Snackbar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                            .show()
                    } else {
                        for (info in listInfo) {
                            val workStateString = when (info.state.toString()) {
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
                "positioningChecking" -> {
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
                                val workStateString = when (info.state.toString()) {
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
                "manualChecking" -> {
                    Snackbar.make(view, getString(R.string.workStatus) + " " + getString(R.string.show_status_for_manual_method), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                        .show()
                }
                else -> {
                    Snackbar.make(view, getString(R.string.workStatus) + " " + getString(R.string.show_status_for_no_selection), Snackbar.LENGTH_LONG)
                        .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
                        .show()
                }
            }
        }

        checkInButton.setOnClickListener {
            val detectionMethodSelectionTemp = sharedPreferences.getString("detectionMethodSelection", "nothing")
            if (detectionMethodSelectionTemp != "manualChecking") {
                Toast.makeText(requireContext(), getString(R.string.no_selection_manual_click_button), Toast.LENGTH_SHORT).show()
            } else {
                if (addressConfigurationFinished && maxOccupancyConfigurationFinished) {
                    Toast.makeText(requireContext(), getString(R.string.checkInMessage), Toast.LENGTH_SHORT).show()
                    processingCheck(DateTime.now().toString(), "IN")
                } else {
                    Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

        checkOutButton.setOnClickListener {
            val detectionMethodSelectionTemp = sharedPreferences.getString("detectionMethodSelection", "nothing")
            if (detectionMethodSelectionTemp != "manualChecking") {
                Toast.makeText(requireContext(), getString(R.string.no_selection_manual_click_button), Toast.LENGTH_SHORT).show()
            } else {
                if (addressConfigurationFinished && maxOccupancyConfigurationFinished) {
                    Toast.makeText(requireContext(), getString(R.string.checkOutMessage), Toast.LENGTH_SHORT).show()
                    processingCheck(DateTime.now().toString(), "OUT")
                } else {
                    Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val cancelSpecificCheckCardButton = view.findViewById<ImageButton>(R.id.cancelSpecificCheckCardButton)
        val dateForCheck = view.findViewById<TextView>(R.id.dateForCheck)
        val timeForCheck = view.findViewById<TextView>(R.id.timeForCheck)
        val specificCheckButton = view.findViewById<Button>(R.id.specificCheckButton)

        checkInButton.setOnLongClickListener {
            val detectionMethodSelectionTemp = sharedPreferences.getString("detectionMethodSelection", "nothing")
            if (detectionMethodSelectionTemp != "manualChecking") {
                Toast.makeText(requireContext(), getString(R.string.no_selection_manual_click_button), Toast.LENGTH_SHORT).show()
            } else {
                if (addressConfigurationFinished && maxOccupancyConfigurationFinished) {
                    blurView.visibility = View.VISIBLE
                    specificTimeCheckCard.visibility = View.VISIBLE
                    specificCheckButton.text = getString(R.string.checkInButton)
                    assignDateAndTimeToText(dateForCheck, timeForCheck)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_SHORT).show()
                }
            }

            true
        }

        checkOutButton.setOnLongClickListener {
            val detectionMethodSelectionTemp = sharedPreferences.getString("detectionMethodSelection", "nothing")
            if (detectionMethodSelectionTemp != "manualChecking") {
                Toast.makeText(requireContext(), getString(R.string.no_selection_manual_click_button), Toast.LENGTH_SHORT).show()
            } else {
                if (addressConfigurationFinished && maxOccupancyConfigurationFinished) {
                    blurView.visibility = View.VISIBLE
                    specificTimeCheckCard.visibility = View.VISIBLE
                    specificCheckButton.text = getString(R.string.checkOutButton)
                    assignDateAndTimeToText(dateForCheck, timeForCheck)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_SHORT).show()
                }
            }

            true
        }

        cancelSpecificCheckCardButton.setOnClickListener {
            blurView.visibility = View.GONE
            specificTimeCheckCard.visibility = View.GONE
        }

        dateForCheck.setOnClickListener {
            val cal = Calendar.getInstance()
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateForCheck.text = SimpleDateFormat("yyyy-MM-dd").format(cal.time)
            }
            DatePickerDialog(requireContext(), dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        timeForCheck.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                timeForCheck.text = SimpleDateFormat("HH:mm").format(cal.time)
            }
            TimePickerDialog(requireContext(), timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        specificCheckButton.setOnClickListener {
            blurView.visibility = View.GONE
            specificTimeCheckCard.visibility = View.GONE

            val action = if (specificCheckButton.text == getString(R.string.checkInButton)) {
                Toast.makeText(requireContext(), getString(R.string.checkInMessage), Toast.LENGTH_SHORT).show()
                "IN"
            } else {
                Toast.makeText(requireContext(), getString(R.string.checkOutMessage), Toast.LENGTH_SHORT).show()
                "OUT"
            }

            val specificDateTime = "${dateForCheck.text}T${timeForCheck.text}:00.000+01:00"
            processingCheck(specificDateTime, action)
        }

        val verifyCodeBtn = view.findViewById<Button>(R.id.verifyCodeBtn)
        verifyCodeBtn.setOnClickListener {
            val db = Firebase.firestore
            val sharedCodeInput = view.findViewById<TextInputLayout>(R.id.sharedCodeInput).editText?.text.toString()
            db.collection("BuildingNameList")
                .whereEqualTo("sharedCode", sharedCodeInput)
                .get()
                .addOnSuccessListener { documents ->
                    var numDocuments = 0
                    for (document in documents) {
                        numDocuments ++
                        val configBuilding = document.data["BuildingName"].toString()

                        db.collection(configBuilding)
                            .document("Building_Information")
                            .get()
                            .addOnSuccessListener { document ->
                                Log.d("DocumentSnapshot data:", "${document.data}")
                                when {
                                    document.data?.getValue("detectionMethod") == "WIFI" -> {
                                        with(sharedPreferences.edit()) {
                                            putString("ssid", document.data?.getValue("SSID").toString())
                                            putString("bssid", document.data?.getValue("BSSID").toString())
                                            putString("address", document.data?.getValue("Address").toString())
                                            putString("maxOccupancy", document.data?.getValue("Maximum_expected_number").toString())
                                            putString("ssidConfigurationFinished", "true")
                                            putString("bssidConfigurationFinished", "true")
                                            putString("addressConfigurationFinished", "true")
                                            putString("maxOccupancyConfigurationFinished", "true")
                                            putString("detectionMethodSelection", "wifiChecking")
                                            putString("targetBuildingForPro", document.data?.getValue("Address").toString())
                                            apply()
                                        }
                                    }
                                    document.data?.getValue("detectionMethod") == "POSITIONING" -> {
                                        with(sharedPreferences.edit()) {
                                            putString("latitude", document.data?.getValue("latitude").toString())
                                            putString("longitude", document.data?.getValue("longitude").toString())
                                            putString("address", document.data?.getValue("Address").toString())
                                            putString("maxOccupancy", document.data?.getValue("Maximum_expected_number").toString())
                                            putString("addressConfigurationFinished", "true")
                                            putString("maxOccupancyConfigurationFinished", "true")
                                            putString("latitudeConfigurationFinished", "true")
                                            putString("longitudeConfigurationFinished", "true")
                                            putString("energySavingMode", "off")
                                            putString("detectionMethodSelection", "positioningChecking")
                                            putString("targetBuildingForPro", document.data?.getValue("Address").toString())
                                            apply()
                                        }
                                    }
                                    document.data?.getValue("detectionMethod") == "GEOFENCE" -> {
                                        with(sharedPreferences.edit()) {
                                            putString("latitude", document.data?.getValue("latitude").toString())
                                            putString("longitude", document.data?.getValue("longitude").toString())
                                            putString("address", document.data?.getValue("Address").toString())
                                            putString("maxOccupancy", document.data?.getValue("Maximum_expected_number").toString())
                                            putString("addressConfigurationFinished", "true")
                                            putString("maxOccupancyConfigurationFinished", "true")
                                            putString("latitudeConfigurationFinished", "true")
                                            putString("longitudeConfigurationFinished", "true")
                                            putString("energySavingMode", "on")
                                            putString("detectionMethodSelection", "positioningChecking")
                                            putString("targetBuildingForPro", document.data?.getValue("Address").toString())
                                            apply()
                                        }
                                    }
                                    document.data?.getValue("detectionMethod") == "MANUAL" -> {
                                        with(sharedPreferences.edit()) {
                                            putString("address", document.data?.getValue("Address").toString())
                                            putString("maxOccupancy", document.data?.getValue("Maximum_expected_number").toString())
                                            putString("addressConfigurationFinished", "true")
                                            putString("maxOccupancyConfigurationFinished", "true")
                                            putString("detectionMethodSelection", "manualChecking")
                                            putString("targetBuildingForPro", document.data?.getValue("Address").toString())
                                            apply()
                                        }
                                    }
                                }

                                val docRef = user?.let { db.collection("RegisteredUser").document(it) }
                                val input = hashMapOf("targetBuilding" to document.data?.getValue("Address").toString())
                                docRef?.set(input, SetOptions.merge())

                                Toast.makeText(requireContext(), getString(R.string.finish_download_conf_msg), Toast.LENGTH_LONG).show()

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    fragmentManager?.beginTransaction()?.detach(this)?.commitNow()
                                    fragmentManager?.beginTransaction()?.attach(this)?.commitNow()
                                } else {
                                    fragmentManager?.beginTransaction()?.detach(this)?.attach(this)?.commit()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d("get failed with ", "$exception")
                            }
                    }
                    if (numDocuments == 0) {
                        Toast.makeText(requireContext(), getString(R.string.no_match_shared_code_msg), Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.logInUnsuccess), Toast.LENGTH_SHORT).show()
                }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startAlarmNotification() {
        val localNotificationOnOrOff = sharedPreferences.getString("localNotificationOnOrOff", "true").toBoolean()
        if (localNotificationOnOrOff) {
            createNotificationChannel()
            val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 12345, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                setUpCalendarForAlarmNotification(14),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            val frequentNotificationOnOffCondition = sharedPreferences.getString("frequentNotificationOnOffCondition", "false").toBoolean()
            if (frequentNotificationOnOffCondition) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    setUpCalendarForAlarmNotification(7),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    setUpCalendarForAlarmNotification(18),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun setUpCalendarForAlarmNotification(hour: Int): Long {
        val date = Date()
        val calAlarm = Calendar.getInstance()
        val calNow = Calendar.getInstance()
        calNow.time = date
        calAlarm.time = date
        calAlarm.set(Calendar.MINUTE, 5)
        calAlarm.set(Calendar.HOUR_OF_DAY, hour)
        if (calAlarm.before(calNow)) {
            calAlarm.add(Calendar.DATE, 1)
        }

        return calAlarm.timeInMillis
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "thisIsMyChannel"
            val description = "Channel for Alarm Manager"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("myNewChannel", name, importance)
            channel.description = description
            val notificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun stopAlarmNotification() {
        val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 12345, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
    }

    private fun manageMyPeriodicWorkForWiFiBasedMethod() {
        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")
        val address = sharedPreferences.getString("address", "nothing")
        val ssid = sharedPreferences.getString("ssid", "nothing")
        val bssid = sharedPreferences.getString("bssid", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")
        val startTimestamp = sharedPreferences.getString("setting_start_time", "07:00")
        val endTimestamp = sharedPreferences.getString("setting_stop_time", "23:00")
        val workingIntervalList = resources.getStringArray(R.array.working_interval)
        val workingIntervalListPosition = sharedPreferences.getString("workingIntervalSpinnerPosition", "0")
        val workingInterval = workingIntervalList[workingIntervalListPosition!!.toInt()].toInt()

        initializationForWiFiBasedMethod(user, address, ssid, bssid, maxOccupancy, startTimestamp, endTimestamp, workingInterval.toString())
        startMyPeriodicWorkForWiFiBasedMethod(address, user, ssid, bssid, startTimestamp, endTimestamp, workingInterval)
    }

    private fun initializationForWiFiBasedMethod(
        user: String?,
        address: String?,
        ssid: String?,
        bssid: String?,
        maxOccupancy: String?,
        start_timestamp: String?,
        end_timestamp: String?,
        working_interval:String
    ) {
        val db = Firebase.firestore
        val inputBuildingInfo = hashMapOf("Address" to address, "SSID" to ssid, "BSSID" to bssid, "Maximum_expected_number" to maxOccupancy)
        val sensitivityOnOrOff = sharedPreferences.getString("sensitivityOnOrOff", "on")
        val inputUserInfo = hashMapOf(
            "UserName" to user,
            "startTime" to start_timestamp,
            "stopTime" to end_timestamp,
            "working_interval" to working_interval,
            "sensitivity" to sensitivityOnOrOff,
            "serviceStatus" to "working"
        )
        db.collection(address!!).document("Building_Information").set(inputBuildingInfo, SetOptions.merge())
        db.collection(address).document("$user").set(inputUserInfo, SetOptions.merge())
        db.collection("BuildingNameList").document(address).set(hashMapOf("BuildingName" to address), SetOptions.merge())
        db.collection("RegisteredUser").document(user!!).set(hashMapOf(
            "startTime" to start_timestamp,
            "stopTime" to end_timestamp,
            "working_interval" to working_interval,
            "sensitivity" to sensitivityOnOrOff,
            "serviceStatus" to "working"
        ), SetOptions.merge())
    }

    private fun startMyPeriodicWorkForWiFiBasedMethod(
        address: String?,
        user: String?,
        ssid: String?,
        bssid: String?,
        start_timestamp: String?,
        end_timestamp: String?,
        working_interval: Int
    ) {
        val constraints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(false)
                .setRequiresDeviceIdle(false)
                .build()
        } else {
            Constraints.Builder()
                .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(false)
                .build()
        }

        val myRequest = PeriodicWorkRequestBuilder<MyPeriodicWifiCheckingWork>(working_interval.toLong(), TimeUnit.MINUTES)
            .setInputData(
                workDataOf("collection" to address,
                "document" to user,
                "Time_Start" to start_timestamp,
                "Time_End" to end_timestamp,
                "ssid" to ssid,
                "bssid" to bssid)
            )
            .setConstraints(constraints)
            .addTag("wifiCheckingPeriodicWork")
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "myPeriodicWorkForWifiChecking",
            ExistingPeriodicWorkPolicy.KEEP,
            myRequest
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAndStartGeofence(
        lat: Double,
        lon: Double,
        radius: Float,
        pendingIntent: PendingIntent
    ) {
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
                GEOFENCE_LOCATION_REQUEST_CODE
            )
        } else {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)
        }
    }

    private fun removeAndStopGeofence(pendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(pendingIntent)
    }

    private fun startPeriodicWorkForBackgroundLocationTracking(
        address: String?,
        user: String?,
        start_timestamp: String?,
        end_timestamp: String?,
        lat: Double,
        lon: Double,
        radius: Float,
        working_interval: Int
    ) {
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

    @Suppress("DEPRECATION")
    private fun stopWork() {
        when (sharedPreferences.getString("detectionMethodSelection", "nothing")) {
            "wifiChecking" -> {
                WorkManager.getInstance().cancelAllWorkByTag("wifiCheckingPeriodicWork")
            }
            "positioningChecking" -> {
                WorkManager.getInstance().cancelAllWorkByTag("backgroundPeriodicLocationUpdate")
            }
        }
    }

    private fun startNotificationAndSubscribeTopic() {
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
    }

    private fun stopNotificationAndUnsubscribeTopic() {
        Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartService")
        Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartServiceAdditionalMorning")
        Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartServiceAdditionalEvening")
    }

    private fun processingCheck(timestamp: String, action: String) {
        val db = Firebase.firestore

        val address = sharedPreferences.getString("address", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")
        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")

        val inputBuildingInfo = hashMapOf("Address" to address, "Maximum_expected_number" to maxOccupancy)
        val input = hashMapOf("presence" to action, "timestamp" to timestamp)

        db.collection(address!!).document("Building_Information").set(inputBuildingInfo, SetOptions.merge())
        db.collection("BuildingNameList").document(address).set(hashMapOf("BuildingName" to address), SetOptions.merge())
        db.collection(address).document(user!!).set(hashMapOf("UserName" to user), SetOptions.merge())
        db.collection(address)
            .document(user)
            .collection("MANUAL")
            .document(timestamp)
            .set(input, SetOptions.merge())

        val newestAction = hashMapOf("newestAction" to hashMapOf("timestamp" to timestamp, "presence" to action))
        db.collection("RegisteredUser").document(user).set(newestAction, SetOptions.merge())
    }

    private fun buttonStatusWhenWorking(startBtn: Button, restartBtn: Button, stopBtn: Button) {
        startBtn.visibility = View.GONE
        restartBtn.visibility = View.VISIBLE
        stopBtn.isEnabled = true
    }

    private fun buttonStatusWhenNoWorking(startBtn: Button, restartBtn: Button, stopBtn: Button) {
        startBtn.visibility = View.VISIBLE
        restartBtn.visibility = View.GONE
        stopBtn.isEnabled = false
    }

    private fun blurBackground(blurView: BlurView) {
        val decorView = activity?.window?.decorView
        val rootView = decorView?.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = decorView?.background

        blurView.setupWith(rootView!!)
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(RenderScriptBlur(requireContext()))
            .setBlurRadius(10f)
            .setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(true)
    }

    private fun cancelBlurEffectAndMakeInvisible(
        card1: MaterialCardView,
        card2: MaterialCardView,
        card3: MaterialCardView,
        blurView: BlurView,
        linearLayout1: LinearLayout,
        linearLayout2: LinearLayout,
        linearLayout3: LinearLayout,
        card4: MaterialCardView
    ) {
        TransitionManager.beginDelayedTransition(card1, AutoTransition())
        TransitionManager.beginDelayedTransition(card2, AutoTransition())
        TransitionManager.beginDelayedTransition(card3, AutoTransition())
        blurView.visibility = View.GONE
        linearLayout1.visibility = View.GONE
        linearLayout2.visibility = View.GONE
        linearLayout3.visibility = View.GONE
        card4.visibility = View.GONE
        menu.getItem(1).isEnabled = true
        menu.getItem(0).setIcon(R.drawable.ic_baseline_cloud_download_24)
    }

    private fun showMethodGuidance(card: MaterialCardView, blurView: BlurView, guidance: LinearLayout) {
        TransitionManager.beginDelayedTransition(card, AutoTransition())
        blurView.visibility = View.VISIBLE
        guidance.visibility = View.VISIBLE
    }

    @Suppress("SameParameterValue")
    private fun longClickToSelectMethod(card: MaterialCardView, methodString: String): Boolean {

        if (sharedPreferences.getString("wifiCheckingStatus", "false").toBoolean() ||
            sharedPreferences.getString("positioningCheckingStatus", "false").toBoolean()) {
            Toast.makeText(requireContext(), getString(R.string.long_click_card_no_stop_service), Toast.LENGTH_SHORT).show()
        } else {
            val detectionMethodSelectionTemp = sharedPreferences.getString("detectionMethodSelection", "nothing")

            if (card.isChecked) {
                with(sharedPreferences.edit()) {
                    putString( "detectionMethodSelection", "nothing")
                    commit()
                }
                card.isChecked = !card.isChecked
            } else if (detectionMethodSelectionTemp != methodString && detectionMethodSelectionTemp != "nothing") {
                Toast.makeText(requireContext(), getString(R.string.already_selected_method_message), Toast.LENGTH_SHORT).show()
            } else {
                with(sharedPreferences.edit()) {
                    putString( "detectionMethodSelection", methodString)
                    commit()
                }
                card.isChecked = !card.isChecked
            }
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        inflater.inflate(R.menu.configuration_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.configuration_button -> {
                when (sharedPreferences.getString("detectionMethodSelection", "nothing")) {
                    "wifiChecking" -> {
                        findNavController().navigate(R.id.wifiConfigurationFragment)
                    }
                    "positioningChecking" -> {
                        findNavController().navigate(R.id.positioningConfigurationFragment)
                    }
                    "manualChecking" -> {
                        findNavController().navigate(R.id.manualConfigurationFragment)
                    }
                    else -> {
                        Toast.makeText(requireContext(), getString(R.string.no_selection_go_conf_message), Toast.LENGTH_SHORT).show()
                    }
                }

                return true
            }
            R.id.action_download_conf -> {
                val downloadConfLayout = view?.findViewById<LinearLayout>(R.id.downloadConfLayout)
                if (blurView.visibility == View.GONE) {
                    blurView.visibility = View.VISIBLE
                    downloadConfLayout?.visibility = View.VISIBLE
                    menu.getItem(1).isEnabled = false
                    menu.getItem(0).setIcon(R.drawable.ic_baseline_close_24)
                } else {
                    blurView.visibility = View.GONE
                    downloadConfLayout?.visibility = View.GONE
                    menu.getItem(1).isEnabled = true
                    menu.getItem(0).setIcon(R.drawable.ic_baseline_cloud_download_24)
                }

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onResume() {
        super.onResume()

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    private fun assignDateAndTimeToText(date: TextView, time: TextView) {
        val now = DateTime.now()
        val formatDate = DateTimeFormat.forPattern("yyyy-MM-dd")
        val formatTime = DateTimeFormat.forPattern("HH:mm")
        date.text = now.toString(formatDate)
        time.text = now.toString(formatTime)
    }

    private fun popUpDialogueForStartService(context: Context){
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.start_popUp_title))
            .setMessage(getString(R.string.start_popUp_msg))
            .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ ->
                Toast.makeText(context, getString(R.string.startServiceMessage), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

}


//        wifiChecking.setOnClickListener {
//            findNavController().navigate(R.id.wifiCheckingFragment)
//        }
//        positioningChecking.setOnClickListener {
//            findNavController().navigate(R.id.positioningCheckingFragment)
//        }
//        manualChecking.setOnClickListener {
//            findNavController().navigate(R.id.manualCheckingFragment)
//        }