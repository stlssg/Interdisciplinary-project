package it.polito.interdisciplinaryProjects2021.smartpresence.others

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.polito.interdisciplinaryProjects2021.smartpresence.R

class StartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val mAuth = FirebaseAuth.getInstance()
//        val user = mAuth.currentUser
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val logInOrNot = sharedPreferences.getString("logInOrNot", "false").toBoolean()
        if (logInOrNot){
            findNavController().navigate(R.id.nav_introduction)
        } else {
            val startTitle = view.findViewById<TextView>(R.id.startTitle)
            val startContent = view.findViewById<TextView>(R.id.startContent)
            val startAnim = view.findViewById<LottieAnimationView>(R.id.startAnim)

            startTitle.animate().translationY(-1600f).setDuration(1000).startDelay = 4000
            startContent.animate().translationY(-1600f).setDuration(1000).startDelay = 4000
            startAnim.animate().translationY(1600f).setDuration(1000).startDelay = 4000

            val handler = Handler()
            handler.postDelayed(Runnable {
                (activity as AppCompatActivity).supportActionBar?.show()
                val enableSplash = true
                if (enableSplash) {
                    findNavController().navigate(R.id.splashBaseFragment)
                } else {
                    findNavController().navigate(R.id.signInFragment)
                }
            },5500)
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as AppCompatActivity).findViewById<BottomNavigationView>(R.id.nav_view).visibility = GONE
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()

        (activity as AppCompatActivity).findViewById<BottomNavigationView>(R.id.nav_view).visibility = VISIBLE
        (activity as AppCompatActivity).supportActionBar?.show()
    }

}