package com.abdulrahman.littlesnap.fragments.stickers

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.abdulrahman.littlesnap.R
import com.abdulrahman.littlesnap.callbacks.StickerViewListener
import com.abdulrahman.littlesnap.fragments.BaseFragment
import com.abdulrahman.littlesnap.model.stickers.Stickers
import com.abdulrahman.littlesnap.model.stickers.remote.StickersRemote
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

const val TAG = "Stickerfragments"

class StickerFragment : BaseFragment(), View.OnClickListener, StickerAdapter.StickerListener {

    private val NUM_COL = 3
    private var stickers = mutableListOf<Stickers>()

    private lateinit var adapter: StickerAdapter
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private lateinit var stickerListener: StickerViewListener

    companion object {
        fun newInstance(): Fragment {
            return StickerFragment()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        database = FirebaseDatabase.getInstance().getReference("stickers")
        storage = FirebaseStorage.getInstance().getReference("stickers")
        stickerListener = activity as StickerViewListener
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_stickers
    }

    private lateinit var recyclerView: RecyclerView

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {
        recyclerView = view.findViewById(R.id.sticker_recyclerview)
        recyclerView.layoutManager = GridLayoutManager(activity!!, NUM_COL)
        adapter = StickerAdapter(activity!!, stickers, this)
        recyclerView.adapter = adapter
        getStickers()
        val t = StickersRemote()
        GlobalScope.launch(Dispatchers.IO) {
            //            Log.i("xyz","Now i got this stickers  ${temp.size}")
            val temp = t.fetchStickers().await()
            Log.i("xyz", "Morning try >> ${temp.size}")
        }


    }

    override fun onStickerClicked(position: Int) {
        Log.i(TAG, "Clicked this stickers ${stickers[position].stickerId} ")
        stickerListener.sendStickerId(stickers[position].stickerId)
    }

    private fun getStickers() {

        database.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(e: DatabaseError) {
                Log.d(TAG, "getStrickers throw exception ${e.message}")
            }

            override fun onDataChange(dataSnapsot: DataSnapshot) {
                dataSnapsot.children.forEach {
                    val sticker = it.getValue(Stickers::class.java)
                    stickers.add(sticker!!)
                }
                adapter.notifyDataSetChanged()
            }
        })
    }

    override fun onClick(p0: View?) {

    }


    private fun uploadStickers() {
        repeat(tempSaveStickers().size) {
            val tempUri =
                Uri.parse("android.resource://${activity!!.packageName}/${tempSaveStickers()[it]}")
            val refStorage = storage.child("sticker_$it.png")
            try {
                refStorage.putFile(tempUri).continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    return@continueWithTask refStorage.downloadUrl
                }.addOnCompleteListener { x ->
                    if (x.isSuccessful) {
                        val result = x.result
                        val sticker = Stickers("name_$it", result.toString())
                        val stickerId = sticker.stickerId
                        database.child(stickerId).setValue(sticker)
                        Log.i(TAG, " onComplete done .")
                    }
                }.addOnFailureListener { e ->
                    Log.i(TAG, " failure throw this exception ${e.message} .")
                }
            } catch (e: Exception) {
                Log.d(TAG, " uploadStickers throw exception ${e.message}")
            }
        }

    }

    private fun tempSaveStickers(): MutableList<Int> {
        val allStickers = mutableListOf<Int>()
        allStickers.add(R.drawable.smallest_circle)
        allStickers.add(R.drawable.smiley_face_emoji)
        allStickers.add(R.drawable.smiley_face_tightly_closed_eyes_emoji)
        allStickers.add(R.drawable.smiley_smiling_eyes_emoji)
        allStickers.add(R.drawable.astonished_face_emoji)
        allStickers.add(R.drawable.cry_emoji)
        allStickers.add(R.drawable.evil_monkey_emoji)
        allStickers.add(R.drawable.nerd_emoji)
        allStickers.add(R.drawable.penguin_emoji)
        allStickers.add(R.drawable.sad_emoji)
        allStickers.add(R.drawable.slightly_smiling_face_emoji)
        allStickers.add(R.drawable.sunglasses_emoji)
        allStickers.add(R.drawable.unamused_emoji)
        allStickers.add(R.drawable.upside_down_face_emoji)
        allStickers.add(R.drawable.tears_of_joy_emoji)
        return allStickers
    }


}

//Sticker adapter
class StickerAdapter(val context: Context, val stickers: MutableList<Stickers>, val listener: StickerListener) :
    RecyclerView.Adapter<StickerHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): StickerHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.layout_stickers_list_item, p0, false)
        return StickerHolder(view, listener)
    }

    override fun getItemCount(): Int = stickers.size

    override fun onBindViewHolder(holder: StickerHolder, p1: Int) {
        val sticker = stickers[p1]
        holder.bindDate(sticker)
    }

    interface StickerListener {
        fun onStickerClicked(position: Int)
    }
}

//Sticker holder
class StickerHolder(itemView: View, val listener: StickerAdapter.StickerListener) : RecyclerView.ViewHolder(itemView) {

    val stickerImageView: ImageView = itemView.findViewById(R.id.sticker_imageview)
    lateinit var sticker: Stickers
    fun bindDate(sticker: Stickers) {
        this.sticker = sticker
        with(itemView) {
            Glide.with(context)
                .load(sticker.stickerUri)
                .fitCenter()
                .error(R.drawable.x_white_icon)
                .into(stickerImageView)

            this.setOnClickListener {
                listener.onStickerClicked(adapterPosition)
            }

        }
    }
}
