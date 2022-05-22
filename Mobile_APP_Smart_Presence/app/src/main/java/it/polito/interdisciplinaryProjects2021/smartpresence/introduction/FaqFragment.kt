package it.polito.interdisciplinaryProjects2021.smartpresence.introduction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import java.util.*
import kotlin.collections.ArrayList

class FaqFragment : Fragment() {

    private lateinit var faqRecyclerView: RecyclerView
    private lateinit var faqList: ArrayList<QuestionAndAnswer>
    private lateinit var tempFaqList: ArrayList<QuestionAndAnswer>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_faq, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val questionList = resources.getStringArray(R.array.question_list)
        val answerList = resources.getStringArray(R.array.answer_list)

        faqList = arrayListOf()
        tempFaqList = arrayListOf()

        for (i in questionList.indices) {
            faqList.add(QuestionAndAnswer(questionList[i], answerList[i]))
        }
        tempFaqList.addAll(faqList)

        faqRecyclerView = view.findViewById(R.id.faqRecyclerView)
        faqRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val rvAdapter = FaqCardAdapter(faqRecyclerView, tempFaqList, requireContext())
        faqRecyclerView.adapter = rvAdapter
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
                tempFaqList.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if (searchText.isNotEmpty()) {
                    faqList.forEach {
                        if (it.question.lowercase(Locale.getDefault()).contains(searchText)) {
                            tempFaqList.add(it)
                        }
                    }
                    faqRecyclerView.adapter?.notifyDataSetChanged()
                } else {
                    tempFaqList.clear()
                    tempFaqList.addAll(faqList)
                    faqRecyclerView.adapter?.notifyDataSetChanged()
                }
                return false
            }
        })
    }

}

class FaqCardAdapter (
    private val recyclerView: RecyclerView,
    private val faqList: ArrayList<QuestionAndAnswer>,
    val context: Context,
): RecyclerView.Adapter<FaqCardAdapter.FaqCardViewHolder>() {
    class FaqCardViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val question: TextView = v.findViewById(R.id.question)
        val answer: TextView = v.findViewById(R.id.answer)
        val arrowBtn: ImageButton = v.findViewById(R.id.arrowBtn)
        val singleFaqCard: MaterialCardView = v.findViewById(R.id.singleFaqCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqCardViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.faq_card, parent, false)
        return FaqCardViewHolder(v)
    }

    override fun onBindViewHolder(holder: FaqCardViewHolder, position: Int) {
        holder.question.text = faqList[position].question
        holder.answer.text = faqList[position].answer

        holder.arrowBtn.setOnClickListener{
            dealWithExpandableView(it as ImageButton, holder.answer)
        }

        holder.singleFaqCard.setOnClickListener{
            dealWithExpandableView(holder.arrowBtn, holder.answer)
        }
    }

    override fun getItemCount(): Int {
        return faqList.size
    }

    private fun dealWithExpandableView(btn: ImageButton, view: TextView) {
        TransitionManager.beginDelayedTransition(recyclerView, AutoTransition())
        if (view.visibility == View.GONE) {
            view.visibility = View.VISIBLE
            btn.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
        } else {
            view.visibility = View.GONE
            btn.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
        }
    }
}

data class QuestionAndAnswer(var questionInput: String, var answerInput: String) {
    var question: String = questionInput
    var answer: String = answerInput
    var id: String = ""
}



//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        backLinearLayout = view.findViewById(R.id.backLinearLayout)
//
//        // this is not clever but it works
//
//        // 1
//        view.findViewById<CardView>(R.id.card1).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn1), view.findViewById(R.id.ans1))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn1).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans1))
//        }
//
//        // 2
//        view.findViewById<CardView>(R.id.card2).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn2), view.findViewById(R.id.ans2))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn2).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans2))
//        }
//
//        // 3
//        view.findViewById<CardView>(R.id.card3).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn3), view.findViewById(R.id.ans3))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn3).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans3))
//        }
//
//        // 4
//        view.findViewById<CardView>(R.id.card4).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn4), view.findViewById(R.id.ans4))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn4).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans4))
//        }
//
//        // 5
//        view.findViewById<CardView>(R.id.card5).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn5), view.findViewById(R.id.ans5))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn5).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans5))
//        }
//
//        // 6
//        view.findViewById<CardView>(R.id.card6).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn6), view.findViewById(R.id.ans6))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn6).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans6))
//        }
//
//        // 7
//        view.findViewById<CardView>(R.id.card7).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn7), view.findViewById(R.id.ans7))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn7).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans7))
//        }
//
//        // 8
//        view.findViewById<CardView>(R.id.card8).setOnClickListener {
//            dealWithExpandableView(view.findViewById(R.id.arrowBtn8), view.findViewById(R.id.ans8))
//        }
//        view.findViewById<ImageButton>(R.id.arrowBtn8).setOnClickListener {
//            dealWithExpandableView(it as ImageButton, view.findViewById(R.id.ans8))
//        }
//    }
//
//    private fun dealWithExpandableView(btn: ImageButton, view: TextView) {
//        TransitionManager.beginDelayedTransition(backLinearLayout, AutoTransition())
//        if (view.visibility == View.GONE) {
//            view.visibility = View.VISIBLE
//            btn.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
//        } else {
//            view.visibility = View.GONE
//            btn.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
//        }
//    }