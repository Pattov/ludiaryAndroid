package com.ludiary.android.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.entity.SessionEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * Fragment principal del Dashboard de la aplicación Ludiary.
 *
 * Actúa como la pantalla inicial tras el inicio de sesión, mostrando un resumen general de la
 * actividad lúdica del usuario.
 */
class DashboardFragment : Fragment (R.layout.fragment_dashboard) {

    private var selectedIndex: Int = -1
    private val weekDayKeys = mutableListOf<Long>()

    /**
     * Metodo del ciclo de vida del fragment.
     * Se ejecuta cuando la vista ya ha sido creada y asociada al layout.
     *
     * @param view Vista raíz del fragment.
     * @param savedInstanceState Esta guardado.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val weekContainer = view.findViewById<LinearLayout>(R.id.weekContainer)
        weekContainer.removeAllViews()

        val locale = Locale.getDefault()

        val now = Calendar.getInstance(locale)

        val startOfWeek = (now.clone() as Calendar).apply {
            firstDayOfWeek = now.firstDayOfWeek
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val diff = (7 + (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek)) % 7
            add(Calendar.DAY_OF_MONTH, -diff)
        }

        val dowFormat = java.text.SimpleDateFormat("EEEEE", locale) // 1 letra
        val dayFormat = java.text.SimpleDateFormat("d", locale)     // número día

        var todayIndex = 0

        for (i in 0 until 7) {
            val dayCal = (startOfWeek.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }

            val dayKey = dayStartMillis(dayCal)
            weekDayKeys.add(dayKey)

            val dayView = layoutInflater.inflate(R.layout.item_dashboard_day, weekContainer, false)

            val chip = dayView.findViewById<View>(R.id.dayChip)
            val tvDow = dayView.findViewById<TextView>(R.id.tvDow)
            val tvDay = dayView.findViewById<TextView>(R.id.tvDay)
            val dot = dayView.findViewById<View>(R.id.dot)

            tvDow.text = dowFormat.format(dayCal.time)
            tvDay.text = dayFormat.format(dayCal.time)

            dot.visibility = View.GONE

            if (sameDay(dayCal, now)) todayIndex = i

            chip.setOnClickListener {
                setSelectedDay(weekContainer, i)
            }

            weekContainer.addView(dayView)
        }

        setSelectedDay(weekContainer, todayIndex)

        val db = LudiaryDatabase.getInstance(requireContext().applicationContext)

        val grid = view.findViewById<GridLayout>(R.id.gridKpis)
        val kpi0 = grid.getChildAt(0) // Juegos
        val kpi1 = grid.getChildAt(1) // Partidas
        val kpi2 = grid.getChildAt(2) // Media
        val kpi3 = grid.getChildAt(3) // Horas

        fun setKpi(card: View, labelRes: Int, value: String, iconRes: Int) {
            card.findViewById<TextView>(R.id.tvKpiLabel).setText(labelRes)
            card.findViewById<TextView>(R.id.tvKpiValue).text = value
            card.findViewById<ImageView>(R.id.ivKpiIcon).setImageResource(iconRes)
        }

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerRecentSessions)
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val recentAdapter = DashboardRecentSessionsAdapter()
        recycler.adapter = recentAdapter

        val chart = view.findViewById<BarChart>(R.id.chartWeeklySessions)
        setupWeeklyChart(chart)

        viewLifecycleOwner.lifecycleScope.launch {
            db.sessionDao().observeAllActiveSessions().collect { sessions ->
                applyDots(weekContainer, sessions)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.userGameDao().observeUserGamesCount().collect { count ->
                setKpi(kpi0, R.string.dashboard_kpi_games, count.toString(), R.drawable.ic_library_filled)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.sessionDao().observeSessionsCount().collect { count ->
                setKpi(kpi1, R.string.dashboard_kpi_sessions, count.toString(), R.drawable.ic_sessions_filled)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.sessionDao().observeAvgRating().collect { avg ->
                val txt = if (avg == null) "—" else String.format(Locale.getDefault(), "%.1f", avg)
                setKpi(kpi2, R.string.dashboard_kpi_avg_rating, txt, R.drawable.ic_dashboard_filled)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.sessionDao().observeTotalMinutes().collect { minutes ->
                val total = minutes ?: 0
                val hours = total / 60
                setKpi(kpi3, R.string.dashboard_kpi_hours, getString(R.string.dashboard_hours_format, hours), R.drawable.ic_sync)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.sessionDao().observeRecentSessions(3).collect { list ->
                recentAdapter.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val weekRange = computeWeekRangeMillis()
            val playedAtList = withContext(Dispatchers.IO) {
                db.sessionDao().getPlayedAtBetween(weekRange.first, weekRange.second)
            }
            renderWeeklySessionsChart(chart, playedAtList)
        }
    }

    private fun dayStartMillis(cal: Calendar):Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun computeWeekRangeMillis(): Pair<Long, Long> {
        val locale = Locale.getDefault()
        val now = Calendar.getInstance(locale)

        val start = (now.clone() as Calendar).apply {
            firstDayOfWeek = now.firstDayOfWeek
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val diff = (7 + (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek)) % 7
            add(Calendar.DAY_OF_MONTH, -diff)
        }

        val end = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 7)
            add(Calendar.MILLISECOND, -1)
        }

        return start.timeInMillis to end.timeInMillis
    }

    private fun setupWeeklyChart(chart: BarChart) {
        chart.description.isEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)

        chart.axisRight.isEnabled = false

        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.granularity = 1f

        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        chart.legend.isEnabled = false
    }

    private fun renderWeeklySessionsChart(chart: BarChart, playedAtList: List<Long>) {
        val locale = Locale.getDefault()

        val now = Calendar.getInstance(locale)
        val start = (now.clone() as Calendar).apply {
            firstDayOfWeek = now.firstDayOfWeek
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val diff = (7 + (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek)) % 7
            add(Calendar.DAY_OF_MONTH, -diff)
        }

        val counts = IntArray(7)

        for (millis in playedAtList) {
            val d = Calendar.getInstance(locale).apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffDays = ((d.timeInMillis - start.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
            if (diffDays in 0..6) counts[diffDays]++
        }

        val entries = (0..6).map { i -> BarEntry(i.toFloat(), counts[i].toFloat()) }

        val dataSet = BarDataSet(entries, "Sesiones")
        dataSet.setDrawValues(true)

        val data = BarData(dataSet)
        data.barWidth = 0.7f

        chart.data = data

        // Etiquetas de X: L M X J V S D según locale
        val dowFormat = java.text.SimpleDateFormat("EEEEE", locale)
        val labels = (0..6).map { i ->
            val c = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            dowFormat.format(c.time)
        }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return labels.getOrNull(idx) ?: ""
            }
        }

        chart.invalidate()
    }


    private fun millisToDayStart(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun applyDots(container: LinearLayout, sessions: List<SessionEntity>) {
        if (weekDayKeys.size != 7) return

        val daysWithSessions = sessions
            .map { millisToDayStart(it.playedAt) }
            .toSet()

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val dot = child.findViewById<View>(R.id.dot)
            val dayKey = weekDayKeys.getOrNull(i)
            dot.visibility = if (dayKey != null && dayKey in daysWithSessions) View.VISIBLE else View.GONE
        }
    }

    private fun setSelectedDay(container: LinearLayout, index: Int) {
        if (selectedIndex in 0 until container.childCount) {
            val prev = container.getChildAt(selectedIndex)
            prev.findViewById<View>(R.id.dayChip)
                .setBackgroundResource(R.drawable.bg_dashboard_day_unselected)
            prev.findViewById<TextView>(R.id.tvDow).setTextColor(
                prev.context.getColor(android.R.color.black)
            )
            prev.findViewById<TextView>(R.id.tvDay).setTextColor(
                prev.context.getColor(android.R.color.black)
            )
        }

        // marcar nuevo
        val cur = container.getChildAt(index)
        cur.findViewById<View>(R.id.dayChip)
            .setBackgroundResource(R.drawable.bg_dashboard_day_selected)
        cur.findViewById<TextView>(R.id.tvDow).setTextColor(
            cur.context.getColor(android.R.color.white)
        )
        cur.findViewById<TextView>(R.id.tvDay).setTextColor(
            cur.context.getColor(android.R.color.white)
        )

        selectedIndex = index
    }
}