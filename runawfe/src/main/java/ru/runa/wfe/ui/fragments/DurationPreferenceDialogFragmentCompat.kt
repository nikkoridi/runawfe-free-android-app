package ru.runa.wfe.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import androidx.preference.PreferenceDialogFragmentCompat
import ru.runa.wfe.R
import ru.runa.wfe.ui.notification.DurationPreference

class DurationPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    private lateinit var timePicker: TimePicker
    private val preference: DurationPreference by lazy { getPreference() as DurationPreference }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        timePicker = view.findViewById(R.id.durationPicker) ?: error("Can't find TimePicker in dialog with id ''")
        val savedDurationMinutes = preference.duration
        val hours = savedDurationMinutes / 60
        val minutes = savedDurationMinutes % 60
        timePicker.apply {
            setIs24HourView(true) // TODO: It's designed to be a duration picker, am/pm format is questionable
            hour = hours
            minute = minutes
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val inputDurationMinutes = timePicker.hour * 60 + timePicker.minute
            preference.apply {
                if (callChangeListener(inputDurationMinutes)) {
                    duration = inputDurationMinutes
                }
            }
        }
        else {
            Toast.makeText(context,
                this.getString(R.string.settings_empty_value_message),
                Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
    fun newInstance(key: String?) = DurationPreferenceDialogFragmentCompat().apply {
            arguments = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
        }
    }
}