package it.polito.interdisciplinaryProjects2021.smartpresence.others

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import it.polito.interdisciplinaryProjects2021.smartpresence.MainActivity
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import it.polito.interdisciplinaryProjects2021.smartpresence.utility.AlarmReceiver
import java.util.*

class SettingFragment : Fragment() {

    private var workingInterval: Int = 15
    private var language: String = "English"
    private var signOutCondition: Boolean = false
    private lateinit var accountModeNameForPair: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    @SuppressLint("SimpleDateFormat", "UseSwitchCompatOrMaterialCode", "LongLogTag")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        val currentAccount = sharedPreferences.getString("keyCurrentAccount", "noEmail")
        view.findViewById<TextView>(R.id.setting_current_account).text = currentAccount

        val professionalOrNot = sharedPreferences.getString("professionalOrNot", "false")?.toBoolean()
        val account_description = view.findViewById<TextView>(R.id.account_description)
        account_description.text = if (professionalOrNot == true) {
            accountModeNameForPair = getString(R.string.professionalAccountModeName)
            getString(R.string.professionalAccountSettingDescription)
        } else {
            accountModeNameForPair = getString(R.string.regularAccountModeName)
            getString(R.string.regularAccountSettingDescription)
        }
        account_description.makeLinks(
            Pair(getString(R.string.pair_delete), View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.setting_alert_title))
                    .setMessage(getString(R.string.setting_alert_content))
                    .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                    .setPositiveButton(getString(R.string.setting_alert_confirm)) { _, _ ->
                        val db = Firebase.firestore
                        db.collection("RegisteredUser").document(currentAccount!!).set(hashMapOf("deleteRequirement" to "YES"), SetOptions.merge())

                        with(sharedPreferences.edit()) {
                            putString("ssid", "nothing")
                            putString("bssid", "nothing")
                            putString("address", "nothing")
                            putString("maxOccupancy", "nothing")
                            putString("latitude", "nothing")
                            putString("longitude", "nothing")
                            putString("ssidConfigurationFinished", "false")
                            putString("bssidConfigurationFinished", "false")
                            putString("addressConfigurationFinished", "false")
                            putString("maxOccupancyConfigurationFinished", "false")
                            putString("latitudeConfigurationFinished", "false")
                            putString("longitudeConfigurationFinished", "false")
                            putString("targetBuildingForPro", "nothing")
                            commit()
                        }

                        Toast.makeText(requireContext(), getString(R.string.deleteDataMessage), Toast.LENGTH_LONG).show()
                    }
                    .show()
            }),
            Pair(accountModeNameForPair, View.OnClickListener {
                if (professionalOrNot == true) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.modeSwitchTitle))
                        .setMessage(getString(R.string.modeSwitchMessage_professionalAccount))
                        .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                        .setPositiveButton(getString(R.string.modeSwitchButton)) { _, _ ->
                            with(sharedPreferences.edit()) {
                                putString("professionalOrNot", "false")
                                apply()
                            }

                            restartAppFromSettingFragment()
                        }
                        .show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.modeSwitchTitle))
                        .setMessage(getString(R.string.modeSwitchMessage_regularAccount))
                        .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                        .setPositiveButton(getString(R.string.modeSwitchButton)) { _, _ ->
                            with(sharedPreferences.edit()) {
                                putString("professionalOrNot", "true")
                                apply()
                            }

                            restartAppFromSettingFragment()
                        }
                        .show()
                }
            })
        )

        val workingIntervalList = resources.getStringArray(R.array.working_interval)
        val workingIntervalSpinner = view.findViewById<Spinner>(R.id.workingIntervalSpinner)
        if (workingIntervalSpinner != null) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workingIntervalList)
            workingIntervalSpinner.adapter = adapter
        }
        val workingIntervalSpinnerPosition = sharedPreferences.getString("workingIntervalSpinnerPosition", "0")?.toInt()
        workingIntervalSpinnerPosition?.let { workingIntervalSpinner.setSelection(it) }
        workingIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                Toast.makeText(requireContext(), working_interval_list[position], Toast.LENGTH_SHORT).show()
                workingInterval = workingIntervalList[position].toInt()
