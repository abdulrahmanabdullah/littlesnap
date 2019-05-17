package com.abdulrahman.littlesnap.model

import java.util.*

data class Stickers(val stickerName:String?="",val stickerUri:String?="") {

    val stickerId = UUID.randomUUID().toString()
}