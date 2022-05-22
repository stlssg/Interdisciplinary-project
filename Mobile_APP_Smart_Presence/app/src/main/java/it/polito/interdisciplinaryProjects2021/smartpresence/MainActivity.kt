package it.polito.interdisciplinaryProjects2021.smartpresence

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.interdisciplinaryProjects2021.smartpresence.databinding.ActivityMainBinding
import it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.manualBased.Shortcuts
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        Log.d("language!!!!!!!!", Locale.getDefault().language)

        val sharedPreferences: SharedPreferences = getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        val firstInstallChecking = sharedPreferences.getString("firstInstallChecking", "true")?.toBoolean()
        if (firstInstallChecking != false) {
            with(sharedPreferences.edit()) {
                putString("firstInstallChecking", "true")
                commit()
            }
        }

        val languageSpinnerPosition = sharedPreferences.getString("languageSpinnerPosition", "0")?.toInt()
        val customizedLanguage = sharedPreferences.getString("customizedLanguage", "false")?.toBoolean()
        if (customizedLanguage == true) {
            when (languageSpinnerPosition) {
                1 -> setLang("it")
                2 -> setLang("zh")
                else -> setLang("en")
            }
        } else {
            when (Locale.getDefault().language) {
                "it" -> {
                    setLang("it")
                    with(sharedPreferences.edit()) {
                        putString("languageSpinnerPosition", "1")
                        commit()
                    }
                }
                "zh" -> {
                    setLang("zh")
                    with(sharedPreferences.edit()) {
                        putString("languageSpinnerPosition", "2")
                        commit()
                    }
                }
                else -> {
                    setLang("en")
                    with(sharedPreferences.edit()) {
                        putString("languageSpinnerPosition", "0")
                        commit()
                    }
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        val professionalOrNot = sharedPreferences.getString("professionalOrNot", "false")?.toBoolean()
        if (professionalOrNot == false) {
            navView.menu.removeItem(R.id.nav_chart)
        }
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_introduction, R.id.nav_presence_detection, R.id.nav_setting, R.id.nav_chart))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= 25) {
            Shortcuts.setUp(applicationContext)
        }

        val currentAccount = sharedPreferences.getString("keyCurrentAccount", "noEmail")
        if (currentAccount != "noEmail") {
            val db = Firebase.firestore
            val docRef = db.collection("RegisteredUser").document(currentAccount.toString())
            docRef.get().addOnSuccessListener { document ->
                if (document.data?.get("needFrequentNotification") == "YES") {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.frequentNotificationRemindTitle))
                        .setMessage(getString(R.string.frequentNotificationRemindMessage))
                        .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                        .setPositiveButton(getString(R.string.frequentNotificationRemindPositiveButton)) { _, _ ->
                            navController.navigate(R.id.nav_setting)
                        }
                        .show()

                    val input = hashMapOf("needFrequentNotification" to "NO")
                    docRef.set(input, SetOptions.merge())
                }
            }
        }

        val checkFromNotification = intent.getStringExtra("fromNotificationToFragmentOrNot")
        if (checkFromNotification != null) {
//            val wifiCheckingStatus = sharedPreferences.getString("wifiCheckingStatus", "false").toBoolean()
//            val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false").toBoolean()
//
//            if (wifiCheckingStatus) {
//                navController.navigate(R.id.wifiCheckingFragment)
//            } else if (positioningCheckingStatus) {
//                navController.navigate(R.id.positioningCheckingFragment)
//            }
            navController.navigate(R.id.nav_presence_detection)
        }

        val fromCheckInOrOut = intent.getStringExtra("fromCheckInOrOut")
        if (fromCheckInOrOut != null) {
            navController.navigate(R.id.manualConfigurationFragment)
        }

        val fromSettingChangeLanguage = intent.getStringExtra("fromSettingChangeLanguage")
        if (fromSettingChangeLanguage == "yes") {
            navController.navigate(R.id.nav_setting)
            intent.putExtra("fromSettingChangeLanguage", "no")
        }

        val checkFromLocalNotification = intent.getStringExtra("fromLocalNotificationToFragmentOrNot")
        if (checkFromLocalNotification != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.from_local_notification_title))
                .setMessage(getString(R.string.from_local_notification_content))
                .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ -> }
                .show()

            navController.navigate(R.id.nav_presence_detection)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setLang(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

//    fun setBottomNavigation(professionalOrNot: Boolean) {
//        if (professionalOrNot) {
//            binding.navView.menu.removeItem(R.id.nav_chart)
//        }
//    }
}