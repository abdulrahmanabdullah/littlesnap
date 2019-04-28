package com.abdulrahman.littlesnap.viewPagerAdapter

import android.animation.ArgbEvaluator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import com.abdulrahman.littlesnap.R
import kotlinx.android.synthetic.main.view_snap_tab_laytou.view.*

class SnapTabView
    @JvmOverloads
    constructor(context: Context,attributeSet: AttributeSet,defStyle: Int=0): FrameLayout(context,attributeSet,defStyle) , ViewPager.OnPageChangeListener , View.OnClickListener{


    private lateinit var mArgEvaluator:ArgbEvaluator
    private var mCenterColor:Int? = null
    private var mSideColor:Int? = null
    private var mEndViewTranslationX = 0
    private var mIndcaitorTranslationx = 0f
    private var mCenterTranstionY = 0
    init {
        init()
    }

    private fun init(){
        LayoutInflater.from(context).inflate(R.layout.view_snap_tab_laytou,this,true)
        mArgEvaluator = ArgbEvaluator()
        mCenterColor = ContextCompat.getColor(context,R.color.White)
        mSideColor = ContextCompat.getColor(context,R.color.darkGrey)

        mIndcaitorTranslationx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,80f,resources.displayMetrics)

        tab_bottom_imageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{

            override fun onGlobalLayout() {
                mEndViewTranslationX = ((tab_bottom_imageView.x - tab_start_imageView.x) - mIndcaitorTranslationx ).toInt()
                tab_bottom_imageView.viewTreeObserver.removeOnGlobalLayoutListener {this}

                mCenterTranstionY = height - tab_bottom_imageView.bottom

            }

        })




    }
    override fun onPageScrollStateChanged(p0: Int) {

    }

    override fun onPageScrolled(position: Int, postionOffset: Float, p2: Int) {
       if(position == 0 ){
           setColor(1 - postionOffset)
           moveView(1 - postionOffset)
           tab_indicator_view.translationX = (postionOffset -1 ) * mIndcaitorTranslationx
           moveAndScaleCenter(1 - postionOffset)
       }
        else if (position == 1 ){
           setColor(postionOffset)
           moveView(postionOffset)
           tab_indicator_view.translationX = (postionOffset) * mIndcaitorTranslationx
           moveAndScaleCenter( postionOffset)
       }
    }



    override fun onPageSelected(position: Int) {
        when(position){
            1 -> {
                Log.i("snap","Here Call camera 2 fragment ")
            }
        }

    }

    private fun setColor(fraction:Float){
        var color :Int = mArgEvaluator.evaluate(fraction,mCenterColor,mSideColor) as Int
        tab_center_imageView.setColorFilter(color)
        tab_end_imageView.setColorFilter(color)
        tab_start_imageView.setColorFilter(color)

    }


    private fun moveView(fractionFromCenter:Float){
        tab_start_imageView.translationX = mEndViewTranslationX.times(fractionFromCenter)
        tab_end_imageView.translationX = -mEndViewTranslationX.times(fractionFromCenter)

        tab_indicator_view.alpha= fractionFromCenter
        tab_indicator_view.scaleX = fractionFromCenter

    }

    private fun moveAndScaleCenter(fractionFromCenter:Float){
        var scale = .7f + ( (1 - fractionFromCenter) * .3f)
        tab_center_imageView.scaleX = scale
        tab_center_imageView.scaleY = scale
        val translation = mCenterTranstionY.times(fractionFromCenter)
        tab_center_imageView.translationY = translation
        tab_bottom_imageView.translationY = translation

        tab_bottom_imageView.alpha = 1 - fractionFromCenter

    }
    private lateinit var mViewPager: ViewPager
    fun setupSnapTabViewListener(viewPager: ViewPager){
        mViewPager = viewPager
        mViewPager.addOnPageChangeListener(this)
        tab_center_imageView.setOnClickListener(this)
        tab_start_imageView.setOnClickListener(this)
        tab_end_imageView.setOnClickListener(this)
    }


    override fun onClick(viewId: View) {
      when(viewId.id) {
          R.id.tab_start_imageView -> {
              //Move to chat fragment
             if(mViewPager.currentItem != 0){
                 mViewPager.currentItem = 0
             }
          }

          R.id.tab_center_imageView ->{
              if (mViewPager.currentItem != 1){
                  mViewPager.currentItem = 1
              }
          }
          R.id.tab_end_imageView -> {
              //Move to story fragment
              if(mViewPager.currentItem != 2){
                  mViewPager.currentItem = 2
              }
          }


      }
    }
}