package com.abdulrahman.littlesnap

import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.View
import com.abdulrahman.littlesnap.utlities.PERMISSIONS
import com.abdulrahman.littlesnap.utlities.REQUEST_CAMERA_PERMISSIONS
import com.abdulrahman.littlesnap.utlities.showToast

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    //Set full screen view
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        var decoreView: View = window.decorView
        if (hasFocus) {
            decoreView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        }
    }

    //Permissions region
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            if (!hasAllPermissionsGranted()) { //Some permissions denied
                //To check user choice deny = true OR don't ask again = false
                if (shouldShowRationale()) {
                    //todo create dialog between denied and accept permissions
                    requestCameraPermissions() // replace this line with new dialog
                    this.showToast("Hi from line 37")
                } else {
                    //todo create dialog
                    PermissionConfirmationDialog().show(supportFragmentManager, "TT")
                    this.showToast(" Some Permissions don't ask again")
                }
            } else {
                //todo startCamera2 fragment
                this.showToast("All permissions granted , onRequestPermissions ")
                startCamera2()
            }
        }
    }

    private fun shouldShowRationale(): Boolean {
        for (permission in PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.i("main", "Some permissions denied")
                return true
            }
        }
        return false
    }

    //When this method return true which mean all permissions has granted
    //Otherwise call requestCameraPermissions function
    private fun hasAllPermissionsGranted(): Boolean {
        for (permission in PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }


    //Call this function when app start up and user click deny permissions
    private fun requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CAMERA_PERMISSIONS)
    }
    //End region

    //todo : rename this function and replace it job .
    private fun init() {
        if (!hasAllPermissionsGranted()) {
            requestCameraPermissions()
            return
        } else {
            startCamera2()
        }
    }

    //Inflate Camera2Fragment
    private fun startCamera2() {
//        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, Camera2Fragment.newInstance())
//            .commit()
    }

    override fun onStart() {
        super.onStart()
        init()//To check permissions every startup
    }

    //Dialog appear when user choice don't ask me again ...
    class PermissionConfirmationDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(activity)
            .setMessage(R.string.rationale_ask)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                //todo Create beaut layout to notify user accept our permissions
                activity?.finish()
            }
            .create()
    }
}
