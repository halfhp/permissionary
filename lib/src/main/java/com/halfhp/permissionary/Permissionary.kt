package com.halfhp.permissionary

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.annotation.StringRes
import android.support.v4.app.*
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog

import io.reactivex.Single

class Permissionary(private val activity: Activity) {

    constructor(fragment: Fragment) : this(fragment.activity) {
        this.fragment = fragment
    }

    private var fragment: Fragment? = null

    private var permissionsCallback: PermissionsCallback? = null

    fun requestPermission(request: PermissionsRequest): Single<Boolean> {
        permissionsCallback?.onPermissionsDenied()

        return Single.create<Boolean> { single ->
            if (!hasPermissions(*request.permissions)) {
                permissionsCallback = object : PermissionsCallback(request) {

                    override fun onPermissionsGranted() {
                        if (!single.isDisposed) {
                            single.onSuccess(true)
                        }
                    }

                    override fun onPermissionsDenied() {
                        if (!single.isDisposed) {
                            single.onSuccess(false)
                        }
                    }
                }
                promptForPermission()
            } else {
                // we've got permission!
                single.onSuccess(true)
            }
        }
    }

    private fun promptForPermission() {
        permissionsCallback?.let { callback ->
            val thisFrag = fragment
            if (thisFrag != null) {
                thisFrag.requestPermissions(
                        callback.permissionsRequest.permissions, callback.requestCode)
            } else {
                ActivityCompat.requestPermissions(activity,
                        callback.permissionsRequest.permissions, callback.requestCode)
            }
        }
    }

    /**
     * Display a dialog rationalizing the app's need for the specified permission(s).
     * @param permissionsRequest The permission(s) being requested.
     * @param isNeverAskAgain If true, a button shortcut to opening app settings will be included.
     * * Otherwise, a retry button is displayed.
     */
    private fun showPermissionRationale(permissionsRequest: PermissionsRequest, isNeverAskAgain: Boolean) {

        @StringRes val title = if (isNeverAskAgain) {
            R.string.pmny_perm_denied_dlg_title_default
        } else {
            R.string.pmny_perm_denied_dlg_title_confirm_default
        }

        @StringRes val positiveButtonLabel = if (isNeverAskAgain) {
            R.string.pmny_open_settings
        } else {
            R.string.pmny_allow
        }

        @StringRes val negativeButtonLabel = if (isNeverAskAgain) {
            R.string.pmny_dismiss
        } else {
            R.string.pmny_deny
        }

        val positiveClickListener = DialogInterface.OnClickListener { _, _ ->
            permissionsCallback?.let { callback ->
                if (isNeverAskAgain) {
                    callback.onPermissionsDenied()
                    showSystemSettings()
                } else {
                    promptForPermission()
                }
            }
        }

        val negativeClickListener = DialogInterface.OnClickListener { _, _ ->
            permissionsCallback?.onPermissionsDenied()
        }

        showYesNoDlg(activity, title, permissionsRequest.rationale, positiveButtonLabel, positiveClickListener,
                negativeButtonLabel, negativeClickListener)
    }

    private fun showSystemSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    /**
     * Check permission request results to see whether the request was granted.
     * or denied.  Activities using PermissionsHelper MUST call through to this method.
     * from their own handlePermissionResults implementation.
     */
    fun handlePermissionResults(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsCallback?.let { callback ->
            if (requestCode != callback.requestCode) return

            val result = PermissionsResult(callback.permissionsRequest, permissions, grantResults)
            if (!result.isAllGranted()) {
                showPermissionRationale(callback.permissionsRequest, result.isNeverAskAgain(activity))
            } else {
                callback.onPermissionsGranted()
            }
        }
    }

    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !hasPermissions(permission) && !ActivityCompat
                .shouldShowRequestPermissionRationale(activity, permission)
    }

    fun hasPermissions(vararg permissions: String): Boolean {
        return permissions.none { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }
    }
}

/**
 * @param rationale Message to be displayed if {@link PermissionHelper} determines that a permission
 * rationale should be shown to the user.
 * @param permissions One or more permissions to be be requested
 */
class PermissionsRequest(val rationale: String, vararg permissions: String) {

    val permissions: Array<out String> = permissions
}

abstract class PermissionsCallback(val permissionsRequest: PermissionsRequest, val requestCode: Int = 3432) {

    abstract fun onPermissionsGranted()

    abstract fun onPermissionsDenied()
}

class PermissionResult(val permission: String, val resultCode: Int)

class PermissionsResult(val request: PermissionsRequest, permissions: Array<out String>, resultCodes: IntArray) {

    val results = List(permissions.size, { i ->
        val permission = request.permissions.filter {
            it.equals(permissions[i])
        }.first()
        PermissionResult(permission, resultCodes[i])
    })

    fun isAllGranted(): Boolean {
        return results.none { it.resultCode != PackageManager.PERMISSION_GRANTED }
    }

    fun isNeverAskAgain(activity: Activity): Boolean {
        return results.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it.permission) }
    }
}

fun showYesNoDlg(context: Context, @StringRes title: Int,
                    message: CharSequence, @StringRes positiveText: Int,
                    positiveCallback: DialogInterface.OnClickListener?,
                    @StringRes negativeText: Int, negativeCallback: DialogInterface.OnClickListener?): AlertDialog {

    val builder = AlertDialog.Builder(context)
            .setMessage(message)
            .setTitle(title)
            .setPositiveButton(positiveText, positiveCallback)

    if (negativeText != 0) {
        builder.setNegativeButton(negativeText, negativeCallback)
    }

    val dialog = builder.show()
    return dialog
}