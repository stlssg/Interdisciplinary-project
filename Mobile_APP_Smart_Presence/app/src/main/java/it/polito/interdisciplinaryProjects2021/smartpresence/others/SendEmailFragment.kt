package it.polito.interdisciplinaryProjects2021.smartpresence.others

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.polito.interdisciplinaryProjects2021.smartpresence.R

class SendEmailFragment : Fragment() {

    private val args: SendEmailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_send_email, container, false)
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetEmail = args.targetEmail
        view.findViewById<TextView>(R.id.targetEmailAddressEditText).text = targetEmail

        val sendConfirmButton = view.findViewById<Button>(R.id.sendConfirmButton)
        sendConfirmButton.setOnClickListener {
            val subjectEditText = view.findViewById<TextView>(R.id.subjectEditText).text.toString()
            val bodyEditText = view.findViewById<TextView>(R.id.bodyEditText).text.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                val emails = arrayOf(targetEmail)
                putExtra(Intent.EXTRA_EMAIL, emails)
                putExtra(Intent.EXTRA_SUBJECT, subjectEditText)
                putExtra(Intent.EXTRA_TEXT, bodyEditText)
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }

            findNavController().popBackStack()
        }
    }

}