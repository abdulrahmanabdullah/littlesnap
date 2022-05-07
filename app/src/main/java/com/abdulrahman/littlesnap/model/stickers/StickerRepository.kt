package com.abdulrahman.littlesnap.model.stickers

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.abdulrahman.littlesnap.model.stickers.remote.StickersRemote
import kotlinx.coroutines.*

class StickerRepository(val stickersRemote: StickersRemote) : StickersDataSource {

}