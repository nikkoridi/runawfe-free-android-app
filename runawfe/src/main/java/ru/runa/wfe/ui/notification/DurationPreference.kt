package ru.runa.wfe.ui.notification

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import ru.runa.wfe.R

class DurationPreference(
    context: Context,
    attrs: AttributeSet?
) : DialogPreference(context, attrs) {
    var duration: Int = DEFAULT
        get() = field
        set(value) {
            field = value
            persistInt(value)
        }

    override fun getDialogLayoutResource(): Int {
        return R.layout.duration_picker_dialog
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Int {
        return a.getInt(index, DEFAULT)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        duration = getPersistedInt(defaultValue as? Int ?: DEFAULT)
    }

    companion object {
        const val DEFAULT = 3 // 3 min default value
    }
}