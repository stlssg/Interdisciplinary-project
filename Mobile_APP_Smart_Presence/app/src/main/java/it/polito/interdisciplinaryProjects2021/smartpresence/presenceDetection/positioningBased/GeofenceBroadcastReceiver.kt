package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.joda.time.DateTime

class GeofenceBroadcastReceiver: BroadcastReceiver() {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressLint("LongLogTag")
    override fun onReceive(context: Context?, intent: Intent?) {
        val db = Firebase.firestore
        val now = DateTime.now()
        val sharedPreferences: SharedPreferences = context!!.getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val address = sharedPreferences.getString("address", "nothing")
        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")
        val dbRef = db.collection(address!!).document(user!!).collection("GEOFENCE").document(now.toString())

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceBroadcastReceiver", errorMessage)
            return
        }

        val geofencingTransition = geofencingEvent.geofenceTransition
        when (geofencingTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                val inputIn = hashMapOf("presence" to "IN", "timestamp" to now.toString())
                dbRef.set(inputIn, SetOptions.merge())

                val newestAction = hashMapOf("newestAction" to hashMapOf("timestamp" to now.toString(), "presence" to "IN"))
                db.collection("RegisteredUser").document(user).set(newestAction, SetOptions.merge())
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                val inputOut = hashMapOf("presence" to "OUT", "timestamp" to now.toString())
                dbRef.set(inputOut, SetOptions.merge())

                val newestAction = hashMapOf("newestAction" to hashMapOf("timestamp" to now.toString(), "presence" to "OUT"))
                db.collection("RegisteredUser").document(user).set(newestAction, SetOptions.merge())
            }
            else -> {
                Log.d("GeofenceBroadcastReceiver", "the wrong action is $geofencingTransition")
            }
        }
    }

}