//                Log.d("working_interval", working_interval.toString())
                with(sharedPreferences.edit()) {
                    putString( "workingIntervalSpinnerPosition", position.toString())
                    commit()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("892318613323-qr7gmle90ur1qh9g52laui0so1583tak.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val signOutButton = view.findViewById<Button>(R.id.signOut)
        signOutButton.setOnClickListener{
            with(sharedPreferences.edit()) {
                putString("logInOrNot", "false")
                putString("keyCurrentAccount", "noEmail")
                putString("setting_start_time", "07:00")
                putString("setting_stop_time", "23:00")
                putString("workingIntervalSpinnerPosition", "0")
//                putString("languageSpinnerPosition", "0")
                putString("notificationOnOffCondition", "true")
                putString("ssid", "nothing")
                putString("bssid", "nothing")
                putString("address", "nothing")
                putString("maxOccupancy", "nothing")
                putString("latitude", "nothing")
                putString("longitude", "nothing")
                putString("energySavingMode", "off")
                putString("wifiCheckingStatus", "false")
                putString("positioningCheckingStatus", "false")
                putString("ssidConfigurationFinished", "false")
                putString("bssidConfigurationFinished", "false")
                putString("addressConfigurationFinished", "false")
                putString("maxOccupancyConfigurationFinished", "false")
                putString("latitudeConfigurationFinished", "false")
                putString("longitudeConfigurationFinished", "false")
//                putString("customizedLanguage", "false")
                putString("professionalOrNot", "false")
                putString("frequentNotificationOnOffCondition", "false")
                putString("radiusSpinnerPosition", "2")
                putString("detectionMethodSelection", "nothing")
                putString("professionalAccessGranted", "false")
                putString("targetBuildingForPro", "nothing")
                putString("sharedCode", "nothing")
                putString("sensitivityOnOrOff", "on")
                putString("newAddressSaved", "false")
                putString("localNotificationOnOrOff", "true")
                commit()
            }
            signOutCondition = true

            googleSignInClient.signOut()
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.signInFragment)
            (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        val cal = Calendar.getInstance()
        val setting_start_time = view.findViewById<TextView>(R.id.setting_start_time)
        val setting_start_time_input = sharedPreferences.getString("setting_start_time", "07:00")
//        Log.d("setting_start_time_input", setting_start_time_input.toString())
        setting_start_time.text = setting_start_time_input
        val setting_stop_time = view.findViewById<TextView>(R.id.setting_stop_time)
        val setting_stop_time_input = sharedPreferences.getString("setting_stop_time", "23:00")
        setting_stop_time.text = setting_stop_time_input
        setting_start_time.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                setting_start_time.text = SimpleDateFormat("HH:mm").format(cal.time)
            }
            val timePickerDialog = TimePickerDialog(requireContext(), timeSetListener, cal.get(
                Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }
        setting_stop_time.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                setting_stop_time.text = SimpleDateFormat("HH:mm").format(cal.time)
            }
            val timePickerDialog = TimePickerDialog(requireContext(), timeSetListener, cal.get(
                Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }

        val resetPassword = view.findViewById<Button>(R.id.resetPassword)
        resetPassword.setOnClickListener {
            val mAuth = FirebaseAuth.getInstance()
            val user = mAuth.currentUser
            if (user == null) {
                findNavController().navigate(R.id.resetPasswordFragment)
            } else {
                Toast.makeText(requireContext(), getString(R.string.reset_password_gmail_msg), Toast.LENGTH_LONG).show()
            }
        }

        val wifiCheckingStatus = sharedPreferences.getString("wifiCheckingStatus", "false").toBoolean()
        val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false").toBoolean()
        val notificationOnOff = view.findViewById<Switch>(R.id.notificationOnOff)
        val frequentNotificationOnOff = view.findViewById<Switch>(R.id.frequentNotificationOnOff)
        val notificationOnOffCondition = sharedPreferences.getString("notificationOnOffCondition", "true")
        val frequentNotificationOnOffText = view.findViewById<TextView>(R.id.frequentNotificationOnOffText)
        notificationOnOff.isChecked = notificationOnOffCondition.toBoolean()

        if (notificationOnOff.isChecked) {
            frequentNotificationOnOffText.setTextColor(Color.parseColor("#FF000000"))
        } else {
            frequentNotificationOnOffText.setTextColor(Color.parseColor("#66000000"))
        }
        notificationOnOff?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                with(sharedPreferences.edit()) {
                    putString( "notificationOnOffCondition", "true")
                    commit()
                }

                if (wifiCheckingStatus || positioningCheckingStatus) {
                    Firebase.messaging.subscribeToTopic("RemindingManuallyRestartService")
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(requireContext(), getString(R.string.subscribeNotSuccess), Toast.LENGTH_LONG).show()
                            }
                        }
                }

                frequentNotificationOnOff.isClickable = true
                frequentNotificationOnOffText.setTextColor(Color.parseColor("#FF000000"))
                Toast.makeText(requireContext(), getString(R.string.notificationOnMessage), Toast.LENGTH_LONG).show()
            } else {
                with(sharedPreferences.edit()) {
                    putString( "notificationOnOffCondition", "false")
                    commit()
                }
                Toast.makeText(requireContext(), getString(R.string.notificationOffMessage), Toast.LENGTH_LONG).show()
                Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartService")

                frequentNotificationOnOff.isChecked = false
                frequentNotificationOnOff.isClickable = false
                frequentNotificationOnOffText.setTextColor(Color.parseColor("#66000000"))
            }
        }

        val frequentNotificationOnOffCondition = sharedPreferences.getString("frequentNotificationOnOffCondition", "false")
        frequentNotificationOnOff.isChecked = frequentNotificationOnOffCondition.toBoolean()
        frequentNotificationOnOff?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                with(sharedPreferences.edit()) {
                    putString( "frequentNotificationOnOffCondition", "true")
                    commit()
                }

                if (wifiCheckingStatus || positioningCheckingStatus) {
                    Firebase.messaging.subscribeToTopic("RemindingManuallyRestartServiceAdditionalMorning")
                    Firebase.messaging.subscribeToTopic("RemindingManuallyRestartServiceAdditionalEvening")
                }
                Toast.makeText(requireContext(), getString(R.string.frequentNotificationOnMessage), Toast.LENGTH_LONG).show()
            } else {
                with(sharedPreferences.edit()) {
                    putString( "frequentNotificationOnOffCondition", "false")
                    commit()
                }

                Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartServiceAdditionalMorning")
                Firebase.messaging.unsubscribeFromTopic("RemindingManuallyRestartServiceAdditionalEvening")
                Toast.makeText(requireContext(), getString(R.string.frequentNotificationOffMessage), Toast.LENGTH_LONG).show()
            }
        }

        val localNotificationOnOff = view.findViewById<Switch>(R.id.localNotificationOnOff)
        val localNotificationOnOrOff = sharedPreferences.getString("localNotificationOnOrOff", "true")
        localNotificationOnOff.isChecked = localNotificationOnOrOff.toBoolean()
        localNotificationOnOff.setOnCheckedChangeListener{ _, isChecked ->
            if (isChecked) {
                with(sharedPreferences.edit()) {
                    putString( "localNotificationOnOrOff", "true")
                    commit()
                }
            } else {
                val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, 12345, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                alarmManager.cancel(pendingIntent)

                with(sharedPreferences.edit()) {
                    putString( "localNotificationOnOrOff", "false")
                    commit()
                }
            }
        }

        val languageList = resources.getStringArray(R.array.language)
        val languageSpinner = view.findViewById<Spinner>(R.id.languageSpinner)
        if (languageSpinner != null) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languageList)
            languageSpinner.adapter = adapter
        }
        val languageSpinnerPosition = sharedPreferences.getString("languageSpinnerPosition", "0")?.toInt()
        languageSpinnerPosition?.let { languageSpinner.setSelection(it) }
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                language = languageList[position]
                if (languageSpinnerPosition != position) {
                    when (position) {
                        0 -> {
//                            Toast.makeText(requireContext(), "Please restart the app!", Toast.LENGTH_SHORT).show()
                            writeSharedPreferencesAndRestart(position)
                        }
                        1 -> {
//                            Toast.makeText(requireContext(), "Riavvia l\'app!", Toast.LENGTH_SHORT).show()
                            writeSharedPreferencesAndRestart(position)
                        }
                        else -> {
//                            Toast.makeText(requireContext(), "请重启应用！", Toast.LENGTH_SHORT).show()
                            writeSharedPreferencesAndRestart(position)
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        val sensitivityHighOrLow = view.findViewById<Switch>(R.id.sensitivityHighOrLow)
        val sensitivityOnOrOff = sharedPreferences.getString("sensitivityOnOrOff", "on")
        if (sensitivityOnOrOff == "on") {
            sensitivityHighOrLow.isChecked = true
            sensitivityHighOrLow.text = getString(R.string.high_s)
        } else {
            sensitivityHighOrLow.isChecked = false
            sensitivityHighOrLow.text = getString(R.string.low_s)
        }

        sensitivityHighOrLow?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sensitivityHighOrLow.text = getString(R.string.high_s)
                with(sharedPreferences.edit()) {
                    putString( "sensitivityOnOrOff", "on")
                    commit()
                }
            } else {
                sensitivityHighOrLow.text = getString(R.string.low_s)
                with(sharedPreferences.edit()) {
                    putString( "sensitivityOnOrOff", "off")
                    commit()
                }
            }
        }
    }

    private fun writeSharedPreferencesAndRestart(position: Int) {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        with(sharedPreferences.edit()) {
            putString( "languageSpinnerPosition", position.toString())
            putString( "customizedLanguage", "true")
            commit()
        }

        restartAppFromSettingFragment()
    }

    private fun restartAppFromSettingFragment() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("fromSettingChangeLanguage", "yes")
        startActivity(intent)
    }

    private fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
        val spannableString = SpannableString(this.text)
        var startIndexOfLink = -1
        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    textPaint.color = textPaint.linkColor
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    link.second.onClick(view)
                }
            }
            startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)

            spannableString.setSpan(
                clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        this.movementMethod =
            LinkMovementMethod.getInstance()
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    override fun onPause() {
        super.onPause()

        if (!signOutCondition) {
            val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
            val startTime = requireView().findViewById<TextView>(R.id.setting_start_time)?.text
            val stopTime = requireView().findViewById<TextView>(R.id.setting_stop_time)?.text
            with(sharedPreferences.edit()) {
                putString( "setting_start_time", startTime.toString())
                putString( "setting_stop_time", stopTime.toString())
                commit()
            }
        }
    }

//    private fun recreateFragment() {
//        fragmentManager?.beginTransaction()?.detach(this)?.attach(this)?.commit()
//    }

}