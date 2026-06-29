package ru.runa.wfe.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.preference.PreferenceDialogFragmentCompat
import ru.runa.wfe.R
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.ui.notification.DurationPreference
import java.util.concurrent.TimeUnit
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

class DurationPreferenceDialogFragmentCompat: PreferenceDialogFragmentCompat() {
    private lateinit var durationValue: EditText
    private lateinit var timeUnitPicker: Spinner
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var timeUnit: TimeUnit
    private val preference: DurationPreference by lazy { getPreference() as DurationPreference }
    private val durationPickerTimeUnits: List<TimeUnit> = listOf(
        TimeUnit.SECONDS,
        TimeUnit.MINUTES,
        TimeUnit.HOURS
    )

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        preferencesManager = PreferencesManager(view.context)

        durationValue = view.findViewById(R.id.durationPicker)
        timeUnitPicker = view.findViewById(R.id.durationTimeUnit)

        val pollingIntervalSeconds = preferencesManager.getValue(
            PreferencesManager.POLLING_INTERVAL,
            DurationPreference.DEFAULT
        ) / 1000
        val durationSavedValue: Long
        if (pollingIntervalSeconds % 60 == 0L) {
            if (pollingIntervalSeconds % 3_600 == 0L) {
                timeUnit = TimeUnit.HOURS
                durationSavedValue = pollingIntervalSeconds / 3_600
            } else {
                timeUnit = TimeUnit.MINUTES
                durationSavedValue = pollingIntervalSeconds / 60
            }
        } else {
            timeUnit = TimeUnit.SECONDS
            durationSavedValue = pollingIntervalSeconds
        }
        durationValue.text = Editable.Factory.getInstance().newEditable(durationSavedValue.toString())

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_duration_units,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeUnitPicker.adapter = adapter
        }

        timeUnitPicker.setSelection(
            durationPickerTimeUnits.indexOf(timeUnit)
        )

        timeUnitPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                timeUnit = durationPickerTimeUnits.getOrNull(pos) ?: TimeUnit.SECONDS
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val inputValue = (durationValue.text.toString().toLong()).toDuration(timeUnit.toDurationUnit())
            val durationMilliseconds = inputValue.inWholeMilliseconds
            preference.apply {
                if (callChangeListener(durationMilliseconds)) {
                    duration = durationMilliseconds
                }
            }
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