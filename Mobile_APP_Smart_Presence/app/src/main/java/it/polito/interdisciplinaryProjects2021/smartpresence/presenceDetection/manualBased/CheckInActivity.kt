package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.manualBased

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.interdisciplinaryProjects2021.smartpresence.MainActivity
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import org.joda.time.DateTime

class CheckInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_check_in)

        val sharedPreferences: SharedPreferences = getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val addressConfigurationFinished = sharedPreferences.getString("addressConfigurationFinished", "false").toBoolean()
        val maxOccupancyConfigurationFinished = sharedPreferences.getString("maxOccupancyConfigurationFinished", "false").toBoolean()

        if (addressConfigurationFinished && maxOccupancyConfigurationFinished) {
            val db = Firebase.firestore

            val address = sharedPreferences.getString("address", "nothing")
            val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")

            val now = DateTime.now()

            val input = hashMapOf("presence" to "IN", "timestamp" to now.toString())

            db.collection(address!!).document(user!!).set(hashMapOf("UserName" to user), SetOptions.merge())
            db.collection(address)
                .document(user)
                .collection("MANUAL")
                .document(now.toString())
                .set(input, SetOptions.merge())

            val newestAction = hashMapOf("newestAction" to hashMapOf("timestamp" to now.toString(), "presence" to "IN"))
            db.collection("RegisteredUser").document(user).set(newestAction, SetOptions.merge())
            db.collection("BuildingNameList").document(address).set(hashMapOf("BuildingName" to address), SetOptions.merge())

            Toast.makeText(applicationContext, getString(R.string.shortcutCheckInMessage), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(applicationContext, getString(R.string.shortcutNoConfiguration), Toast.LENGTH_LONG).show()

            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("fromCheckInOrOut", "yes")
            startActivity(intent)
        }

        finish()
    }
}