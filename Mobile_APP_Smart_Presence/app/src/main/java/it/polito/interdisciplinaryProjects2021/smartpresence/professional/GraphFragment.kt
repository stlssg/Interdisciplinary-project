package it.polito.interdisciplinaryProjects2021.smartpresence.professional

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import com.github.aachartmodel.aainfographics.aachartcreator.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import it.polito.interdisciplinaryProjects2021.smartpresence.R
import java.util.*

class GraphFragment : Fragment() {

    private val dailyData : MutableList<Float> = mutableListOf()
    private val weeklyData : MutableList<Float> = mutableListOf()
    private val monthlyData : MutableList<Float> = mutableListOf()
    private val yearlyData : MutableList<Float> = mutableListOf()
    private val totalData : MutableList<Float> = mutableListOf()

    private val dailyLabel : MutableList<String> = mutableListOf()
    private val weeklyLabel : MutableList<String> = mutableListOf()
    private val monthlyLabel : MutableList<String> = mutableListOf()
    private val yearlyLabel : MutableList<String> = mutableListOf()
    private val totalLabel : MutableList<String> = mutableListOf()

    private var resolutionSelection: String = "daily"
    private lateinit var withStandardOrNotCheckBox: CheckBox
    private val standardOccupancyWeekday: MutableList<Float> = mutableListOf(
        1f * 100,
        1f * 100,
        1f * 100,
        1f * 100,
        1f * 100,
        1f * 100,
        0.5f * 100,
        0.5f * 100,
        0.5f * 100,
        0.1f * 100,
        0.1f * 100,
        0.1f * 100,
        0.1f * 100,
        0.2f * 100,
        0.2f * 100,
        0.2f * 100,
        0.5f * 100,
        0.5f * 100,
        0.5f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        1f * 100,
        1f * 100
    )

    private val standardOccupancyWeekend: MutableList<Float> = mutableListOf(
        1f * 100,
        1f * 100,
        1f * 100,
        1f * 100,
        1f * 100,
        1f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        0.8f * 100,
        1f * 100,
        1f * 100
    )

    private val monthListFixedString: MutableList<String> = mutableListOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    @Suppress("NAME_SHADOWING")
    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)
        val targetBuildingForPro = sharedPreferences.getString("targetBuildingForPro", "nothing")
        val languageSpinnerPosition = sharedPreferences.getString("languageSpinnerPosition", "0")?.toInt()
        val noTargetBuildingText = view.findViewById<TextView>(R.id.noTargetBuildingText)
        val graphConstraintLayout = view.findViewById<ConstraintLayout>(R.id.graphConstraintLayout)
        withStandardOrNotCheckBox = view.findViewById(R.id.withStandardOrNotCheckBox)
        withStandardOrNotCheckBox.isChecked = false

        if (targetBuildingForPro == "nothing") {
            noTargetBuildingText.visibility = VISIBLE
            when (languageSpinnerPosition) {
                1 -> {
                    noTargetBuildingText.text = "Non hai ancora specificato il tuo edificio di destinazione."
                }
                2 -> {
                    noTargetBuildingText.text = "您尚未指定目标建筑."
                }
                else -> {
                    noTargetBuildingText.text = "You haven't specify your target building yet."
                }
            }
        } else {
            val db = Firebase.firestore
            val docRef = db.collection(targetBuildingForPro!!).document("Results")
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.data?.get("daily_available") == true ||
                        document.data?.get("weekly_available") == true ||
                        document.data?.get("monthly_12_available") == true ||
                        document.data?.get("yearly_available") == true) {
                        noTargetBuildingText.visibility = GONE
                        graphConstraintLayout.visibility = VISIBLE

                        val aaChartView = view.findViewById<AAChartView>(R.id.aa_chart_view)
                        aaChartView.visibility = GONE
                        val errorTextView = view.findViewById<TextView>(R.id.errorTextView)
                        errorTextView.visibility = GONE
                        val loadingAnim = view.findViewById<LottieAnimationView>(R.id.loadingAnim)

                        val dailyOption = view.findViewById<TextView>(R.id.dailyOption)
                        val weeklyOption = view.findViewById<TextView>(R.id.weeklyOption)
                        val monthlyOption = view.findViewById<TextView>(R.id.monthlyOption)
                        val yearlyOption = view.findViewById<TextView>(R.id.yearlyOption)

                        val monthSpinner = view.findViewById<Spinner>(R.id.monthSpinner)
                        val yearSpinner = view.findViewById<Spinner>(R.id.yearSpinner)

                        val monthList = resources.getStringArray(R.array.monthList)
                        if (monthSpinner != null) {
                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthList)
                            monthSpinner.adapter = adapter
                        }

                        if (document.data?.get("yearly_available") == true) {
                            docRef.collection("yearly_data")
                                .get()
                                .addOnSuccessListener { documents ->
                                    val yearListTemp = mutableListOf<String>()
                                    var tempYear = " "
                                    for (doc in documents) {
                                        val tempStr = doc.id.slice(0..3)
                                        if (tempStr != tempYear) {
                                            yearListTemp.add(tempStr)
                                            tempYear = tempStr
                                        }
                                    }

                                    val yearList: Array<String> = yearListTemp.toTypedArray()
                                    if (yearSpinner != null) {
                                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, yearList)
                                        yearSpinner.adapter = adapter
                                    }
                                }
                        }

