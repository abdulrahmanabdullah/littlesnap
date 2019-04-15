package com.abdulrahman.littlesnap.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R

class StoryFragment : BaseFragment() {

    companion object {
        fun newInstance():StoryFragment = StoryFragment()
    }
    override fun getLayoutResId(): Int {
        return R.layout.fragment_story
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.i("main","Story fragment is attach")
    }
    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {
        Log.i("main","Here Call fragment story")
    }


    override fun onDetach() {
        super.onDetach()
        Log.i("main","Detach fragment story")
    }

    override fun onPause() {
        super.onPause()
        Log.i("main","Pause fragment story")
    }
}