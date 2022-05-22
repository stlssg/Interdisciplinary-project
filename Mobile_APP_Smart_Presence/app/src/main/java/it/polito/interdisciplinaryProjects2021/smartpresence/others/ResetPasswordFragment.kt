package it.polito.interdisciplinaryProjects2021.smartpresence.others

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import it.polito.interdisciplinaryProjects2021.smartpresence.utility.MqttClient

class ResetPasswordFragment : Fragment() {

    private val mqttClient: MqttClient by lazy {
        MqttClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mqttClient.connect()

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val currentAccount = sharedPreferences.getString("keyCurrentAccount", "noEmail")

        val oldPassword = view.findViewById<TextInputLayout>(R.id.oldPassword)
        val forgetPasswordBtn = view.findViewById<TextView>(R.id.forgetPasswordBtn)
        val newPassword = view.findViewById<TextInputLayout>(R.id.newPassword)
        val newPasswordRepeat = view.findViewById<TextInputLayout>(R.id.newPasswordRepeat)
        val confirmPasswordButton = view.findViewById<Button>(R.id.confirmPasswordButton)

        confirmPasswordButton.setOnClickListener{
            val db = Firebase.firestore
            val registeredUserCollection = db.collection("RegisteredUser")
            val newPasswordInput = newPassword.editText?.text.toString()
            registeredUserCollection
                .whereEqualTo("email", currentAccount)
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        if (doc.data["password"] != oldPassword.editText?.text.toString()) {
                            Toast.makeText(requireContext(), getString(R.string.passwordNotCorrect), Toast.LENGTH_SHORT).show()
                        } else {
                            when {
                                newPasswordInput != newPasswordRepeat.editText?.text.toString() -> {
                                    Toast.makeText(requireContext(), getString(R.string.reset_p_not_same), Toast.LENGTH_SHORT).show()
                                }
                                newPasswordInput == doc.data["password"] -> {
                                    Toast.makeText(requireContext(), getString(R.string.new_p_used), Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    registeredUserCollection.document(currentAccount!!).set(hashMapOf("password" to newPasswordInput), SetOptions.merge())
                                    findNavController().popBackStack()
                                    Toast.makeText(requireContext(), getString(R.string.successfully_reset_p), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.logInUnsuccess), Toast.LENGTH_SHORT).show()
                }
        }

        forgetPasswordBtn.setOnClickListener{
            mqttClient.publishMessage("POLITO_ICT4SS_IP/smartPresenceApp/forgetPassword", currentAccount.toString())
            Toast.makeText(requireContext(), getString(R.string.email_sent_msg), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        mqttClient.close()
    }

}