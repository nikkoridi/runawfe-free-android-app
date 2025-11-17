package ru.runa.wfe.ui.notification

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ru.runa.wfe.R

class NotificationPermissionActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE = 0
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                Toast.makeText(
                    this,
                    this.getString(R.string.permission_error),
                    Toast.LENGTH_LONG
                ).show()
            }
            else {
                Toast.makeText(
                    this,
                    this.getString(R.string.permission_given),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        finish()
    }
}