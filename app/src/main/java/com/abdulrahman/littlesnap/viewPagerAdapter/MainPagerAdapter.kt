package com.abdulrahman.littlesnap.viewPagerAdapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.abdulrahman.littlesnap.fragments.Camera2Fragment
import com.abdulrahman.littlesnap.fragments.ChatFragment
import com.abdulrahman.littlesnap.fragments.StoryFragment
import java.lang.NullPointerException

class MainPagerAdapter constructor(fm:FragmentManager): FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position){
            0 -> { // chat fragment
                ChatFragment.newInstance()
            }
            1 ->{ // Camera fragment
                Camera2Fragment.newInstance()
            }
            2 ->{ //  Story fragment .
                StoryFragment.newInstance()
            }
            else ->{
                throw NullPointerException("Fragment not found ")
            }
        }
    }

    override fun getCount(): Int  = 3

}