package it.polito.interdisciplinaryProjects2021.smartpresence.introduction

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.polito.interdisciplinaryProjects2021.smartpresence.R

class IntroductionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_introduction, container, false)
    }

    @SuppressLint("BatteryLife", "WifiManagerLeak")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            (activity as AppCompatActivity).supportActionBar?.hide()
        }

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val firstInstallChecking = sharedPreferences.getString("firstInstallChecking", "true")?.toBoolean()
        if (firstInstallChecking == true) {
            with(sharedPreferences.edit()) {
                putString("firstInstallChecking", "false")
                commit()
            }

            showAlertDialog(getString(R.string.firstInstallRemindTitle), getString(R.string.firstInstallRemindMessage), requireContext())
        } else {
            val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!wifiManager.isWifiEnabled && !LocationManagerCompat.isLocationEnabled(locationManager)) {
                showAlertDialog(getString(R.string.wifiOrLocationServiceOffAlertTitle), getString(R.string.wifiOffLocationOffAlertMessage), requireContext())
            } else if (wifiManager.isWifiEnabled && !LocationManagerCompat.isLocationEnabled(locationManager)) {
                showAlertDialog(getString(R.string.wifiOrLocationServiceOffAlertTitle), getString(R.string.wifiOnLocationOffAlertMessage), requireContext())
            } else if (!wifiManager.isWifiEnabled && LocationManagerCompat.isLocationEnabled(locationManager)) {
                showAlertDialog(getString(R.string.wifiOrLocationServiceOffAlertTitle), getString(R.string.wifiOffLocationOnAlertMessage), requireContext())
            }
        }

        val packageName = (activity as AppCompatActivity).packageName
        val introductionMoreText = view.findViewById<TextView>(R.id.introduction_more_text)
        introductionMoreText.makeLinks(
            Pair(getString(R.string.pairIntroduction1), View.OnClickListener {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.alreadyGrantAllTimePositioningPermission), Toast.LENGTH_SHORT).show()
                }
            }),
            Pair(getString(R.string.pairIntroduction2), View.OnClickListener {
                val pm = requireContext().applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
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

        val introductionPlusText = view.findViewById<TextView>(R.id.introduction_plus_text)
        introductionPlusText.makeLinks(
            Pair(getString(R.string.pairIntroduction3), View.OnClickListener {
                findNavController().navigate(R.id.infoBrandFragment)
            })
        )
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
        this.movementMethod = LinkMovementMethod.getInstance()
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    private fun showAlertDialog(title: String, content: String, context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(getString(R.string.energySavingModeAlertButton)) { _, _ -> }
            .show()
    }

    override fun onDetach() {
        super.onDetach()

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.faq_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.goto_faq_button -> {
                findNavController().navigate(R.id.faqFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}