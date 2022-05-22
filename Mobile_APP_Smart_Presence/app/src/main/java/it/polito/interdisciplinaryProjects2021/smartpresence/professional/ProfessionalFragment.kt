package it.polito.interdisciplinaryProjects2021.smartpresence.professional

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sun.mail.imap.protocol.FetchResponse.getItem
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import java.util.*
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")
class ProfessionalFragment : Fragment() {

    private lateinit var blurView: BlurView
    private lateinit var grantAccessLayout: LinearLayout
    private lateinit var buildingListLayout: ConstraintLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var menu: Menu
    private lateinit var myBuildingListRecyclerView: RecyclerView
    private lateinit var buildingList: ArrayList<String>
    private lateinit var tempBuildingList: ArrayList<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_professional, container, false)
    }

    @SuppressLint("LongLogTag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        sharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        grantAccessLayout = view.findViewById<LinearLayout>(R.id.grant_access_layout)
        buildingListLayout = view.findViewById<ConstraintLayout>(R.id.buildingListLayout)

        blurView = view.findViewById<BlurView>(R.id.proFragmentBlurView)
        blurBackground(blurView)
        blurView.setOnClickListener{
            blurView.visibility = View.GONE
            grantAccessLayout.visibility = View.GONE
            buildingListLayout.visibility = View.GONE
            menu.getItem(0).setIcon(R.drawable.ic_baseline_lock_open_24)
            menu.getItem(1).setIcon(R.drawable.ic_baseline_format_list_bulleted_24)
            menu.getItem(0).isEnabled = true
            menu.getItem(1).isEnabled = true
        }

        val myViewPager = view.findViewById<ViewPager>(R.id.myViewPager)
        myViewPager.adapter = PageAdapter(childFragmentManager, getString(R.string.fragment_name_graph), getString(
            R.string.proActionTabName
        ))
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.setupWithViewPager(myViewPager)
        tabLayout.getTabAt(0)?.setIcon(R.drawable.ic_baseline_bar_chart_24)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_baseline_list_24)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            (activity as AppCompatActivity).supportActionBar?.hide()
            tabLayout.visibility = View.GONE
        }

        val noBuildingMessage = view.findViewById<TextView>(R.id.noBuildingMessage)
        myBuildingListRecyclerView = view.findViewById<RecyclerView>(R.id.building_list_recyclerView)
        myBuildingListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val searchView = view.findViewById<SearchView>(R.id.search_building)

