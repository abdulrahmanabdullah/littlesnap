package com.abdulrahman.littlesnap.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R

class ChatFragment  : BaseFragment(){
    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {

    }


    companion object {
        fun newInstance():ChatFragment = ChatFragment()
    }


    override fun getLayoutResId(): Int {
        return R.layout.fragment_chat
    }
}