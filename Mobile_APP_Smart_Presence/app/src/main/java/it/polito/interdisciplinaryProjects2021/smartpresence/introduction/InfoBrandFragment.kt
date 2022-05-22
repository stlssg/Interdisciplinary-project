package it.polito.interdisciplinaryProjects2021.smartpresence.introduction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import java.util.*
import kotlin.collections.ArrayList

class InfoBrandFragment : Fragment() {

    private lateinit var brandInfoRecyclerView: RecyclerView
    private lateinit var brandInfoList: ArrayList<BrandInfo>
    private lateinit var tempBrandInfoList: ArrayList<BrandInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_info_brand, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        brandInfoList = arrayListOf()
        tempBrandInfoList = arrayListOf()
        brandInfoList.add(BrandInfo(R.drawable.huawei_logo, getString(R.string.huawei_description)))
        brandInfoList.add(BrandInfo(R.drawable.samsung_logo, getString(R.string.samsung_description)))
        brandInfoList.add(BrandInfo(R.drawable.oneplus_logo, getString(R.string.onePlus_description)))
        brandInfoList.add(BrandInfo(R.drawable.meizu_logo, getString(R.string.meizu_description)))
        brandInfoList.add(BrandInfo(R.drawable.ic_baseline_phone_android_24, "More information will be available soon."))
        tempBrandInfoList.addAll(brandInfoList)

        brandInfoRecyclerView = view.findViewById(R.id.brandInfoRecyclerView)
        brandInfoRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val rvAdapter = BrandInfoCardAdapter(tempBrandInfoList, requireContext())
        brandInfoRecyclerView.adapter = rvAdapter

        val contactAppProvider = view.findViewById<TextView>(R.id.textView7)
        val appProviderEmail = "s287288@studenti.polito.it"
        contactAppProvider.makeLinks(
            Pair(appProviderEmail, View.OnClickListener {
                val action = InfoBrandFragmentDirections.actionInfoBrandFragmentToSendEmailFragment(targetEmail=appProviderEmail)
                findNavController().navigate(action)
            })
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)
        val item = menu.findItem(R.id.search_faq)
        val searchView = item.actionView as SearchView
        searchView.setIconifiedByDefault(true)

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(newText: String?): Boolean {
                tempBrandInfoList.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if (searchText.isNotEmpty()) {
                    brandInfoList.forEach {
                        if (it.brandInfoText.lowercase(Locale.getDefault()).contains(searchText)) {
                            tempBrandInfoList.add(it)
                        }
                    }
                    brandInfoRecyclerView.adapter?.notifyDataSetChanged()
                } else {
                    tempBrandInfoList.clear()
                    tempBrandInfoList.addAll(brandInfoList)
                    brandInfoRecyclerView.adapter?.notifyDataSetChanged()
                }
                return false
            }
        })
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

}

class BrandInfoCardAdapter (
    private val brandInfoList: ArrayList<BrandInfo>,
    val context: Context,
): RecyclerView.Adapter<BrandInfoCardAdapter.BrandInfoCardViewHolder>() {
    class BrandInfoCardViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val brandInfoImage: ImageView = v.findViewById(R.id.brandInfoImage)
        val brandInfoText: TextView = v.findViewById(R.id.brandInfoText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandInfoCardViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.brand_info_card, parent, false)
        return BrandInfoCardViewHolder(v)
    }

    override fun onBindViewHolder(holder: BrandInfoCardViewHolder, position: Int) {
        holder.brandInfoImage.setImageResource(brandInfoList[position].brandImageID)
        holder.brandInfoText.text = brandInfoList[position].brandInfoText
    }

    override fun getItemCount(): Int {
        return brandInfoList.size
    }
}

data class BrandInfo(var brandImageIDInput: Int, var brandInfoTextInput: String) {
    var brandImageID: Int = brandImageIDInput
    var brandInfoText: String = brandInfoTextInput
    var id: String = ""
}