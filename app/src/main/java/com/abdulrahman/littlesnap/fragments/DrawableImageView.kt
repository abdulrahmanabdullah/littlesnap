package com.abdulrahman.littlesnap.fragments

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.abdulrahman.littlesnap.R
import com.abdulrahman.littlesnap.model.Stickers
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val TAG = "DrawableImageView"

class DrawableImageView : AppCompatImageView {

    private var stickers = mutableListOf<Stickers>()

    private var color: Int = 0

    private var width: Float = 0f

    private var mPenList = mutableListOf<Pen>()

    private var mHostActivity: Activity? = null

    private var mIsDrawingEnable = false


    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private class Pen(val color: Int) {

        internal var path: Path
        internal var paint: Paint

        init {
            path = Path()
            paint = Paint()
            paint.isAntiAlias = true
            paint.strokeWidth = 20f
            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
        }
    }


    private fun init(context: Context) {
        mPenList.add(Pen(color))
        setDrawingEnable(true)
        if (context is Activity) {
            mHostActivity = context as Activity
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

//        if (localStickerList.size > 0 ){
//            for (s in localStickerList){
//                canvas?.drawBitmap(s.bitmap,s.x.toFloat(),s.y.toFloat(),s.paint)
//            }
//        }
        if (stickers.size > 0) {
            for (s in stickers) {
                Glide.with(context)
                    .asBitmap()
                    .load(s.stickerUri)
                    .override(45,45)
                    .fitCenter()
                    .error(R.drawable.sad_emoji)
                    .into(this)
            }
        }
        for (pen in mPenList) {
            canvas?.drawPath(pen.path, pen.paint)
        }
    }

    //Call this func when user click pen to draw .
    fun touchEvent(motionEvent: MotionEvent): Boolean {
//        hideTabLayoutIcons()
        if (mIsDrawingEnable) {
            val eventX = motionEvent.x
            val eventY = motionEvent.y

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.i(TAG, "Action Down ")
                    mPenList.add(Pen(color))
                    mPenList[mPenList.size - 1].path.moveTo(eventX, eventY)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    Log.i(TAG, "Action Move ")
                    mPenList[mPenList.size - 1].path.lineTo(eventX, eventY)
                }

                MotionEvent.ACTION_UP -> {
                    Log.i(TAG, "Action up ")

                }
                else -> {
                    return false
                }
            }
        }
        invalidate()
        return true
    }


    fun removeLastDraw() {
        if (mPenList.size > 0) {
            mPenList.removeAt(mPenList.size - 1)
            invalidate()
        }
    }


    private fun hideStatusBar() {
        if (mHostActivity != null) {
            val view = mHostActivity!!.window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
            view.systemUiVisibility = uiOptions
        }
    }

    fun reset() {
        for (pen in mPenList) {
            pen.path.reset()
        }
        invalidate()
    }

    fun setDrawingEnable(bool: Boolean) {
        mIsDrawingEnable = bool
    }


    fun getBrushColor(): Int {
        return color
    }

    fun setBrushColor(color: Int) {
        this.color = color
    }

    fun addNewSticker(stickerId: String?) {
        if (stickerId != null) {
            querySticker(stickerId)
            //todo solve this issues :->
            /**
             I need drawable image from glide or anywhere
             also need Bitmap.
             The problem now how get drawable from bitmap .
             */
            val bit = bitmapToDrawable(resources.getDrawable(R.drawable.sad_emoji))
            val ss = LocalSticker(bit,resources.getDrawable(R.drawable.sad_emoji),0,200)
            localStickerList.add(ss)
        } else {
            Log.i(TAG, "not receive any stickers ")
        }
        invalidate()
    }

    private fun querySticker(stickerId: String) {
        val query = FirebaseDatabase.getInstance().getReference("stickers")
            .orderByChild("stickerId").equalTo(stickerId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "listener throw exception ${error.message}")
            }

            override fun onDataChange(datasnpashot: DataSnapshot) {
                datasnpashot.children.forEach {
                    val sticker = it.getValue(Stickers::class.java)
                    stickers.add(sticker!!)
                    Log.i(TAG, "Stickers size = ${stickers.size}")
                }
            }

        })
        if (stickers.size > 0) {
//            val bitmap = Glide.with(context)
//                .asBitmap().load(stickers[0].stickerUri)
//                .into(object : CustomTarget<Bitmap>() {
//                    override fun onLoadCleared(placeholder: Drawable?) {
//                        Log.i(TAG, "onLoadCleared .. ")
//                    }
//
//                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                        Log.i(TAG, "onResourceReady  ${resource.height} , ${resource.width}")
//                    }
//                })
//            Glide
//                .with(context)
//                .load(stickers[0].stickerUri)
//                .override(300,300)
//                .into(this)
//
//            invalidate()
//            Log.i(TAG, "get this image ${bitmap}")
        }
    }

    private var localStickerList = mutableListOf<LocalSticker>()

    private class LocalSticker(val bitmap: Bitmap,val drawable: Drawable,val x:Int,val y:Int){
        var paint:Paint
        var rec:Rect
        init {
            paint = Paint()
            rec = Rect(x,y,x+200,y+300)
        }
    }

    fun bitmapToDrawable(drawable: Drawable):Bitmap{
        var bitmap:Bitmap? = null
        if(drawable is BitmapDrawable){
            val bitmapDrawable = drawable as BitmapDrawable
            if(bitmapDrawable.bitmap != null){
                return Bitmap.createScaledBitmap(bitmapDrawable.bitmap,200,200,false)
            }
        }else{
            bitmap = Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0,0,bitmap!!.width,bitmap.height)
        drawable.draw(canvas)
        return bitmap
    }
}