package com.friday.mistakenotebook.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.reminder.ReminderNotifier
import com.friday.mistakenotebook.data.reminder.ReminderPrefs
import com.friday.mistakenotebook.data.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderSettingsUiState(
    val enabled: Boolean = false,
    val hour: Int = 19,
    val minute: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val enabledFlow = ReminderPrefs.enabledFlow(context)
    private val timeFlow = ReminderPrefs.timeFlow(context)

    val reminderSettings: StateFlow<ReminderSettingsUiState> = timeFlow
        .combine(enabledFlow) { (hour, minute), enabled ->
            ReminderSettingsUiState(enabled = enabled, hour = hour, minute = minute)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettingsUiState())

    /** 开关：开启时按已保存的时间调度，关闭即取消 */
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val state = reminderSettings.value
            ReminderPrefs.setEnabled(context, enabled)
            if (enabled) {
                ReminderNotifier.ensureChannel(context)
                ReminderScheduler.schedule(context, state.hour, state.minute)
            } else {
                ReminderScheduler.cancel(context)
            }
        }
    }

    /** 修改提醒时间：保存并按需重新调度 */
    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            ReminderPrefs.setTime(context, hour, minute)
            if (reminderSettings.value.enabled) {
                ReminderScheduler.schedule(context, hour, minute)
            }
        }
    }
}
