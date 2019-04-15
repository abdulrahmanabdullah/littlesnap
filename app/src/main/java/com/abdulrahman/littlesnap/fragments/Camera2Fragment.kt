package com.abdulrahman.littlesnap.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R

class Camera2Fragment : BaseFragment() {

    //todo : resolve open and close camera, This fragment always onAttach and not destroyed
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.i("main","Camera fragment is attach")
    }

    override fun onStop() {
        super.onStop()
        Log.i("main","Pause fragment Camera *** ")
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("main","Camera fragment is Destroy")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.i("main","Camera fragment is onDestroyView")
    }
    override fun onDetach() {
        super.onDetach()
        Log.i("main","Camera fragment is detach")
    }
    override fun getLayoutResId(): Int {
        return R.layout.fragment_camera2
    }

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {

        Log.i("main","Here Call fragment Camera")
    }


    companion object {
        fun newInstance():Fragment{
            return Camera2Fragment()
        }
    }


//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?= inflater.inflate(
//        R.layout.fragment_camera2,container,false)

}