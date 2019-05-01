package com.abdulrahman.littlesnap.fragments.stickers

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.R
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.layout_stickers_list_item.view.*

class StickerAdapter(private val listOfStickers: MutableList<Drawable>,private val clickListener:RecyclerViewClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {



    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_stickers_list_item,parent,false)
        return ViewHolder(v,clickListener)
    }

    override fun getItemCount(): Int  = listOfStickers.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val image = listOfStickers[position]

        (holder as ViewHolder).bindStickers(image)
    }


    class ViewHolder(val view:View,val clickListener: RecyclerViewClickListener) : RecyclerView.ViewHolder(view) , View.OnClickListener {

        fun bindStickers(image:Drawable){
            with(view){
                Glide.with(this.context)
                    .load(image)
                    .into(sticker_imageview)
            }

        }

        override fun onClick(viewId: View?) {
            when(viewId?.id){
                R.id.sticker_imageview ->{
                    clickListener.onStickerClicked(adapterPosition)
                }
            }
        }

    }


    interface RecyclerViewClickListener{
        fun onStickerClicked(position:Int)
    }
}