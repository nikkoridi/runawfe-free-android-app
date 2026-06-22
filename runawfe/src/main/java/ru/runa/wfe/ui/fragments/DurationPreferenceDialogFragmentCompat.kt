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
import ru.runa.wfe.ui.notification.TimeUnits

class DurationPreferenceDialogFragmentCompat: PreferenceDialogFragmentCompat() {
    private lateinit var durationValue: EditText
    private lateinit var timeUnitPicker: Spinner
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var timeUnit: TimeUnits
    private val preference: DurationPreference by lazy { getPreference() as DurationPreference }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        preferencesManager = PreferencesManager(view.context)

        durationValue = view.findViewById(R.id.durationPicker)
        timeUnitPicker = view.findViewById(R.id.durationTimeUnit)

        val pollingIntervalSeconds = preferencesManager.getValue(
            PreferencesManager.POLLING_INTERVAL,
            DurationPreference.DEFAULT
        ) / 1000
        val durationSavedValue: Int
        if (pollingIntervalSeconds % 60 == 0) {
            if (pollingIntervalSeconds % 3_600 == 0) {
                timeUnit = TimeUnits.HOURS
                durationSavedValue = pollingIntervalSeconds / 3_600
            } else {
                timeUnit = TimeUnits.MINUTES
                durationSavedValue = pollingIntervalSeconds / 60
            }
        } else {
            timeUnit = TimeUnits.SECONDS
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
            TimeUnits.entries.indexOf(timeUnit)
        )

        timeUnitPicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                timeUnit = TimeUnits.entries.getOrElse(pos) { TimeUnits.SECONDS }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val inputValueMilliseconds = durationValue.text.toString().toInt()  * 1_000
            val durationMilliseconds = when(timeUnit) {
                TimeUnits.SECONDS ->  inputValueMilliseconds
                TimeUnits.MINUTES -> inputValueMilliseconds * 60
                TimeUnits.HOURS -> inputValueMilliseconds * 3_600
            }
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