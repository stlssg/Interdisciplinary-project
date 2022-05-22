package it.polito.interdisciplinaryProjects2021.smartpresence.splashScreen

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import me.relex.circleindicator.CircleIndicator3

class SplashBaseFragment : Fragment() {

    private var titleList = mutableListOf<String>()
    private var contentList = mutableListOf<String>()
    private var imageList = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash_base, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postToList()

        val splashScreenBaseViewPager = view.findViewById<ViewPager2>(R.id.splashScreenBaseViewPager)
        val navController = findNavController()
        splashScreenBaseViewPager.adapter = ViewPagerAdapter(titleList, contentList, imageList, getString(R.string.splashButtonStart), navController)
        splashScreenBaseViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        val indicator = view.findViewById<CircleIndicator3>(R.id.indicator)
        indicator.setViewPager(splashScreenBaseViewPager)
    }

    private fun addToList(title: String, content: String, image: Int) {
        titleList.add(title)
        contentList.add(content)
        imageList.add(image)
    }

    private fun postToList() {
        addToList(getString(R.string.splash1Title), getString(R.string.splash1Content), R.drawable.pic_splash_1)
        addToList(getString(R.string.splash2Title), getString(R.string.splash2Content), R.drawable.pic_splash_2)
        addToList(getString(R.string.splash3Title), getString(R.string.splash3Content), R.drawable.pic_splash_3)
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

}

class ViewPagerAdapter(private var title: List<String>,
                       private var content: List<String>,
                       private var image: List<Int>,
                       private var textForThirdPage: String,
                       private var navController: NavController): RecyclerView.Adapter<ViewPagerAdapter.Pager2ViewHolder>() {

    inner class Pager2ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val itemTitle: TextView = itemView.findViewById(R.id.splashTextTitle)
        val itemContent: TextView = itemView.findViewById(R.id.splashTextContent)
        val imageView: ImageView = itemView.findViewById(R.id.splashImage)
        val finishSplashButton: Button = itemView.findViewById(R.id.finishSplashButton)

        init {
            finishSplashButton.setOnClickListener{
                navController.navigate(R.id.signInFragment)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewPagerAdapter.Pager2ViewHolder {
        return Pager2ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.splash_content, parent, false))
    }

    override fun onBindViewHolder(holder: ViewPagerAdapter.Pager2ViewHolder, position: Int) {
        holder.itemTitle.text = title[position]
        holder.itemContent.text = content[position]
        holder.imageView.setImageResource(image[position])

        if (position == 2) {
            holder.finishSplashButton.text = textForThirdPage
        }
    }

    override fun getItemCount(): Int {
        return title.size
    }

}