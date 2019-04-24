package com.abdulrahman.littlesnap.fragments

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.abdulrahman.littlesnap.utlities.TAG

class DrawableImageView : AppCompatImageView {

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

    private class Pen(val color: Int, val width: Float) {

        internal var path: Path
        internal var paint: Paint

        init {
            path = Path()
            paint = Paint()
            paint.isAntiAlias = true
            paint.strokeWidth = width
            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
        }
    }


    private fun init(context: Context) {
        mPenList.add(Pen(color, width))
        setDrawingEnable(true)
        if (context is Activity) {
            mHostActivity = context as Activity
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        for (pen in mPenList) {
            canvas?.drawPath(pen.path, pen.paint)
        }
    }


    //Call this func when user click pen to draw .
    fun touchEvent(motionEvent: MotionEvent): Boolean {
//        hideStatusBar()
        if (mIsDrawingEnable) {
            val eventX = motionEvent.x
            val eventY = motionEvent.y

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.i(TAG,"Action Down ")
                    mPenList.add(Pen(color, width))
                    mPenList[mPenList.size - 1].path.moveTo(eventX, eventY)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    Log.i(TAG,"Action Move ")
                    mPenList[mPenList.size - 1].path.lineTo(eventX, eventY)
                }

                MotionEvent.ACTION_UP -> {
                    Log.i(TAG,"Action up ")

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


    fun getBrushColor():Int{
        return color
    }

    fun setBrushColor(color:Int){
        this.color = color
    }
}