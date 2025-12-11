package ru.runa.wfe

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.content.Intent

class EmptyURLDialogFragment : DialogFragment() {
    var activityOfMessage: Activity? = null

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Введите адрес")
            builder.setMessage("Адресная строка пустая")
                .setPositiveButton("ОК") {
                        dialog, id ->
                    startActivity(Intent(activityOfMessage, SettingsActivity::class.java))
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}