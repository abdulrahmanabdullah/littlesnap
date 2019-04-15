package com.abdulrahman.littlesnap.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
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

//Adapter
class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatHolder>(){

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ChatHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(p0: ChatHolder, p1: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    //Holder ..
     class ChatHolder(val view:View) : RecyclerView.ViewHolder(view){

    }
}