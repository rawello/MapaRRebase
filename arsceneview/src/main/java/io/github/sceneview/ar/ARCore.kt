package io.github.sceneview.ar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import io.github.sceneview.ar.arcore.ARSession

const val kDefaultHitTestInstantDistance = 2.0f

class ARCore(
    val onSessionCreated: (session: Session) -> Unit,
    val onSessionResumed: (session: Session) -> Unit,
    val onSessionPaused: (session: Session) -> Unit,
    val onArSessionFailed: (exception: Exception) -> Unit,
    val onSessionConfigChanged: (session: Session, config: Config) -> Unit
) {

    var checkCameraPermission = true
    var checkAvailability = true

    lateinit var features: Set<Session.Feature>
    lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var cameraPermissionRequested = false
    lateinit var appSettingsLauncher: ActivityResultLauncher<Intent>

    private var appSettingsRequested = false
    private var installRequested = false
    internal var session: ARSession? = null
        private set

    fun create(context: Context, activity: ComponentActivity?, features: Set<Session.Feature>) {
        this.features = features
        if (activity != null) {
            cameraPermissionLauncher = activity.activityResultRegistry.register(
                "sceneview_camera_permission",
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.CAMERA
                        )
                    ) {
                        appSettingsRequested = true
                        showCameraPermissionSettings(activity)
                    }
                }
            }
            appSettingsLauncher = activity.activityResultRegistry.register(
                "sceneview_app_settings",
                ActivityResultContracts.StartActivityForResult()
            ) {
                appSettingsRequested = false
            }
            if (checkPermissionAndInstall(activity)) {
                createSession(context)
            }
        } else {
            createSession(context)
        }
    }

    fun resume(context: Context, activity: ComponentActivity?) {
        if (session == null) {
            if (activity == null || checkPermissionAndInstall(activity)) {
                createSession(context)
            }
        }
        session?.resume()
    }

    fun pause() {
        session?.pause()
    }

    fun createSession(context: Context) {
        try {
            session = ARSession(
                context,
                features,
                onResumed = onSessionResumed,
                onPaused = onSessionPaused,
                onConfigChanged = onSessionConfigChanged
            ).also(onSessionCreated)
        } catch (exception: Exception) {
            onException(exception)
        }
    }

    fun checkPermissionAndInstall(activity: ComponentActivity): Boolean {
        if (checkCameraPermission && !cameraPermissionRequested &&
            !checkCameraPermission(activity, cameraPermissionLauncher)
        ) {
            cameraPermissionRequested = true
        } else if (!appSettingsRequested) {
            try {
                if (checkAvailability && !installRequested &&
                    !checkInstall(activity, installRequested)
                ) {
                    installRequested = true
                } else {
                    return true
                }
            } catch (e: Exception) {
                onException(e)
            }
        }
        return false
    }

    fun destroy() {
        session?.let {
            synchronized(it) {
                if (session == null) return@synchronized
                it.close()
                session = null
            }
        }
    }

    fun onException(exception: Exception) {
        onArSessionFailed(exception)
    }

    fun hasCameraPermission(context: Context) = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    fun checkCameraPermission(
        context: Context,
        permissionLauncher: ActivityResultLauncher<String>
    ): Boolean {
        return if (!hasCameraPermission(context)) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            false
        } else {
            true
        }
    }

    fun showCameraPermissionSettings(activity: ComponentActivity) {
        Toast.makeText(
            activity,
            "Требуется разрешение на использование камеры",
            Toast.LENGTH_LONG
        ).show()
        appSettingsLauncher.launch(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", activity.packageName, null)
        })
    }

    fun checkInstall(activity: ComponentActivity, installRequested: Boolean): Boolean {
        return isInstalled(activity) || !install(activity, installRequested)
    }

    fun isInstalled(context: Context) =
        ArCoreApk.getInstance().checkAvailability(context) == Availability.SUPPORTED_INSTALLED

    fun canBeInstalled(context: Context): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        return availability == Availability.SUPPORTED_APK_TOO_OLD || availability == Availability.SUPPORTED_NOT_INSTALLED
    }

    fun install(activity: ComponentActivity, installRequested: Boolean): Boolean {
        if (!canBeInstalled(activity)) {
            throw UnavailableDeviceNotCompatibleException()
        }
        return ArCoreApk.getInstance().requestInstall(
            activity,
            !installRequested
        ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
    }
}

fun TrackingFailureReason.getDescription(context: Context) = when (this) {
    TrackingFailureReason.NONE -> ""
    TrackingFailureReason.BAD_STATE -> "Неверное состояние"
    TrackingFailureReason.INSUFFICIENT_LIGHT -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        "Недостаточно света"
    } else {
        "Недостаточно света (Android S)"
    }

    TrackingFailureReason.EXCESSIVE_MOTION -> "Слишком много движения"
    TrackingFailureReason.INSUFFICIENT_FEATURES -> "Недостаточно особенностей"
    TrackingFailureReason.CAMERA_UNAVAILABLE -> "Камера недоступна"
    else -> "Неизвестная ошибка отслеживания: $this"
}
