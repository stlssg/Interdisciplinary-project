package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.wifiBased

import android.content.Context
import android.net.wifi.WifiManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

class MyPeriodicWifiCheckingWork (context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    override fun doWork(): Result {

        val now = DateTime.now()
        val start = inputData.getString("Time_Start").toString()
        val end = inputData.getString("Time_End").toString()

        if (isTimeInRange(now, start, end)) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val netID = wifiInfo.networkId

            if (netID != -1) {
                val db = Firebase.firestore
                val ssidString = wifiInfo.ssid
                val currentSSID = if (ssidString.startsWith("\"") && ssidString.endsWith("\"")) {
                    ssidString.substring(1, ssidString.length - 1)
                } else {
                    ssidString
                }
                val bssidString = wifiInfo.bssid
                val currentBSSID = if (bssidString.startsWith("\"") && bssidString.endsWith("\"")) {
                    bssidString.substring(1, ssidString.length - 1)
                } else {
                    bssidString
                }

                val ssid = inputData.getString("ssid").toString()
                val bssid = inputData.getString("bssid").toString()
                if (!(currentBSSID != bssid && currentSSID != ssid)) {
                    val collectionPath = inputData.getString("collection").toString()
                    val documentPath = inputData.getString("document").toString()
                    val input = hashMapOf("presence" to "CONNECTED", "timestamp" to now.toString())
                    db.collection(collectionPath)
                        .document(documentPath)
                        .collection("WIFI")
                        .document(now.toString())
                        .set(input, SetOptions.merge())

                    val newestAction = hashMapOf("newestAction" to hashMapOf("timestamp" to now.toString(), "presence" to "CONNECTED"))
                    db.collection("RegisteredUser").document(documentPath).set(newestAction, SetOptions.merge())
                }
            }
        }

        return Result.success()
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