//                        val totalOption = view.findViewById<TextView>(R.id.totalOption)
//                        dailyOption.text = getString(R.string.graphOptionDaily)
//                        weeklyOption.text = getString(R.string.graphOptionWeekly)
//                        monthlyOption.text = getString(R.string.graphOptionMonthly)
//                        yearlyOption.text = getString(R.string.graphOptionYearly)
//                        totalOption.text = getString(R.string.graphOptionTotal)

                        when (sharedPreferences.getString("languageSpinnerPosition", "0")?.toInt()) {
                            1 -> {
                                dailyOption.text = "Quotidiano"
                                weeklyOption.text = "Settimanalmente"
                                monthlyOption.text = "Mensile"
                                yearlyOption.text = "Annuale"
//                                totalOption.text = "Totale"
                                withStandardOrNotCheckBox.text = "Standard"
                            }
                            2 -> {
                                dailyOption.text = "日"
                                weeklyOption.text = "周"
                                monthlyOption.text = "月"
                                yearlyOption.text = "年"
//                                totalOption.text = "总"
                                withStandardOrNotCheckBox.text = "标准"
                            }
                            else -> {
                                dailyOption.text = "Daily"
                                weeklyOption.text = "Weekly"
                                monthlyOption.text = "Monthly"
                                yearlyOption.text = "Yearly"
//                                totalOption.text = "Total"
                                withStandardOrNotCheckBox.text = "Standard"
                            }
                        }

                        dailyOption.setOnClickListener {
                            resolutionSelection = "daily"
                            resetData()
                            setButton(dailyOption, weeklyOption, monthlyOption, yearlyOption)
//                            setButton(dailyOption, weeklyOption, monthlyOption, yearlyOption, totalOption)
                            errorTextView.visibility = GONE
                            loadingAnim.visibility = VISIBLE

                            docRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.data?.get("daily_available") == true) {
                                        docRef.collection("daily_data")
                                            .orderBy("interval")
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                loadingAnim.visibility = GONE
                                                aaChartView.visibility = VISIBLE
                                                for (document in documents) {
                                                    dailyLabel.add(document.data.getValue("interval").toString())
                                                    dailyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                                }
                                                aaChartView.aa_drawChartWithChartModel(
                                                    generateChart(
                                                        dailyLabel,
                                                        dailyData,
                                                        getString(R.string.graphDailyTitle),
                                                        getString(R.string.graphDailySubtitle),
                                                        targetBuildingForPro.replace("_", " ")
                                                    )
                                                )
                                            }
                                    } else {
                                        loadingAnim.visibility = GONE
                                        aaChartView.visibility = GONE
                                        errorTextView.visibility = VISIBLE
                                        errorTextView.text = getString(R.string.graphNoDataMessage)
                                    }
                                }
                                .addOnFailureListener {
                                    loadingAnim.visibility = GONE
                                    aaChartView.visibility = GONE
                                    errorTextView.visibility = VISIBLE
                                    errorTextView.text = getString(R.string.graphErrorMessage)
                                }
                        }

                        weeklyOption.setOnClickListener {
                            resolutionSelection = "weekly"
                            resetData()
                            setButton(weeklyOption, dailyOption, monthlyOption, yearlyOption)
//                            setButton(weeklyOption, dailyOption, monthlyOption, yearlyOption, totalOption)
                            errorTextView.visibility = GONE
                            loadingAnim.visibility = VISIBLE

                            docRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.data?.get("weekly_available") == true) {
                                        docRef.collection("weekly_data")
                                            .orderBy("interval")
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                loadingAnim.visibility = GONE
                                                aaChartView.visibility = VISIBLE
                                                for (document in documents) {
                                                    weeklyLabel.add(document.data.getValue("interval").toString().substring(2))
                                                    weeklyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                                }
                                                aaChartView.aa_drawChartWithChartModel(
                                                    generateChart(
                                                        weeklyLabel,
                                                        weeklyData,
                                                        getString(R.string.graphWeeklyTitle),
                                                        getString(R.string.graphWeeklySubtitle),
                                                        targetBuildingForPro.replace("_", " ")
                                                    )
                                                )
                                            }
                                    } else {
                                        loadingAnim.visibility = GONE
                                        aaChartView.visibility = GONE
                                        errorTextView.visibility = VISIBLE
                                        errorTextView.text = getString(R.string.graphNoDataMessage)
                                    }
                                }
                                .addOnFailureListener {
                                    loadingAnim.visibility = GONE
                                    aaChartView.visibility = GONE
                                    errorTextView.visibility = VISIBLE
                                    errorTextView.text = getString(R.string.graphErrorMessage)
                                }
                        }

                        monthlyOption.setOnClickListener {
                            resolutionSelection = "monthly"
                            resetData()
                            setButton(monthlyOption, dailyOption, weeklyOption, yearlyOption)
                            errorTextView.visibility = GONE
                            loadingAnim.visibility = VISIBLE

                            docRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.data?.get("monthly_12_available") == true) {
                                        docRef.collection("monthly_data")
                                            .orderBy("interval")
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                loadingAnim.visibility = GONE
                                                aaChartView.visibility = VISIBLE
                                                val selectedMonth = monthListFixedString[monthSpinner.selectedItemPosition]
                                                for (document in documents) {
                                                    if (selectedMonth == document.id.slice(0..2)) {
                                                        monthlyLabel.add(document.data.getValue("interval").toString())
                                                        monthlyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                                    }
                                                }
                                                if (monthlyData[0] < 0) {
                                                    aaChartView.visibility = GONE
                                                    errorTextView.visibility = VISIBLE
                                                    errorTextView.text = getString(R.string.graphNoDataMessage)
                                                } else {
                                                    aaChartView.aa_drawChartWithChartModel(
                                                        generateChart(
                                                            monthlyLabel,
                                                            monthlyData,
                                                            getString(R.string.graphDailyTitle) + " (${selectedMonth})",
                                                            getString(R.string.graphDailySubtitle) + " in $selectedMonth",
                                                            targetBuildingForPro.replace("_", " ")
                                                        )
                                                    )
                                                }
                                            }
                                    } else {
                                        loadingAnim.visibility = GONE
                                        aaChartView.visibility = GONE
                                        errorTextView.visibility = VISIBLE
                                        errorTextView.text = getString(R.string.graphNoDataMessage)
                                    }
                                }
                                .addOnFailureListener {
                                    loadingAnim.visibility = GONE
                                    aaChartView.visibility = GONE
                                    errorTextView.visibility = VISIBLE
                                    errorTextView.text = getString(R.string.graphErrorMessage)
                                }
                        }

                        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                if (resolutionSelection == "monthly") {
                                    resetData()
                                    setButton(monthlyOption, dailyOption, weeklyOption, yearlyOption)
                                    errorTextView.visibility = GONE
                                    loadingAnim.visibility = VISIBLE

                                    docRef.get()
                                        .addOnSuccessListener { document ->
                                            if (document.data?.get("monthly_12_available") == true) {
                                                docRef.collection("monthly_data")
                                                    .orderBy("interval")
                                                    .get()
                                                    .addOnSuccessListener { documents ->
                                                        loadingAnim.visibility = GONE
                                                        aaChartView.visibility = VISIBLE
                                                        val selectedMonth = monthListFixedString[monthSpinner.selectedItemPosition]
                                                        for (document in documents) {
                                                            if (selectedMonth == document.id.slice(0..2)) {
                                                                monthlyLabel.add(document.data.getValue("interval").toString())
                                                                monthlyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                                            }
                                                        }
                                                        if (monthlyData[0] < 0) {
                                                            aaChartView.visibility = GONE
                                                            errorTextView.visibility = VISIBLE
                                                            errorTextView.text = getString(R.string.graphNoDataMessage)
                                                        } else {
                                                            aaChartView.aa_drawChartWithChartModel(
                                                                generateChart(
                                                                    monthlyLabel,
                                                                    monthlyData,
                                                                    getString(R.string.graphDailyTitle) + " (${selectedMonth})",
                                                                    getString(R.string.graphDailySubtitle) + " in $selectedMonth",
                                                                    targetBuildingForPro.replace("_", " ")
                                                                )
                                                            )
                                                        }
                                                    }
                                            } else {
                                                loadingAnim.visibility = GONE
                                                aaChartView.visibility = GONE
                                                errorTextView.visibility = VISIBLE
                                                errorTextView.text = getString(R.string.graphNoDataMessage)
                                            }
                                        }
                                        .addOnFailureListener {
                                            loadingAnim.visibility = GONE
                                            aaChartView.visibility = GONE
                                            errorTextView.visibility = VISIBLE
                                            errorTextView.text = getString(R.string.graphErrorMessage)
                                        }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) { }
                        }

                        yearlyOption.setOnClickListener {
                            resolutionSelection = "yearly"
                            resetData()
                            setButton(yearlyOption, dailyOption, weeklyOption, monthlyOption)
                            errorTextView.visibility = GONE
                            loadingAnim.visibility = VISIBLE

                            docRef.get()
                                .addOnSuccessListener { document ->
                                    if (document.data?.get("yearly_available") == true) {
                                        docRef.collection("yearly_data")
                                            .orderBy("interval")
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                loadingAnim.visibility = GONE
                                                aaChartView.visibility = VISIBLE
                                                val selectedYear = yearSpinner.selectedItem.toString()
                                                for (document in documents) {
                                                    if (selectedYear == document.id.slice(0..3)) {
                                                        yearlyLabel.add(document.data.getValue("interval").toString())
                                                        yearlyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                                    }
                                                }
                                                aaChartView.aa_drawChartWithChartModel(
                                                    generateChart(
                                                        yearlyLabel,
                                                        yearlyData,
                                                        getString(R.string.graphDailyTitle) + " (${selectedYear})",
                                                        getString(R.string.graphDailySubtitle) + " in $selectedYear",
                                                        targetBuildingForPro.replace("_", " ")
                                                    )
                                                )
                                            }
                                    } else {
                                        loadingAnim.visibility = GONE
                                        aaChartView.visibility = GONE
                                        errorTextView.visibility = VISIBLE
                                        errorTextView.text = getString(R.string.graphNoDataMessage)
                                    }
                                }
                                .addOnFailureListener {
                                    loadingAnim.visibility = GONE
                                    aaChartView.visibility = GONE
                                    errorTextView.visibility = VISIBLE
                                    errorTextView.text = getString(R.string.graphErrorMessage)
                                }
                        }

                        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                if (resolutionSelection == "yearly") {
                                    resetData()
                                    setButton(yearlyOption, dailyOption, weeklyOption, monthlyOption)
                                    errorTextView.visibility = GONE
                                    loadingAnim.visibility = VISIBLE

                                    docRef.get()
                                        .addOnSuccessListener { document ->
                                            if (document.data?.get("yearly_available") == true) {
                                                docRef.collection("yearly_data")
                                                    .orderBy("interval")
                                                    .get()
                                                    .addOnSuccessListener { documents ->
                                                        loadingAnim.visibility = GONE
                                                        aaChartView.visibility = VISIBLE
                                                        val selectedYear = yearSpinner.selectedItem.toString()
                                                        for (document in documents) {
                                                            if (selectedYear == document.id.slice(0..3)) {
                                                                yearlyLabel.add(document.data.getValue("interval").toString())
                                                                yearlyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                                            }
                                                        }
                                                        aaChartView.aa_drawChartWithChartModel(
                                                            generateChart(
                                                                yearlyLabel,
                                                                yearlyData,
                                                                getString(R.string.graphDailyTitle) + " (${selectedYear})",
                                                                getString(R.string.graphDailySubtitle) + " in $selectedYear",
                                                                targetBuildingForPro.replace("_", " ")
                                                            )
                                                        )
                                                    }
                                            } else {
                                                loadingAnim.visibility = GONE
                                                aaChartView.visibility = GONE
                                                errorTextView.visibility = VISIBLE
                                                errorTextView.text = getString(R.string.graphNoDataMessage)
                                            }
                                        }
                                        .addOnFailureListener {
                                            loadingAnim.visibility = GONE
                                            aaChartView.visibility = GONE
                                            errorTextView.visibility = VISIBLE
                                            errorTextView.text = getString(R.string.graphErrorMessage)
                                        }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) { }
                        }

