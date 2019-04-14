package com.abdulrahman.littlesnap.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R

class Camera2Fragment : BaseFragment() {


    override fun getLayoutResId(): Int {
        return R.layout.fragment_camera2
    }

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {

    }


    companion object {
        fun newInstance():Fragment{
            return Camera2Fragment()
        }
    }



//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?= inflater.inflate(
//        R.layout.fragment_camera2,container,false)

}