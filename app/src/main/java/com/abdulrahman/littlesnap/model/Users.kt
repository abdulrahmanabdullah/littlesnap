package com.abdulrahman.littlesnap.model

import com.abdulrahman.littlesnap.R
import java.util.*

//todo: convert this class to entity class with Room
data class Users(val userImage: Int,
                 val userName: String,
                 val isChatOpen: Boolean) {
    var userId: UUID

    init {
        userId = UUID.randomUUID()
    }

}

//upload all images and return position of image
fun getAvatars(position: Int): Int {

    var p = position
    val avatars = listOf(
        R.drawable.img_amusing,
        R.drawable.img_boild_man,
        R.drawable.img_hat_man,
        R.drawable.img_raing_ear,
        R.drawable.img_ready_man,
        R.drawable.img_red_hair_main,
        R.drawable.ic_yellow_hair_women
    )
//    if (position >= avatars.size - 1)
//        p = 0
    return avatars[position%5]
}

object ShowUser {

    fun showUsers(): MutableList<Users> {
        val list = mutableListOf<Users>()
        for (index in 1..30) {
            val u = Users(getAvatars(index), "user name $index",index%2==0)
            list.add(u)
        }
        return list
    }

}