//                        val example_results = db.collection("example_result").document("Results")
//                        totalOption.setOnClickListener {
//                            resetData()
//                            setButton(totalOption, dailyOption, weeklyOption, monthlyOption, yearlyOption)
//                            errorTextView.visibility = GONE
//                            loadingAnim.visibility = VISIBLE
//
//                            example_results.get()
//                                .addOnSuccessListener { document ->
//                                    if (document.data?.get("total_available") == true) {
//                                        example_results.collection("total_data")
//                                            .orderBy("interval")
//                                            .get()
//                                            .addOnSuccessListener { documents ->
//                                                loadingAnim.visibility = GONE
//                                                aaChartView.visibility = VISIBLE
//                                                for (document in documents) {
//                                                    totalLabel.add(document.data.getValue("interval").toString())
//                                                    totalData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
//                                                }
//                                                aaChartView.aa_drawChartWithChartModel(
//                                                    generateChart(
//                                                        totalLabel,
//                                                        totalData,
//                                                        getString(R.string.graphDailyTitle),
//                                                        getString(R.string.graphDailySubtitle),
//                                                        targetBuildingForPro.replace("_", " ")
//                                                    )
//                                                )
//                                            }
//                                    } else {
//                                        loadingAnim.visibility = GONE
//                                        aaChartView.visibility = GONE
//                                        errorTextView.visibility = VISIBLE
//                                        errorTextView.text = getString(R.string.graphNoDataMessage)
//                                    }
//                                }
//                                .addOnFailureListener { _ ->
//                                    loadingAnim.visibility = GONE
//                                    aaChartView.visibility = GONE
//                                    errorTextView.visibility = VISIBLE
//                                    errorTextView.text = getString(R.string.graphErrorMessage)
//                                }
//                        }

                        // initialization for graph of daily and need more!!!!!!!
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document.data?.get("daily_available") == true) {
                                    docRef.collection("daily_data")
                                        .orderBy("interval")
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            loadingAnim.visibility = GONE
                                            aaChartView.visibility = VISIBLE
                                            for (document in documents) {
//                                                Log.d("data!!!!!!!", "${document.id} => " + "${document.data.getValue("occupancy")} and " + "${document.data.getValue("time_interval")}")
                                                dailyLabel.add(document.data.getValue("interval").toString())
                                                dailyData.add(document.data.getValue("occupancy").toString().toFloat() * 100)
                                            }
//                                            Log.d("data!!!!!!!", "$dailyData and $dailyLabel")
                                            aaChartView.aa_drawChartWithChartModel(
                                                generateChart(
                                                    dailyLabel,
                                                    dailyData,
                                                    getString(R.string.graphDailyTitle),
                                                    getString(R.string.graphDailySubtitle),
                                                    targetBuildingForPro.replace("_", " ")
                                                )
                                            )
                                        }
                                } else {
                                    loadingAnim.visibility = GONE
                                    aaChartView.visibility = GONE
                                    errorTextView.visibility = VISIBLE
                                    errorTextView.text = getString(R.string.graphNoDataMessage)
                                }
                            }
                            .addOnFailureListener {
                                loadingAnim.visibility = GONE
                                aaChartView.visibility = GONE
                                errorTextView.visibility = VISIBLE
                                errorTextView.text = getString(R.string.graphErrorMessage)
                            }
                    } else {
                        noTargetBuildingText.visibility = VISIBLE
                        when (languageSpinnerPosition) {
                            1 -> {
                                noTargetBuildingText.text = "I risultati non sono ancora disponibili in questo momento, per favore prova più tardi."
                            }
                            2 -> {
                                noTargetBuildingText.text = "暂时还没有结果，请稍后再试。"
                            }
                            else -> {
                                noTargetBuildingText.text = "The results are still not available at this moment, please try it later."
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("get failed with ", "$exception")
                }
        }

    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        when (sharedPreferences.getString("languageSpinnerPosition", "0")?.toInt()) {
            1 -> setLang("it")
            2 -> setLang("zh")
            else -> setLang("en")
        }

        resetData()
    }

    override fun onPause() {
        super.onPause()

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("AppSharedPreference", Context.MODE_PRIVATE)

        when (sharedPreferences.getString("languageSpinnerPosition", "0")?.toInt()) {
            1 -> setLang("it")
            2 -> setLang("zh")
            else -> setLang("en")
        }

        resetData()
    }

    private fun setLang(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        (activity as AppCompatActivity).baseContext.resources.updateConfiguration(config, (activity as AppCompatActivity).baseContext.resources.displayMetrics)
    }

    private fun generateChart(
        labels: MutableList<String>,
        data: MutableList<Float>,
        title: String,
        subtitle: String,
        buildingName: String
    ): AAChartModel {
        var inputColor = arrayOf<Any>("#fe117c")
        val inputArray = if (withStandardOrNotCheckBox.isChecked && data.size == 24) {
            inputColor = arrayOf("#fe117c", "#06caf4")
            arrayOf(
                AASeriesElement()
                    .name(buildingName)
                    .data(data.toTypedArray()),
                AASeriesElement()
                    .name("EN 16798-1:2019")
                    .data(standardOccupancyWeekday.toTypedArray())
            )
        } else if (withStandardOrNotCheckBox.isChecked && data.size >= 24) {
            inputColor = arrayOf("#fe117c", "#06caf4")
            val standardOccupancyInput: MutableList<Float> = mutableListOf()
            for (i in 0..4) { standardOccupancyInput.addAll(standardOccupancyWeekday) }
            for (i in 0..1) { standardOccupancyInput.addAll(standardOccupancyWeekend) }
            arrayOf(
                AASeriesElement()
                    .name(buildingName)
                    .data(data.toTypedArray()),
                AASeriesElement()
                    .name("EN 16798-1:2019")
                    .data(standardOccupancyInput.toTypedArray())
            )
        }else {
            arrayOf(
                AASeriesElement()
                    .name(buildingName)
                    .data(data.toTypedArray())
            )
        }

        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Area)
            .animationType(AAChartAnimationType.EaseOutBack)
            .title(title)
            .subtitle(subtitle)
            .yAxisTitle(getString(R.string.graphYTitle))
            .dataLabelsEnabled(false)
            .categories(labels.toTypedArray())
            .colorsTheme(inputColor)
            .series(inputArray)

        return aaChartModel
    }

