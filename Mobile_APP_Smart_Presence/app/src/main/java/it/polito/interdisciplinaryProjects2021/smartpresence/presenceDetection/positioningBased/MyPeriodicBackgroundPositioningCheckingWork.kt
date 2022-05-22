package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.interdisciplinaryProjects2021.smartpresence.MainActivity
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

@Suppress("DEPRECATION")
class MyPeriodicBackgroundPositioningCheckingWork (context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        private const val UPDATE_INTERVAL_IN_MIL: Long = 1000
        private const val FAST_UPDATE_INTERVAL_IN_MIL: Long = UPDATE_INTERVAL_IN_MIL / 2
        private const val MY_CHANNEL_ID: String = "notification_channel_positioning"
        private const val MY_CHANNEL_NAME: String = "it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased"
        private const val NOTIFICATION_ID = 98765
    }

    private var locationRequest : LocationRequest? = null
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private var locationCallback : LocationCallback? = null
    private var mLocation : Location? = null

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {

        val now = DateTime.now()
        val start = inputData.getString("time_Start").toString()
        val end = inputData.getString("time_End").toString()

        if (isTimeInRange(now, start, end)) {
            val foregroundInfo  = ForegroundInfo(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            setForeground(foregroundInfo)

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)

            locationRequest = LocationRequest()
            locationRequest!!.interval = UPDATE_INTERVAL_IN_MIL
            locationRequest!!.fastestInterval = FAST_UPDATE_INTERVAL_IN_MIL
            locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                }
            }

            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            try {
                fusedLocationProviderClient!!.lastLocation
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            mLocation = task.result
                            if (mLocation == null) {
                                Toast.makeText(applicationContext, "The location service is not working properly, please try it later", Toast.LENGTH_LONG).show()
                            } else {
                                val currentLatitude = mLocation?.latitude!!.toDouble()
                                val currentLongitude = mLocation?.longitude!!.toDouble()
                                val targetLatitude = inputData.getString("latitude").toString().toDouble()
                                val targetLongitude = inputData.getString("longitude").toString().toDouble()
                                val radius = inputData.getString("radius").toString().toFloat()
                                val results: FloatArray = floatArrayOf(0f, 0f, 0f)

                                Location.distanceBetween(currentLatitude, currentLongitude, targetLatitude, targetLongitude, results)
//                                Log.d("Current distance: ", "${results[0]} and radius is $radius")

                                if (results[0] <= radius) {
                                    val db = Firebase.firestore
                                    val collectionPath = inputData.getString("collection").toString()
                                    val documentPath = inputData.getString("document").toString()
                                    val input = hashMapOf("presence" to "CONNECTED", "timestamp" to now.toString())
                                    db.collection(collectionPath)
                                        .document(documentPath)
                                        .collection("POSITIONING")
                                        .document(now.toString())
                                        .set(input, SetOptions.merge())

                                    val newestAction = hashMapOf("newestAction" to hashMapOf("timestamp" to now.toString(), "presence" to "CONNECTED"))
                                    db.collection("RegisteredUser").document(documentPath).set(newestAction, SetOptions.merge())
                                }
                            }
                        }
                        else Log.e("locationChecking", "fail to get location")
                    }
            } catch (ex:SecurityException) {
                Log.e("LocationChecking", "" + ex.message)
            }
        }

        return Result.success()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getNotification(): Notification {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(MY_CHANNEL_ID, MY_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(applicationContext, 0 , intent, PendingIntent.FLAG_ONE_SHOT)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, MY_CHANNEL_ID)
            .setContentTitle("Background Location Update")
            .setContentText("We are comparing your current location and the location of target building to decide presence")
            .setAutoCancel(true)
            .setOngoing(true)
            .setSmallIcon(R.drawable.smart_presence)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        return builder.build()
    }

    private fun isTimeInRange(now: DateTime, start: String, end: String): Boolean {
        val format = DateTimeFormat.forPattern("HH:mm")
        val startTime: LocalTime = format.parseLocalTime(start)
        val endTime: LocalTime = format.parseLocalTime(end)
        val timeZone = DateTimeZone.getDefault()
        val today: LocalDate = LocalDate.now(timeZone)
        val startMoment: DateTime = today.toLocalDateTime(startTime).toDateTime(timeZone)
        val endMoment: DateTime = today.toLocalDateTime(endTime).toDateTime(timeZone)
        return now.isAfter(startMoment) && now.isBefore(endMoment)
    }

}