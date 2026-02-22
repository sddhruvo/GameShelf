package com.gamevault.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.model.PlaySession
import com.gamevault.app.data.repository.GameRepository
import com.gamevault.app.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val startOfDay: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val startOfWeek: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val startOfMonth: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayPlaytime: StateFlow<Long> = repository.getTotalPlaytimeSince(startOfDay)
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val weekPlaytime: StateFlow<Long> = repository.getTotalPlaytimeSince(startOfWeek)
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val monthPlaytime: StateFlow<Long> = repository.getTotalPlaytimeSince(startOfMonth)
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val mostPlayedGames: StateFlow<List<Game>> = repository.getMostPlayedGames(10)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentSessions: StateFlow<List<PlaySession>> = repository.getRecentSessions(30)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weekSessions: StateFlow<List<PlaySession>> = repository.getSessionsSince(startOfWeek)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _overallStreak = MutableStateFlow(0)
    val overallStreak: StateFlow<Int> = _overallStreak

    init {
        viewModelScope.launch {
            val dates = repository.getDistinctPlayDates()
            _overallStreak.value = FormatUtils.calculateStreak(dates)
        }
    }

    // Daily playtime for the past 7 days
    val dailyPlaytimeData: StateFlow<List<Pair<String, Long>>> = repository.getSessionsSince(
        System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
    ).map { sessions ->
        val calendar = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Long>>()

        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)

            val dayLabel = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                .format(java.util.Date(dayStart))

            val totalMs = sessions
                .filter { it.startTime in dayStart until dayEnd }
                .sumOf { it.durationMs }

            result.add(dayLabel to totalMs)
        }

        result
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