//    @RequiresApi(Build.VERSION_CODES.M)
//    @SuppressLint("UseCompatLoadingForDrawables")
//    private fun setButton(selectedButton: TextView, nonSelectedButton: TextView) {
//        selectedButton.background = resources.getDrawable(R.drawable.switch_trcks,null)
//        selectedButton.setTextColor(resources.getColor(R.color.textColor,null))
//        nonSelectedButton.background = null
//        nonSelectedButton.setTextColor(resources.getColor(R.color.light_blue_900,null))
//    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setButton(
        selectedButton: TextView,
        nonSelectedButton1: TextView,
        nonSelectedButton2: TextView,
        nonSelectedButton3: TextView,
    ) {
        selectedButton.background = resources.getDrawable(R.drawable.switch_trcks,null)
        selectedButton.setTextColor(resources.getColor(R.color.textColor,null))
        nonSelectedButton1.background = null
        nonSelectedButton1.setTextColor(resources.getColor(R.color.light_blue_900,null))
        nonSelectedButton2.background = null
        nonSelectedButton2.setTextColor(resources.getColor(R.color.light_blue_900,null))
        nonSelectedButton3.background = null
        nonSelectedButton3.setTextColor(resources.getColor(R.color.light_blue_900,null))
    }

//    @RequiresApi(Build.VERSION_CODES.M)
//    @SuppressLint("UseCompatLoadingForDrawables")
//    private fun setButton(selectedButton: TextView,
//                          button1: TextView,
//                          button2: TextView,
//                          button3: TextView,
//                          button4: TextView) {
//        selectedButton.background = resources.getDrawable(R.drawable.switch_trcks,null)
//        selectedButton.setTextColor(resources.getColor(R.color.textColor,null))
//        button1.background = null
//        button1.setTextColor(resources.getColor(R.color.light_blue_900,null))
//        button2.background = null
//        button2.setTextColor(resources.getColor(R.color.light_blue_900,null))
//        button3.background = null
//        button3.setTextColor(resources.getColor(R.color.light_blue_900,null))
//        button4.background = null
//        button4.setTextColor(resources.getColor(R.color.light_blue_900,null))
//    }

    private fun resetData() {
        dailyData.clear()
        weeklyData.clear()
        monthlyData.clear()
        yearlyData.clear()
        totalData.clear()

        dailyLabel.clear()
        weeklyLabel.clear()
        monthlyLabel.clear()
        yearlyLabel.clear()
        totalLabel.clear()
    }

}



//        val aaChartModel : AAChartModel = AAChartModel()
//            .chartType(AAChartType.Area)
//            .title("title")
//            .subtitle("subtitle")
//            .backgroundColor("#4b2b7f")
//            .dataLabelsEnabled(true)
//            .series(arrayOf(
//                AASeriesElement()
//                    .name("Tokyo")
//                    .data(arrayOf(7.0, 6.9, 9.5, 14.5, 18.2, 21.5, 25.2, 26.5, 23.3, 18.3, 13.9, 9.6)),
//                AASeriesElement()
//                    .name("NewYork")
//                    .data(arrayOf(0.2, 0.8, 5.7, 11.3, 17.0, 22.0, 24.8, 24.1, 20.1, 14.1, 8.6, 2.5)),
//                AASeriesElement()
//                    .name("London")
//                    .data(arrayOf(0.9, 0.6, 3.5, 8.4, 13.5, 17.0, 18.6, 17.9, 14.3, 9.0, 3.9, 1.0)),
//                AASeriesElement()
//                    .name("Berlin")
//                    .data(arrayOf(3.9, 4.2, 5.7, 8.5, 11.9, 15.2, 17.0, 16.6, 14.2, 10.3, 6.6, 4.8))
//            ))