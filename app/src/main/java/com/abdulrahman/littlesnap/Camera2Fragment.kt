package com.abdulrahman.littlesnap

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class Camera2Fragment : Fragment() {


    companion object {
        fun newInstance():Fragment{
            return Camera2Fragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?= inflater.inflate(R.layout.fragment_camera2,container,false)
}