package ru.runa.wfe.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class DurationPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {
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
    private val maxValue = 3_600
    private val minValue = 0

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        preferencesManager = PreferencesManager.getInstance(view.context)

        durationValue = view.findViewById(R.id.durationPicker)
        durationValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    return
                }

                val inputValue = s.toString().toLong()
                if (inputValue < minValue) {
                    durationValue.setText("$minValue")
                    durationValue.setSelection(durationValue.text.length)
                } else if (inputValue > maxValue) {
                    durationValue.setText("$maxValue")
                    durationValue.setSelection(durationValue.text.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        timeUnitPicker = view.findViewById(R.id.durationTimeUnit)

        val pollingIntervalSeconds = preferencesManager.getValue(
            PreferencesManager.POLLING_INTERVAL,
            DurationPreference.DEFAULT
        )
        timeUnit = getTimeUnit(pollingIntervalSeconds)

        val durationSavedValue: Long = when (timeUnit) {
            TimeUnit.HOURS -> pollingIntervalSeconds / 3_600
            TimeUnit.MINUTES -> pollingIntervalSeconds / 60
            else -> pollingIntervalSeconds
        }
        durationValue.setText("$durationSavedValue")

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
            val inputValue =
                (durationValue.text.toString().toLong()).toDuration(timeUnit.toDurationUnit())
            val durationSeconds = inputValue.inWholeSeconds
            preference.apply {
                if (callChangeListener(durationSeconds)) {
                    duration = durationSeconds
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

        fun getTimeUnit(duration: Long): TimeUnit {
            return if (duration % 60 == 0L) {
                if (duration % 3_600 == 0L) {
                    TimeUnit.HOURS
                } else {
                    TimeUnit.MINUTES
                }
            } else {
                TimeUnit.SECONDS
            }
        }
    }
}