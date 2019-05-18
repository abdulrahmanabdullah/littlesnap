package com.abdulrahman.littlesnap.callbacks

interface StickerViewListener {
    fun sendStickerId(stickerId:String)
    fun getStickerId():String?
}