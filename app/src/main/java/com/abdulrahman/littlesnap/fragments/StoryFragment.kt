package com.abdulrahman.littlesnap.fragments

import android.os.Bundle
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

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {

    }
}