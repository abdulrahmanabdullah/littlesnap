package com.abdulrahman.littlesnap.fragments.stickers

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R
import com.abdulrahman.littlesnap.fragments.BaseFragment
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.fragment_stickers.*

class StickerFragment : BaseFragment(), StickerAdapter.RecyclerViewClickListener, View.OnClickListener {


    companion object {
        fun newInstance(): Fragment {
            return StickerFragment()
        }
    }


    private val NUM_COL = 3
    private var stickers = mutableListOf<Drawable>()


    fun getStickers() {
        val d = ContextCompat.getDrawable(activity!!, R.drawable.smiley_face_emoji)
        stickers.add(d!!)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_stickers
    }

    private lateinit var recyclerView: RecyclerView
    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {
        getStickers()
        recyclerView = view.findViewById<RecyclerView>(R.id.sticker_recyclerview)
        recyclerView.layoutManager = GridLayoutManager(activity!!,NUM_COL)
        recyclerView.adapter = StickerAdapter(stickers,this)
    }

    override fun onStickerClicked(position: Int) {

    }


    override fun onClick(p0: View?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}