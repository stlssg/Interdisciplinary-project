package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.manualBased

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import org.joda.time.DateTime

class ManualCheckingFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manual_checking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val checkIn = view.findViewById<Button>(R.id.checkIn)
        val checkOut = view.findViewById<Button>(R.id.checkOut)

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val addressConfigurationFinished = sharedPreferences.getString("addressConfigurationFinished", "false").toBoolean()
        val maxOccupancyConfigurationFinished = sharedPreferences.getString("maxOccupancyConfigurationFinished", "false").toBoolean()

        checkIn.setOnClickListener {
            if (checkStatus(addressConfigurationFinished, maxOccupancyConfigurationFinished)) {
                processingCheck("IN")
                Toast.makeText(requireContext(), getString(R.string.checkInMessage), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_LONG).show()
            }
        }

        checkOut.setOnClickListener {
            if (checkStatus(addressConfigurationFinished, maxOccupancyConfigurationFinished)) {
                processingCheck("OUT")
                Toast.makeText(requireContext(), getString(R.string.checkOutMessage), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.configurationNotFinishedMessage), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processingCheck(action: String) {
        val db = Firebase.firestore

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val address = sharedPreferences.getString("address", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")
        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")

        val now = DateTime.now()

        val inputBuildingInfo = hashMapOf("Address" to address, "Maximum_expected_number" to maxOccupancy)
        val input = hashMapOf("presence" to action, "timestamp" to now.toString())

        db.collection(address!!).document("Building_Information").set(inputBuildingInfo, SetOptions.merge())
        db.collection(address).document(user!!).set(hashMapOf("UserName" to user), SetOptions.merge())
        db.collection(address)
            .document(user)
            .collection("MANUAL")
            .document(now.toString())
            .set(input, SetOptions.merge())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.configuration_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.configuration_button -> {
                findNavController().navigate(R.id.manualConfigurationFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkStatus(x: Boolean, y: Boolean): Boolean = x && y
}