package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.positioningBased

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import java.util.*

class PositioningConfigurationFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_positioning_configuration, container, false)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        if (sharedPreferences.getString("newAddressSaved", "false").toBoolean()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.address_remind_title))
                .setMessage(getString(R.string.address_remind_msg))
                .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ ->
                    with(sharedPreferences.edit()) {
                        putString("newAddressSaved", "false")
                        apply()
                    }
                }
                .show()
        }

        val professionalAccessGranted = sharedPreferences.getString("professionalAccessGranted", "false").toBoolean()
        val uploadConfPositioningFab = view.findViewById<FloatingActionButton>(R.id.uploadConfPositioningFab)
        if (professionalAccessGranted) {
            uploadConfPositioningFab.visibility = View.VISIBLE
        } else {
            uploadConfPositioningFab.visibility = View.GONE
        }

        val address_input = view.findViewById<TextInputLayout>(R.id.address_input)
        val max_occupancy_input = view.findViewById<TextInputLayout>(R.id.max_occupancy_input)
        val latitude_input = view.findViewById<TextInputLayout>(R.id.latitude_input)
        val longitude_input = view.findViewById<TextInputLayout>(R.id.longitude_input)
        val energySavingModeOnOff = view.findViewById<Switch>(R.id.energySavingModeOnOff)
        val update_address_button = view.findViewById<Button>(R.id.update_address_button)
        val radiusSpinner = view.findViewById<Spinner>(R.id.radiusSpinner)

        val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false").toBoolean()
        val wifiCheckingStatus = sharedPreferences.getString("wifiCheckingStatus", "false").toBoolean()
        if (positioningCheckingStatus || wifiCheckingStatus) {
            blurBackground(view)
            setHasOptionsMenu(false)
            address_input.isEnabled = false
            max_occupancy_input.isEnabled = false
            latitude_input.isEnabled = false
            longitude_input.isEnabled = false
            energySavingModeOnOff.isEnabled = false
            update_address_button.isEnabled = false
            radiusSpinner.isEnabled = false
        } else {
            setHasOptionsMenu(true)
            val blurViewText = view.findViewById<TextView>(R.id.blurViewText)
            blurViewText.visibility = View.GONE
        }

