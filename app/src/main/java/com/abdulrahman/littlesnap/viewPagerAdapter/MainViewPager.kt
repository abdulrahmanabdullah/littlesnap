package com.abdulrahman.littlesnap.viewPagerAdapter

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent

class MainViewPager
    constructor(context: Context,attributeSet: AttributeSet): ViewPager(context,attributeSet) {

    private var isSwipeable = false

    init {
        isSwipeable = true
    }


    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        Log.i("pager","swipe ")
        if (isSwipeable){
            return super.onTouchEvent(ev)
        }
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.i("pager","onInterceptHoverEvent")
        if(isSwipeable){
            return super.onInterceptTouchEvent(ev)
        }
        return false
    }

    fun setSwipe(swipeable:Boolean){
        isSwipeable = swipeable
    }
}