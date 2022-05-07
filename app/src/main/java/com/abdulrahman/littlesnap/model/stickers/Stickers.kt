package com.abdulrahman.littlesnap.model.stickers

import java.util.*

data class Stickers(val stickerName:String?="",val stickerUri:String?="") {

    val stickerId = UUID.randomUUID().toString()
}