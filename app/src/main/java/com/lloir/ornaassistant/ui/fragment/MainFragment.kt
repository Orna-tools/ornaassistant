package com.lloir.ornaassistant.ui.fragment

import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lloir.ornaassistant.DungeonVisit
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.db.DungeonVisitDatabaseHelper
import java.time.LocalDate

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MainFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mDb: DungeonVisitDatabaseHelper
    private lateinit var mSharedPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        mDb = DungeonVisitDatabaseHelper(requireContext())
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_main, container, false)
        if (::mDb.isInitialized) {
            drawWeeklyChart(view)
        }
        return view
    }

    class WeekAxisFormatter(private val startDay: Int) : ValueFormatter() {private val days = arrayOf("Mo", "Tu", "Wed", "Th", "Fr", "Sa", "Su")
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            var index = startDay - 1 + value.toInt()
            if (index > 6)
                index -= 7
            return days.getOrNull(index) ?: value.toString()
        }
    }

    class IntegerFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return value.toInt().toString()
        }

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return value.toInt().toString()
        }
    }

    fun drawWeeklyChart(view: View? = this.view) {
        val chart: BarChart = view?.findViewById(R.id.cWeeklyDungeons) as BarChart

        val eDung = mutableListOf<BarEntry>()
        val eFailedDung = mutableListOf<BarEntry>()
        val eOrns = mutableListOf<BarEntry>()

        val startOfToday = LocalDate.now().atStartOfDay()
        val startDay = startOfToday.minusDays(6).dayOfWeek.value
        for (i in 6 downTo 0) {
            val entries = mDb.getEntriesBetween(
                startOfToday.minusDays(i.toLong()),
                startOfToday.minusDays((i - 1).toLong())
            )

            val completed = entries.filter { it.completed } as ArrayList<DungeonVisit>
            val failed = entries.filter { !it.completed } as ArrayList<DungeonVisit>

            eDung.add(BarEntry(i.toFloat(), completed.size.toFloat()))
            eFailedDung.add(BarEntry(i.toFloat(), failed.size.toFloat()))
            var orns = 0f
            entries.forEach {
                orns += it.orns
            }
            orns /= 1000000
            eOrns.add(BarEntry(i.toFloat(), orns))
        }

        var textColor = Color.BLACK
        if (requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            textColor = Color.LTGRAY
        }
        val sDung = BarDataSet(eDung, "Dungeons")
        val sFailedDung = BarDataSet(eFailedDung, "Failed dungeons")
        val sOrns = BarDataSet(eOrns, "Orns gained (mil)")
        sDung.valueFormatter = IntegerFormatter()
        sFailedDung.valueFormatter = IntegerFormatter()
        sDung.valueTextSize = 12f
        sFailedDung.valueTextSize = 12f
        sOrns.valueTextSize = 12f
        sDung.color = Color.parseColor("#ff6d00")
        sFailedDung.color = Color.parseColor("#c62828")
        sOrns.color = Color.parseColor("#558b2f")

        sDung.valueTextColor = textColor
        sFailedDung.valueTextColor = textColor
        sOrns.valueTextColor = textColor
        val data = BarData(sDung, sFailedDung, sOrns)

        val groupSpace = 0.06f
        val barSpace = 0.02f // x2 dataset

        val barWidth = 0.29f
        data.barWidth = barWidth

        chart.data = data
        chart.xAxis.valueFormatter = WeekAxisFormatter(startDay)
        chart.xAxis.textSize = 10f
        chart.xAxis.textColor = textColor
        chart.xAxis.position = XAxis.XAxisPosition.BOTH_SIDED
        chart.groupBars(0F, groupSpace, barSpace)
        chart.xAxis.axisMaximum = 7f
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.setCenterAxisLabels(true)
        chart.xAxis.setDrawGridLines(false)
        chart.description.isEnabled = false

        chart.axisLeft.textColor = textColor
        chart.axisRight.textColor = textColor
        chart.legend.textColor = textColor
        chart.invalidate()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}