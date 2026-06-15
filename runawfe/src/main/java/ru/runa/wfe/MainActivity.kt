package ru.runa.wfe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.notification.NotificationHelpers
import ru.runa.wfe.notification.NotificationHelpers.NotificationType
import ru.runa.wfe.notification.NotificationService
import ru.runa.wfe.rest.TokenManager
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.ServerCheckResult
import ru.runa.wfe.ui.notification.permissionsConstantsMap

class MainActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var navController: NavController
    private var firstRun: Boolean = false

    private var permissionCallback: ((Boolean) -> Unit)? = null
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionCallback?.invoke(isGranted)
            permissionCallback = null
        }
    private var intentCallback: ((Int) -> Unit)? = null
    private val intentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> intentCallback?.invoke(result.resultCode)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Are there no files in /data/data/{applicationId}? Then it's the very first app launch
        firstRun = this.filesDir.listFiles()?.isEmpty() ?: false
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
                        val sdkTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        if (firstRun && sdkTiramisu) {
                            requestPermission(Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
                                if (isGranted) startNotificationService()
                            }
                        } else {
                            startNotificationService()
                        }
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

    private fun canStartNotificationService(): Boolean {
        val channelsEnabled = NotificationHelpers.isChannelEnabled(NotificationType.TASK, this) ||
                NotificationHelpers.isChannelEnabled(NotificationType.MESSAGE, this)
        val permissionGranted = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
                this.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!(ApiClient.isApiClientInitialized() && channelsEnabled && permissionGranted)) {
            return false
        }
        if (preferencesManager.getValue( PreferencesManager.POLLING_INTERVAL, 0) <= 0) {
            return false
        }
        return true
    }

    fun startNotificationService() {
        if (canStartNotificationService()) {
            startForegroundService(Intent(this, NotificationService::class.java))
        }
    }

    fun requestPermission(permission: String, callback: ((Boolean) -> Unit)?) {
        // Create custom dialog with explanations
        val explainDialogBuilder = AlertDialog.Builder(this)
            .setTitle(R.string.permission_request_title)
            .setMessage(permissionsConstantsMap[permission]?.explanation ?: R.string.permission_need)
            .setNeutralButton(R.string.refuse_action, null)
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (firstRun || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                permissionCallback = callback
                explainDialogBuilder
                    .setPositiveButton(R.string.permission_set) { _, _ ->
                        requestPermissionLauncher.launch(permission)
                    }
                    .create().show()
            } else {
                intentCallback = {
                    // Because Android Settings app has no informative result, check the permission state again
                    callback?.invoke(
                        ContextCompat.checkSelfPermission(
                            this,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                explainDialogBuilder
                    .setPositiveButton(R.string.permission_set) { _, _ ->
                        val intent = Intent()
                            .setAction(permissionsConstantsMap[permission]?.settingsPage)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
                        intentLauncher.launch(intent)
                    }
                    .create().show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, NotificationService::class.java))
    }
}