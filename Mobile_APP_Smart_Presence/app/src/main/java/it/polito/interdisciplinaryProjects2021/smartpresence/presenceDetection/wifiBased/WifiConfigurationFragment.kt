package it.polito.interdisciplinaryProjects2021.smartpresence.presenceDetection.wifiBased

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.*
import android.widget.*
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

class WifiConfigurationFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wifi_configuration, container, false)
    }

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
        val uploadConfWifiFab = view.findViewById<FloatingActionButton>(R.id.uploadConfWifiFab)
        if (professionalAccessGranted) {
            uploadConfWifiFab.visibility = View.VISIBLE
        } else {
            uploadConfWifiFab.visibility = View.GONE
        }

        val ssid_input = view.findViewById<TextInputLayout>(R.id.ssid_input)
        val bssid_input = view.findViewById<TextInputLayout>(R.id.bssid_input)
        val address_input = view.findViewById<TextInputLayout>(R.id.address_input)
        val max_occupancy_input = view.findViewById<TextInputLayout>(R.id.max_occupancy_input)
        val update_wifi_info_button = view.findViewById<Button>(R.id.update_wifi_info_button)
        val update_address_button = view.findViewById<Button>(R.id.update_address_button)

        val positioningCheckingStatus = sharedPreferences.getString("positioningCheckingStatus", "false").toBoolean()
        val wifiCheckingStatus = sharedPreferences.getString("wifiCheckingStatus", "false").toBoolean()
        if (positioningCheckingStatus || wifiCheckingStatus) {
            blurBackground(view)
            setHasOptionsMenu(false)
            address_input.isEnabled = false
            max_occupancy_input.isEnabled = false
            ssid_input.isEnabled = false
            bssid_input.isEnabled = false
            update_wifi_info_button.isEnabled = false
            update_address_button.isEnabled = false
            uploadConfWifiFab.isEnabled = false
        } else {
            setHasOptionsMenu(true)
            val blurViewText = view.findViewById<TextView>(R.id.blurViewText)
            blurViewText.visibility = View.GONE
        }

//        view.findViewById<TextInputLayout>(R.id.latitude).isVisible = false
//        view.findViewById<TextInputLayout>(R.id.longitude).isVisible = false
//        val linearLayout = view.findViewById<LinearLayout>(R.id.linearLayout)
//        val layoutParams = LinearLayout.LayoutParams(
//            LinearLayout.LayoutParams.MATCH_PARENT,
//            LinearLayout.LayoutParams.WRAP_CONTENT
//        )
//        layoutParams.setMargins(0, 0, 0, 0)
//        layoutParams.height = 0
//        linearLayout.layoutParams = layoutParams

//        val ssid_required = view.findViewById<TextView>(R.id.ssid_required)
//        val bssid_required = view.findViewById<TextView>(R.id.bssid_required)
//        val address_required = view.findViewById<TextView>(R.id.address_required)
//        val maxOccupancy_required = view.findViewById<TextView>(R.id.maxOccupancy_required)

