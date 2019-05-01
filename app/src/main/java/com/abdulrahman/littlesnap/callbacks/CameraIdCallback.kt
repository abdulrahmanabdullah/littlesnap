package com.abdulrahman.littlesnap.callbacks

interface CameraIdCallback {


    fun setCameraFrontFacing()

    fun setCameraBackFacing()

    fun isCameraFrontFacing():Boolean

    fun isCameraBackFacing():Boolean

    fun setFrontCameraId(cameraId:String)

    fun setBackCameraId(cameraId:String)

    fun getFrontCameraId():String

    fun getBackCameraId() :String


    fun showTabLayoutIcons()


    fun hideTabLayoutIcons()
}