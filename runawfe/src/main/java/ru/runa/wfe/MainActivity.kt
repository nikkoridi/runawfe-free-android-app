package ru.runa.wfe

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.savedstate.SavedState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.notification.NotificationService
import ru.runa.wfe.rest.TokenManager
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.ServerCheckResult
import ru.runa.wfe.ui.notification.PermissionsConstants

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var navController: NavController

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val permission = intent.getStringExtra("permission")
            requestPermission(permission!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerReceiver(
            permissionReceiver,
            IntentFilter(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_NOT_EXPORTED else 0
        )

        preferencesManager = PreferencesManager(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(
            object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: SavedState?
                ) {
                    if (destination.id == R.id.mainFragment) {
                        startNotificationService()
                        controller.removeOnDestinationChangedListener(this)
                    }
                }
            }
        )
        loadDataAndNavigate()
    }

    private fun loadDataAndNavigate() {
        lifecycleScope.launch {
            val wfURL = preferencesManager
                .getValue(PreferencesManager.WEBVIEW_URL, "")

            if (wfURL.isEmpty()) {
                navController.navigate(
                    R.id.settingsFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.loginFragment, inclusive = false).build()
                )
            } else {
                val checkServerUrlResult = ApiClient.checkServer(wfURL)
                if (checkServerUrlResult is ServerCheckResult.Valid) {
                    ApiClient.setServerUrl(checkServerUrlResult)
                    val tokenLoadSuccess = TokenManager.loadToken(preferencesManager)
                    preferencesManager.setKey(PreferencesManager.IS_LOGGED, tokenLoadSuccess)
                    if (tokenLoadSuccess) {
                        navController.popBackStack() // Don't return to login form by pressing back
                        navController.navigate(R.id.mainFragment)
                    } else {
                        preferencesManager.deleteKeyValue(PreferencesManager.TOKEN)
                    }
                }
                else {
                    navController.popBackStack()
                    navController.navigate(R.id.mainFragment)
                }
            }
        }
    }

    private fun startNotificationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val checkDelay: Int = preferencesManager.getValue(
                PreferencesManager.POLLING_INTERVAL,
                0
            )
            if (checkDelay != 0 && ApiClient.isApiClientInitialized()) {
                startForegroundService(Intent(this, NotificationService::class.java))
            }
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
                    ActivityCompat.requestPermissions(
                        this, arrayOf(permission),
                        REQUEST_CODE_PERMISSION
                    )
                }
                .create().show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(permission),
                REQUEST_CODE_PERMISSION
            )
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
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                R.string.permission_given,
                Toast.LENGTH_LONG
            ).show()
            startForegroundService(Intent(this, NotificationService::class.java))
        } else {
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
        stopService(Intent(this, NotificationService::class.java))
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 100
    }
}