//        makeLayoutGone(ssid_required)
//        makeLayoutGone(bssid_required)
//        makeLayoutGone(address_required)
//        makeLayoutGone(maxOccupancy_required)

        ssid_input.editText?.doAfterTextChanged {
//            makeLayoutGone(ssid_required)
            ssid_input.error = null
        }
        bssid_input.editText?.doAfterTextChanged {
//            makeLayoutGone(bssid_required)
            bssid_input.error = null
        }
        address_input.editText?.doAfterTextChanged {
//            makeLayoutGone(address_required)
            address_input.error = null
        }
        max_occupancy_input.editText?.doAfterTextChanged {
//            makeLayoutGone(maxOccupancy_required)
            max_occupancy_input.error = null
        }

        val ssid = sharedPreferences.getString("ssid", "nothing")
        val bssid = sharedPreferences.getString("bssid", "nothing")
        val address = sharedPreferences.getString("address", "nothing")
        val maxOccupancy = sharedPreferences.getString("maxOccupancy", "nothing")

        if (ssid != "nothing") {
            ssid_input.editText?.setText(ssid?.replace("_", " "))
        }
        if (bssid != "nothing") {
            bssid_input.editText?.setText(bssid?.replace("_", " "))
        }
        if (address != "nothing") {
            address_input.editText?.setText(address?.replace("_", " "))
        }
        if (maxOccupancy != "nothing") {
            max_occupancy_input.editText?.setText(maxOccupancy?.replace("_", " "))
        }

        update_wifi_info_button.setOnClickListener {
            val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ssidString = wifiInfo.ssid
            val bssidString = wifiInfo.bssid
            val netId = wifiInfo.networkId

            if (netId == -1) {
                Toast.makeText(requireContext(), getString(R.string.no_wifi_connection_message), Toast.LENGTH_LONG).show()
            } else {
                val ssidFinal = if (ssidString.startsWith("\"") && ssidString.endsWith("\"")) {
                    ssidString.substring(1, ssidString.length -1)
                } else {
                    ssidString
                }
                val bssidFinal = if (bssidString.startsWith("\"") && bssidString.endsWith("\"")) {
                    bssidString.substring(1, ssidString.length -1)
                } else {
                    bssidString
                }

                ssid_input.editText?.setText(ssidFinal)
                bssid_input.editText?.setText(bssidFinal)
            }
        }

        update_address_button.setOnClickListener {
            address_input.error = null
            max_occupancy_input.error = null
            bssid_input.error = null
            ssid_input.error = null

            findNavController().navigate(R.id.mapFragment)
        }

        uploadConfWifiFab.setOnClickListener {
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
            val ssid_input = requireView().findViewById<TextInputLayout>(R.id.ssid_input).editText?.text.toString()
            val bssid_input = requireView().findViewById<TextInputLayout>(R.id.bssid_input).editText?.text.toString()
            val address_input = requireView().findViewById<TextInputLayout>(R.id.address_input).editText?.text.toString()
            val max_occupancy_input = requireView().findViewById<TextInputLayout>(R.id.max_occupancy_input).editText?.text.toString()

            if (ssid_input == "" || bssid_input == "" || address_input == "" || max_occupancy_input == "") {
//                    val ssid_required = requireView().findViewById<TextView>(R.id.ssid_required)
//                    val bssid_required = requireView().findViewById<TextView>(R.id.bssid_required)
//                    val address_required = requireView().findViewById<TextView>(R.id.address_required)
//                    val maxOccupancy_required = requireView().findViewById<TextView>(R.id.maxOccupancy_required)

//                    if (ssid_input == "") { getLayoutBack(ssid_required) }
//                    if (bssid_input == "") { getLayoutBack(bssid_required) }
//                    if (address_input == "") { getLayoutBack(address_required) }
//                    if (max_occupancy_input == "") { getLayoutBack(maxOccupancy_required) }

                if (ssid_input == "") { view?.findViewById<TextInputLayout>(R.id.ssid_input)?.error = getString(R.string.fieldRequiredMessage) }
                if (bssid_input == "") { view?.findViewById<TextInputLayout>(R.id.bssid_input)?.error = getString(R.string.fieldRequiredMessage) }
                if (address_input == "") { view?.findViewById<TextInputLayout>(R.id.address_input)?.error = " " }
                if (max_occupancy_input == "") { view?.findViewById<TextInputLayout>(R.id.max_occupancy_input)?.error = getString(R.string.fieldRequiredMessage) }

                Toast.makeText(requireContext(), getString(R.string.configuration_empty_input_message), Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.configuration_confirm_title))
                    .setMessage("SSID: ${ssid_input}\n" +
                            "BSSID: ${bssid_input}\n" +
                            "${getString(R.string.configurationAlertAddressName)}: ${address_input}\n" +
                            "${getString(R.string.configurationAlertMaxName)}: ${max_occupancy_input}\n")
                    .setNeutralButton(getString(R.string.setting_alert_cancel)) { _, _ -> }
                    .setPositiveButton(getString(R.string.configuration_alert_confirm_button)) { _, _ ->
                        Toast.makeText(requireContext(), getString(R.string.configuration_alert_toast), Toast.LENGTH_SHORT).show()
                        with(sharedPreferences.edit()) {
                            putString("ssid", ssid_input.replace(" ", "_"))
                            putString("bssid", bssid_input)
                            putString("address", address_input.replace(" ", "_"))
                            putString("maxOccupancy", max_occupancy_input)
                            putString("ssidConfigurationFinished", "true")
                            putString("bssidConfigurationFinished", "true")
                            putString("addressConfigurationFinished", "true")
                            putString("maxOccupancyConfigurationFinished", "true")
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

                                    val buildingInput = address_input.replace(" ", "_")
                                    val inputBuildingList = hashMapOf("BuildingName" to buildingInput, "sharedCode" to finalCode)
                                    val inputBuildingInfo = hashMapOf(
                                        "Address" to buildingInput,
                                        "Maximum_expected_number" to max_occupancy_input,
                                        "BSSID" to bssid_input,
                                        "SSID" to ssid_input.replace(" ", "_"),
                                        "detectionMethod" to "WIFI"
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