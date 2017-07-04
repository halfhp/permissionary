package com.halfhp.permissionary.demo

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.halfhp.permissionary.Permissionary
import com.halfhp.permissionary.PermissionsRequest
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var permissionary: Permissionary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionary = Permissionary(this)

        camera_perms_button.setOnClickListener({
            onCameraButtonClick()
        })
    }

    private fun onCameraButtonClick() {
        permissionary?.requestPermission(PermissionsRequest("Camera permission is needed to do stuff.", Manifest.permission.CAMERA))
                ?.subscribe({
                    Toast.makeText(this, "result: $it", Toast.LENGTH_SHORT).show()
                }, {
                    Toast.makeText(this, "error: $it", Toast.LENGTH_SHORT).show()
                })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionary?.handlePermissionResults(requestCode, permissions, grantResults)
    }
}
