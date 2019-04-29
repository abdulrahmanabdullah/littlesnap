package com.abdulrahman.littlesnap.utlities

import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.Toast


fun FragmentActivity.showToast(message:String){
    runOnUiThread { Toast.makeText(this,message,Toast.LENGTH_LONG).show()}
}

fun View.showSnackBar(message: String,duration:Int){
    Snackbar.make(this,message,duration).show()
}