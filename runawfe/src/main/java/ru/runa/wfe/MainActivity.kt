@file:Suppress("DEPRECATION")
package ru.runa.wfe

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import ru.runa.wfe.notification.NotificationService
import ru.runa.wfe.ui.notification.PermissionsConstants

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var prefs: SharedPreferences

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val permission = intent.getStringExtra("permission")
            requestPermission(permission!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerReceiver(permissionReceiver,
            IntentFilter(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName),
            Context.RECEIVER_NOT_EXPORTED)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        as NavHostFragment
        navController = navHostFragment.navController
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val wfURL = prefs.getString("urlQuery", "").toString()
        val isLoggedUser = prefs.getBoolean("isLogged", false)
        if (wfURL.isEmpty()) {
            navController.navigate(R.id.loginFragment)
            navController.navigate(R.id.login_to_settings)
        }
        else if (!isLoggedUser) {
            navController.navigate(R.id.loginFragment)
        }
        else {
            navController.navigate(R.id.mainFragment)
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.mainFragment -> {
                    startNotificationService()
                }
            }
        }
    }

    private fun startNotificationService() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notificationServiceIntent = Intent(this, NotificationService::class.java)
            startForegroundService(notificationServiceIntent)
        }
        else {
            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestPermission(permission: String) {
        // Google's tutorial recommend to create an own dialog with explanations
        // It actually is a duplicate of the System dialog box, except the explanations
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_request_title)
                .setMessage(R.string.permission_notification_need)
                .setPositiveButton(R.string.permission_set) { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(permission),
                        REQUEST_CODE_PERMISSION)
                }
                .create().show()
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(permission),
                REQUEST_CODE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,
                R.string.permission_given,
                Toast.LENGTH_LONG).show()
            NotificationService
        }
        else {
            Snackbar
                .make(findViewById(android.R.id.content), R.string.permission_error, 40000)
                .setAction(R.string.permission_snackbar_set) {
                    requestPermission(permissions[0]) // TODO: how to ask permission twice
                }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(permissionReceiver)
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 100
    }
}