//        val address_required = view.findViewById<TextView>(R.id.address_required)
//        val location_required = view.findViewById<TextView>(R.id.location_required)
//        val maxOccupancy_required = view.findViewById<TextView>(R.id.maxOccupancy_required)
//
//        makeLayoutGone(address_required)
//        makeLayoutGone(location_required)
//        makeLayoutGone(maxOccupancy_required)

        address_input.editText?.doAfterTextChanged {
            address_input.error = null
//            makeLayoutGone(address_required)
        }
        latitude_input.editText?.doAfterTextChanged {
            latitude_input.error = null
//            makeLayoutGone(location_required)
        }
        longitude_input.editText?.doAfterTextChanged {
            longitude_input.error = null
        }
        max_occupancy_input.editText?.doAfterTextChanged {
            max_occupancy_input.error = null
//            makeLayoutGone(maxOccupancy_required)
        }

        val address = sharedPreferences.getString("address", "nothing")
        val latitude = sharedPreferences.getString("latitude", "nothing")
        val longitude = sharedPreferences.getString("longitude", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")
        val energySavingMode = sharedPreferences.getString("energySavingMode", "off")

        if (latitude != "nothing") {
            latitude_input.editText?.setText(latitude?.replace("_", " "))
        }
        if (longitude != "nothing") {
            longitude_input.editText?.setText(longitude?.replace("_", " "))
        }
        if (address != "nothing") {
            address_input.editText?.setText(address?.replace("_", " "))
        }
        if (maxOccupancy != "nothing") {
            max_occupancy_input.editText?.setText(maxOccupancy?.replace("_", " "))
        }

        energySavingModeOnOff.isChecked = (energySavingMode != "off")

        energySavingModeOnOff?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.energySavingModeAlertTitle) + ": ON")
                    .setMessage(getString(R.string.energySavingModeOnMessage))
                    .setNegativeButton(getString(R.string.energySavingModeAlertNoButton)) { _, _ ->
                        energySavingModeOnOff.isChecked = false
                    }
                    .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ ->
                        with(sharedPreferences.edit()) {
                            putString( "energySavingMode", "on")
                            apply()
                        }
                    }
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.energySavingModeAlertTitle) + ": OFF")
                    .setMessage(getString(R.string.energySavingModeOffMessage))
                    .setNegativeButton(getString(R.string.energySavingModeAlertNoButton)) { _, _ ->
                        energySavingModeOnOff.isChecked = true
                    }
                    .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ ->
                        with(sharedPreferences.edit()) {
                            putString( "energySavingMode", "off")
                            apply()
                        }
                    }
                    .show()
            }
        }

        update_address_button.setOnClickListener {
            address_input.error = null
            latitude_input.error = null
            longitude_input.error = null
            max_occupancy_input.error = null

            findNavController().navigate(R.id.mapFragment)
        }

        val radius_list = resources.getStringArray(R.array.radius_list)
        if (radiusSpinner != null) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, radius_list)
            radiusSpinner.adapter = adapter
        }
        val radiusSpinnerPosition = sharedPreferences.getString("radiusSpinnerPosition", "2")?.toInt()
        radiusSpinnerPosition?.let { radiusSpinner.setSelection(it) }
        radiusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                with(sharedPreferences.edit()) {
                    putString( "radiusSpinnerPosition", position.toString())
                    commit()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        uploadConfPositioningFab.setOnClickListener {
            dealWithSavingOrUploadingConfiguration("uploading")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_button -> {
                dealWithSavingOrUploadingConfiguration("saving")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun dealWithSavingOrUploadingConfiguration(action: String) {
        val sharedCode = sharedPreferences.getString("sharedCode", "nothing")
        if (sharedCode != "nothing" && action == "uploading") {
            Toast.makeText(requireContext(), getString(R.string.already_upload_msg) + " " + sharedCode, Toast.LENGTH_SHORT).show()
        } else {
            val address_input = requireView().findViewById<TextInputLayout>(R.id.address_input).editText?.text.toString()
            val latitude_input = requireView().findViewById<TextInputLayout>(R.id.latitude_input).editText?.text.toString()
            val longitude_input = requireView().findViewById<TextInputLayout>(R.id.longitude_input).editText?.text.toString()
            val max_occupancy_input = requireView().findViewById<TextInputLayout>(R.id.max_occupancy_input).editText?.text.toString()

            if (latitude_input == "" || longitude_input == "" || address_input == "" || max_occupancy_input == "") {
//                    val address_required = requireView().findViewById<TextView>(R.id.address_required)
//                    val location_required = requireView().findViewById<TextView>(R.id.location_required)
//                    val maxOccupancy_required = requireView().findViewById<TextView>(R.id.maxOccupancy_required)

//                    if (address_input == "") { getLayoutBack(address_required) }
//                    if (latitude_input == "" || longitude_input == "") { getLayoutBack(location_required) }
//                    if (max_occupancy_input == "") { getLayoutBack(maxOccupancy_required) }

                if (address_input == "") { view?.findViewById<TextInputLayout>(R.id.address_input)?.error = " " }
                if (latitude_input == "") { view?.findViewById<TextInputLayout>(R.id.latitude_input)?.error = getString(R.string.fieldRequiredMessage) }
                if (longitude_input == "") { view?.findViewById<TextInputLayout>(R.id.longitude_input)?.error = getString(R.string.fieldRequiredMessage) }
                if (max_occupancy_input == "") { view?.findViewById<TextInputLayout>(R.id.max_occupancy_input)?.error = getString(R.string.fieldRequiredMessage) }

                Toast.makeText(requireContext(), getString(R.string.configuration_empty_input_message), Toast.LENGTH_LONG).show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.configuration_confirm_title))
                    .setMessage("${getString(R.string.configurationAlertAddressName)}: ${address_input}\n" +
                            "${getString(R.string.configurationAlertLatName)}: ${latitude_input}\n" +
                            "${getString(R.string.configurationAlertLonName)}: ${longitude_input}\n" +
                            "${getString(R.string.configurationAlertMaxName)}: ${max_occupancy_input}\n")
                    .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                    .setPositiveButton(getString(R.string.configuration_alert_confirm_button)) { _, _ ->
                        Toast.makeText(requireContext(), getString(R.string.configuration_alert_toast), Toast.LENGTH_LONG).show()
                        with(sharedPreferences.edit()) {
                            putString("latitude", latitude_input)
                            putString("longitude", longitude_input)
                            putString("address", address_input.replace(" ", "_"))
                            putString("maxOccupancy", max_occupancy_input)
                            putString("addressConfigurationFinished", "true")
                            putString("maxOccupancyConfigurationFinished", "true")
                            putString("latitudeConfigurationFinished", "true")
                            putString("longitudeConfigurationFinished", "true")
                            putString("targetBuildingForPro", address_input.replace(" ", "_"))
                            apply()
                        }

                        val db = Firebase.firestore
                        val user = sharedPreferences.getString("keyCurrentAccount", "noEmail")
                        val docRef = user?.let { db.collection("RegisteredUser").document(it) }
                        val input = hashMapOf("targetBuilding" to address_input.replace(" ", "_"))
                        docRef?.set(input, SetOptions.merge())

                        if (action == "saving") {
                            findNavController().popBackStack()
                        } else if (action == "uploading") {
                            val allowedCharacters = "0123456789QWERTYUIOPASDFGHJKLZXCVBNM"
                            val random = Random()
                            val sb = StringBuilder(6)
                            for (i in 0 until 6)
                                sb.append(allowedCharacters[random.nextInt(allowedCharacters.length)])
                            val finalCode = sb.toString()
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.upload_alert_title))
                                .setMessage(getString(R.string.upload_alert_content) + " " + finalCode)
                                .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                                .setPositiveButton(getString(R.string.upload_alert_btn)) { _, _ ->
                                    Toast.makeText(requireContext(), getString(R.string.upload_success_msg), Toast.LENGTH_SHORT).show()

                                    with(sharedPreferences.edit()) {
                                        putString("sharedCode", finalCode)
                                        apply()
                                    }

                                    val positioningMode = if (sharedPreferences.getString("energySavingMode", "off") == "on") {
                                        "GEOFENCE"
                                    } else {
                                        "POSITIONING"
                                    }
                                    val buildingInput = address_input.replace(" ", "_")
                                    val inputBuildingList = hashMapOf("BuildingName" to buildingInput, "sharedCode" to finalCode)
                                    val inputBuildingInfo = hashMapOf(
                                        "Address" to buildingInput,
                                        "Maximum_expected_number" to max_occupancy_input,
                                        "latitude" to latitude_input,
                                        "longitude" to longitude_input,
                                        "detectionMethod" to positioningMode
                                    )
                                    db.collection("BuildingNameList").document(buildingInput).set(inputBuildingList, SetOptions.merge())
                                    db.collection(buildingInput).document("Building_Information").set(inputBuildingInfo, SetOptions.merge())

                                    findNavController().popBackStack()
                                }
                                .show()
                        }
                    }
                    .show()
            }
        }
    }

//    private fun makeLayoutGone(view: TextView) {
//        view.isVisible = false
//        val para_layout = view.layoutParams
//        para_layout.height = 0
//        view.layoutParams = para_layout
//    }
//
//    private fun getLayoutBack(view: TextView) {
//        view.isVisible = true
//        val para_layout = view.layoutParams
//        para_layout.height = ViewGroup.LayoutParams.WRAP_CONTENT
//        view.layoutParams = para_layout
//    }

    private fun blurBackground(view: View) {
        val blurView = view.findViewById<BlurView>(R.id.blurView)
        val decorView = activity?.window?.decorView
        val rootView = decorView?.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = decorView?.background

        blurView.setupWith(rootView!!)
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(RenderScriptBlur(requireContext()))
            .setBlurRadius(3f)
            .setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(true)
    }

}