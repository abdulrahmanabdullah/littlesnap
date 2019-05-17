package com.abdulrahman.littlesnap.viewPagerAdapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.view.ViewGroup
import com.abdulrahman.littlesnap.fragments.Camera2Fragment
import com.abdulrahman.littlesnap.fragments.ChatFragment
import com.abdulrahman.littlesnap.fragments.StoryFragment
import java.lang.NullPointerException

class MainPagerAdapter constructor(fm:FragmentManager): FragmentStatePagerAdapter(fm) {


    //Create Map to add and track fragments
    private var pagerReference = mutableMapOf<Int,Fragment?>()

    fun getFragment(index:Int): Fragment? {
        return pagerReference[index]
    }


    override fun getItem(position: Int): Fragment? {
        return when(position){
            0 -> { // chat fragment
                pagerReference.put(position,ChatFragment.newInstance())
                ChatFragment.newInstance()

            }
            1 ->{ // Camera fragment
                pagerReference.put(position,Camera2Fragment.newInstance())
                Camera2Fragment.newInstance()
            }
            2 ->{ //  Story fragment .
                pagerReference.put(position,StoryFragment.newInstance())
                StoryFragment.newInstance()
            }
            else ->{
                throw NullPointerException("Fragment not found ")
            }
        }
    }

    override fun getCount(): Int  = 3

    override fun getPageTitle(position: Int): CharSequence? {
        return when(position){
           0 ->{
               "Chat"
           }
            1 ->{
                "Search"
            }

            2 ->{
                "Stories"
            }
            else->{
                " "
            }
        }
    }


    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        pagerReference.remove(position)
    }

    companion object{
        fun makeFragmentTag(viewId:Int):String{
            return "android:switcher$viewId:"
        }
    }
}