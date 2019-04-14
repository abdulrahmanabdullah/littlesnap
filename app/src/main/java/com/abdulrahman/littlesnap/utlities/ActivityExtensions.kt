package com.abdulrahman.littlesnap.utlities

import android.support.v4.app.FragmentActivity
import android.widget.Toast


fun FragmentActivity.showToast(message:String){
    runOnUiThread { Toast.makeText(this,message,Toast.LENGTH_LONG).show()}
}