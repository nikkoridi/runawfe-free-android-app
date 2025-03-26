@file:Suppress("DEPRECATION")

package ru.runa.wfe

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.app.DialogFragment;

class EmptyURLDialogFragment : DialogFragment() {
    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Введите адрес")
            builder.setMessage("Адресная строка пустая")
                .setPositiveButton("ОК") {
                        dialog, id ->  dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}