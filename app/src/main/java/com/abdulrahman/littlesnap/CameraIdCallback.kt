package com.abdulrahman.littlesnap

interface CameraIdCallback {


    fun setCameraFrontFacing()

    fun setCameraBackFacing()

    fun isCameraFrontFacing():Boolean

    fun isCameraBackFacing():Boolean

    fun setFrontCameraId(cameraId:String)

    fun setBackCameraId(cameraId:String)

    fun getFrontCameraId():String

    fun getBackCameraId() :String
}