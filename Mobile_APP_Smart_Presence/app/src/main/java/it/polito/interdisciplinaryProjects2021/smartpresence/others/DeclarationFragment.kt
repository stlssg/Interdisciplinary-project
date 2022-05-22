package it.polito.interdisciplinaryProjects2021.smartpresence.others

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.polito.interdisciplinaryProjects2021.smartpresence.MainActivity
import it.polito.interdisciplinaryProjects2021.smartpresence.R

class DeclarationFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Leave it like this to disable going back
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_declaration, container, false)
    }

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        val checkBox = view.findViewById<CheckBox>(R.id.checkBox)
        val checkBox2 = view.findViewById<CheckBox>(R.id.checkBox2)

        val finishSignIn = view.findViewById<Button>(R.id.finishSignIn)
        finishSignIn.setOnClickListener{
            if (checkBox.isChecked) {
                if (checkBox2.isChecked) {
                    with(sharedPreferences.edit()) {
                        putString("professionalOrNot", "true")
                        commit()
                    }
                } else {
                    with(sharedPreferences.edit()) {
                        putString("professionalOrNot", "false")
                        commit()
                    }
                }
//                findNavController().navigate(R.id.nav_introduction)
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
            } else {
                checkBox.blink(3)
                Toast.makeText(requireContext(), getString(R.string.plzSign), Toast.LENGTH_SHORT).show()
            }
        }

        val packageName = (activity as AppCompatActivity).packageName
        val manuallySettings = view.findViewById<TextView>(R.id.manuallySettings)
        val pm = requireContext().applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        manuallySettings.makeLinks(
            Pair(getString(R.string.pairDeclaration1), View.OnClickListener {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.alreadyGrantAllTimePositioningPermission), Toast.LENGTH_SHORT).show()
                }
            }),
            Pair(getString(R.string.pairDeclaration2), View.OnClickListener {
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    Toast.makeText(requireContext(), getString(R.string.alreadyStopBatteryOptimization), Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            })
        )

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        val professionalSentence = view.findViewById<TextView>(R.id.professionalSentence)
        professionalSentence.makeLinks(
            Pair("(?)", View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.whatIsProfessionalTitle))
                    .setMessage(getString(R.string.whatIsProfessionalMessage))
                    .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ -> }
                    .show()
            })
        )
    }

    override fun onResume() {
        super.onResume()

        (activity as AppCompatActivity).findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()

        (activity as AppCompatActivity).findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.VISIBLE
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    private fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
        val spannableString = SpannableString(this.text)
        var startIndexOfLink = -1
        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    textPaint.color = textPaint.linkColor
                    textPaint.isUnderlineText = true
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    link.second.onClick(view)
                }
            }
            startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)

            spannableString.setSpan(
                clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        this.movementMethod =
            LinkMovementMethod.getInstance()
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    private fun View.blink(
        times: Int = Animation.INFINITE,
        duration: Long = 50L,
        offset: Long = 20L,
        minAlpha: Float = 0.0f,
        maxAlpha: Float = 1.0f,
        repeatMode: Int = Animation.REVERSE
    ) {
        startAnimation(AlphaAnimation(minAlpha, maxAlpha).also {
            it.duration = duration
            it.startOffset = offset
            it.repeatMode = repeatMode
            it.repeatCount = times
        })
    }

}