//        val buildingList = mutableListOf<String>()
        val db = Firebase.firestore
        db.collection("BuildingNameList")
            .get()
            .addOnSuccessListener { result ->
                buildingList = arrayListOf<String>()
                tempBuildingList = arrayListOf<String>()
                if (sharedPreferences.getString("address", "nothing").toString() != "nothing") {
                    buildingList.add(sharedPreferences.getString("address", "nothing").toString())
                }
                for (document in result) {
                    if (document.id != sharedPreferences.getString("address", "nothing").toString()) {
                        buildingList.add(document.id)
                    }
                }
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")
//                buildingList.add("xxxxxxxxxxxxxxx")

                tempBuildingList.addAll(buildingList)

                if (buildingList.size == 0) {
                    noBuildingMessage.visibility = View.VISIBLE
                    myBuildingListRecyclerView.visibility = View.GONE
                } else {
                    noBuildingMessage.visibility = View.GONE
                    myBuildingListRecyclerView.visibility = View.VISIBLE
                }

                val rvAdapter = BuildingCardListAdapter(
                    tempBuildingList,
                    requireContext(),
                    blurView,
                    buildingListLayout,
                    grantAccessLayout,
                    getString(R.string.select_address_message),
                    sharedPreferences,
                    fragmentManager,
                    this
                )
                myBuildingListRecyclerView.adapter = rvAdapter

                searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    @SuppressLint("NotifyDataSetChanged")
                    override fun onQueryTextChange(newText: String?): Boolean {
                        tempBuildingList.clear()
                        val searchText = newText!!.toLowerCase(Locale.getDefault())
                        if (searchText.isNotEmpty()) {
                            buildingList.forEach {
                                if (it.toLowerCase(Locale.getDefault()).contains(searchText)) {
                                    tempBuildingList.add(it)
                                }
                            }
                            myBuildingListRecyclerView.adapter?.notifyDataSetChanged()
                        } else {
                            tempBuildingList.clear()
                            tempBuildingList.addAll(buildingList)
                            myBuildingListRecyclerView.adapter?.notifyDataSetChanged()
                        }
                        return false
                    }

                })
            }
            .addOnFailureListener { exception ->
                Log.d("Error getting documents: ", "$exception")
            }

        val goBackToTopFab = view.findViewById<FloatingActionButton>(R.id.goBackToTopFab)
        goBackToTopFab.setOnClickListener{
            myBuildingListRecyclerView.smoothScrollToPosition(0)
//            Log.d("recyclerview first position: ", "${(myBuildingListRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()}")
//            Log.d("recyclerview last position: ", "${(myBuildingListRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()}")
        }

        val verifyCodeButton = view.findViewById<Button>(R.id.verifyCodeButton)
        verifyCodeButton.setOnClickListener {
            val account = sharedPreferences.getString("keyCurrentAccount", "noEmail")
            val accessCodeInput = requireView().findViewById<TextInputLayout>(R.id.accessCodeInput).editText?.text.toString()
            db.collection("RegisteredUser").document(account!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document.data?.get("accessCode") == null || document.data?.get("accessCode") != accessCodeInput) {
                        Toast.makeText(context, getString(R.string.wrong_code_message), Toast.LENGTH_SHORT).show()
                    } else {
                        with(sharedPreferences.edit()) {
                            putString( "professionalAccessGranted", "true")
                            commit()
                        }
                        Toast.makeText(context, getString(R.string.right_code_message), Toast.LENGTH_SHORT).show()
                        blurView.visibility = View.GONE
                        grantAccessLayout.visibility = View.GONE
                        buildingListLayout.visibility = View.GONE
                        menu.getItem(0).setIcon(R.drawable.ic_baseline_lock_open_24)
                        menu.getItem(1).isEnabled = true
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("get failed with ", "$exception")
                }
        }
    }

    override fun onDetach() {
        super.onDetach()

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        (activity as AppCompatActivity).supportActionBar?.show()
        view?.findViewById<TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        inflater.inflate(R.menu.professional_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_access_grant -> {
                if (blurView.visibility == View.VISIBLE) {
                    menu.getItem(0).setIcon(R.drawable.ic_baseline_lock_open_24)
                    blurView.visibility = View.GONE
                    grantAccessLayout.visibility = View.GONE
                    buildingListLayout.visibility = View.GONE
                    menu.getItem(1).isEnabled = true
                } else {
                    menu.getItem(1).isEnabled = false
                    menu.getItem(0).setIcon(R.drawable.ic_baseline_close_24)
                    blurView.visibility = View.VISIBLE
                    grantAccessLayout.visibility = View.VISIBLE
                }

                return true
            }
            R.id.action_list_building -> {
                if (sharedPreferences.getString("professionalAccessGranted", "false").toBoolean()) {
                    if (blurView.visibility == View.VISIBLE) {
                        menu.getItem(1).setIcon(R.drawable.ic_baseline_format_list_bulleted_24)
                        blurView.visibility = View.GONE
                        buildingListLayout.visibility = View.GONE
                        grantAccessLayout.visibility = View.GONE
                        menu.getItem(0).isEnabled = true
                    } else {
                        menu.getItem(0).isEnabled = false
                        menu.getItem(1).setIcon(R.drawable.ic_baseline_close_24)
                        blurView.visibility = View.VISIBLE
                        buildingListLayout.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(context, getString(R.string.not_grant_pro_access_message), Toast.LENGTH_SHORT).show()
                }

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun blurBackground(blurView: BlurView) {
        val decorView = activity?.window?.decorView
        val rootView = decorView?.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = decorView?.background

        blurView.setupWith(rootView!!)
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(RenderScriptBlur(requireContext()))
            .setBlurRadius(10f)
            .setBlurAutoUpdate(true)
            .setHasFixedTransformationMatrix(true)
    }

}

@Suppress("DEPRECATION")
class PageAdapter(fm: FragmentManager, private val tabName_1: String, private val tabName_2: String) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                GraphFragment()
            }
            1 -> {
                ProActionFragment()
            }
            else -> {
                GraphFragment()
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> { return tabName_1 }
            1 -> { return tabName_2 }
        }
        return super.getPageTitle(position)
    }

    override fun getCount(): Int {
        return 2
    }

}

class BuildingCardListAdapter (
    private val buildingList: List<String>,
    val context: Context,
    val blurView: BlurView,
    private val buildingListLayout: ConstraintLayout,
    private val grantAccessLayout: LinearLayout,
    private val messageString: String,
    private val sharedPreferences: SharedPreferences,
    private val fm: FragmentManager?,
    private val fragment: Fragment
): RecyclerView.Adapter<BuildingCardListAdapter.BuildingCardViewHolder>() {
    class BuildingCardViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val listIndex: TextView = v.findViewById(R.id.listIndex)
        val addressText: TextView = v.findViewById(R.id.addressText)
        val singleBuildingCard: MaterialCardView = v.findViewById(R.id.singleBuildingCard)
        val targetBuildingMsgForCard: TextView = v.findViewById(R.id.targetBuildingMsgForCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingCardViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.single_building_card, parent, false)
        return BuildingCardViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BuildingCardViewHolder, position: Int) {
        if (sharedPreferences.getString("address", "nothing").toString() == "nothing") {
            holder.listIndex.text = (position + 1).toString()
        } else {
            if (position == 0) { holder.targetBuildingMsgForCard.visibility = View.VISIBLE }
            holder.listIndex.text = position.toString()
        }
        holder.addressText.text = buildingList[position].replace("_", " ")

        val targetBuildingForPro = sharedPreferences.getString("targetBuildingForPro", "nothing")
        holder.singleBuildingCard.isChecked = holder.addressText.text.toString().replace(" ", "_") == targetBuildingForPro

        holder.singleBuildingCard.setOnClickListener{
            Toast.makeText(context, messageString, Toast.LENGTH_SHORT).show()

            blurView.visibility = View.GONE
            buildingListLayout.visibility = View.GONE
            grantAccessLayout.visibility = View.GONE

//            (it as MaterialCardView).isChecked = !it.isChecked

            with(sharedPreferences.edit()) {
                putString( "targetBuildingForPro", holder.addressText.text.toString().replace(" ", "_"))
                apply()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fm?.beginTransaction()?.detach(fragment)?.commitNow()
                fm?.beginTransaction()?.attach(fragment)?.commitNow()
            } else {
                fm?.beginTransaction()?.detach(fragment)?.attach(fragment)?.commit()
            }
        }
    }

    override fun getItemCount(): Int {
        return buildingList.size
    }
}