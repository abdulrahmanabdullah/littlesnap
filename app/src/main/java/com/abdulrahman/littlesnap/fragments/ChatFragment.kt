package com.abdulrahman.littlesnap.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R
import com.abdulrahman.littlesnap.model.ShowUser
import com.abdulrahman.littlesnap.model.Users
import com.abdulrahman.littlesnap.utlities.showToast
import kotlinx.android.synthetic.main.list_chat.view.*

class ChatFragment : BaseFragment() {


    val listUsers = ShowUser.showUsers()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.i("main","Chat fragment is attach")
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i("main","Chat fragment is Destroy")
    }

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {
       val recycler =  view.findViewById<RecyclerView>(R.id.chat_recyclerView)
        recycler.layoutManager = LinearLayoutManager(activity)
        val adapter = ChatAdapter(listUsers)
        recycler.adapter =adapter
        Log.i("main", "Here Call fragment chat ${listUsers[0].userId}")
        Log.i("main", "Here Call fragment chat ${listUsers[1].userId}")
        Log.i("main", "Here Call fragment chat ${listUsers[2].userId}")
    }


    companion object {
        fun newInstance(): ChatFragment = ChatFragment()
    }


    override fun getLayoutResId(): Int {
        return R.layout.fragment_chat
    }
}

//Adapter
class ChatAdapter(val list: MutableList<Users>) : RecyclerView.Adapter<ChatAdapter.ChatHolder>() {


    override fun onCreateViewHolder(container: ViewGroup, position: Int): ChatHolder {
        val v = LayoutInflater.from(container.context).inflate(R.layout.list_chat, container, false)
        return ChatHolder(v)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ChatHolder, p1: Int) {
        val user = list[p1]
        holder.bindData(user)
    }


    //Holder ..
    class ChatHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun bindData(users: Users) {
            with(view){
                chat_userName.text = users.userName
                chat_imgView.setImageResource(users.userImage)
                val checkChatOpen = if(users.isChatOpen ) "Open" else "received"
                chat_messageStatus_textView.text = checkChatOpen
            }
        }
    }
}