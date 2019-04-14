package com.abdulrahman.littlesnap.fragments

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class BaseFragment : Fragment() {


    @LayoutRes
    abstract fun getLayoutResId(): Int


    abstract fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mViewRoot = inflater.inflate(getLayoutResId(), container, false)
        inOnCreateView(mViewRoot, container, savedInstanceState)
        return mViewRoot
    }
}