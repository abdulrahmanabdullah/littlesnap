package com.abdulrahman.littlesnap.fragments

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import com.abdulrahman.littlesnap.AutoFitTextureView
import com.abdulrahman.littlesnap.CompareSizeByArea
import com.abdulrahman.littlesnap.R
import com.abdulrahman.littlesnap.utlities.TAG
import com.abdulrahman.littlesnap.utlities.showToast
import java.lang.NullPointerException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera2Fragment : BaseFragment() {

    //Redfined width and height in onViewCreate
    private var SCREEN_WIDTH = 0
    private var SCREEN_HEIGHT = 0
    private val ASPECT_RATIO_ERROR_RANGE = 0.1F


    private val MAX_PREVIEW_WIDTH = 1920
    private val MAX_PREVIEW_HEIGHT = 1080


    //Reference to open camera device
    private var mCameraDevie: CameraDevice? = null

    // Camera device call back when camera change state
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(p0: CameraDevice?) {
            //This method call when camera is opened , start camera preview here
            mOpenCameraCloseLock.release()
            mCameraDevie = p0
            createCameraPreviewSession()
        }

        override fun onDisconnected(p0: CameraDevice?) {
            mOpenCameraCloseLock.release()
            p0!!.close()
            mCameraDevie = null
        }

        override fun onError(p0: CameraDevice?, p1: Int) {
            onDisconnected(p0)
            activity!!.showToast("Some error occurred when open camera  ")
        }

    }

    //[CaptureRequestBuild] for the camera preview
    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder
    //[CaptureRequest ] generated by mCaptureRequestBuilder
    private lateinit var mCaptureRequest: CaptureRequest

    //for camera preview
    private var mCaptureSession: CameraCaptureSession? = null

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private val ORIENTATIONS = SparseIntArray()

    private var mSensorsOrientation = 0


    /**
     * Camera state: Showing camera preview.
     */
    private val STATE_PREVIEW = 0

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private val STATE_WAITING_LOCK = 1

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private val STATE_WAITING_PRECAPTURE = 2

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private val STATE_WAITING_NON_PRECAPTURE = 3

    /**
     * Camera state: Picture was taken.
     */
    private val STATE_PICTURE_TAKEN = 4

    /** The current state of camera state for taking picture  */
    private var state = STATE_PREVIEW
    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        state = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            ) {
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPrecaptureSequence()
                }
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession?,
            request: CaptureRequest?,
            result: TotalCaptureResult
        ) {
            process(result)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession?,
            request: CaptureRequest?,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

    }

    private fun captureStillPicture() {
    }

    private fun runPrecaptureSequence() {

    }

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }


    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mOpenCameraCloseLock = Semaphore(1)
    //The Size of camera Preview
    private lateinit var mPreviewSize: Size
    /** Object of [AutoFitTextureView] and init in inOnCreateView*/
    private lateinit var mTextureView: AutoFitTextureView
    //Create surface listener of texture from layout and set preview
    private val mTextViewSurface = object : TextureView.SurfaceTextureListener {

        //setup preview coming from camera
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, width: Int, height: Int) {
            configTransForm(width, height)
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean = true

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, width: Int, height: Int) {
            activity!!.showToast("Hi from listener")
            openCamera(width, height)
        }

    }

    //To get camera Id
    private lateinit var mCameraId: String

    //todo : resolve open and close camera, This fragment always onAttach and not destroyed
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Log.i("main", "Camera fragment is attach")
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_camera2
    }

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {
        mTextureView = view.findViewById(R.id.camera_textureView)
        setMaxRatio()
    }


    override fun onResume() {
        super.onResume()
        startBackgroundHandler()
        if (mTextureView.isAvailable) {
            openCamera(mTextureView.width, mTextureView.height)
        } else {
            mTextureView.surfaceTextureListener = mTextViewSurface
        }

    }

    //Open and setup camera region
    private fun openCamera(width: Int, height: Int) {
        //double check permissions
        if (ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //todo : create dialog to confirm user accept camera permission
            return
        }


        setupCameraOutput(width, height)
        configTransForm(width, height)
        Log.i(TAG, " Test mPreviewSize = $mPreviewSize")
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            //wait for camera to open 2.5 seconds
            if (!mOpenCameraCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler)

        } catch (e: CameraAccessException) {
            Log.d(TAG, " openCamera Throw Camera exception ${e.message} ")
        }
    }


    private fun setupCameraOutput(width: Int, height: Int) {

        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            findCameraId()
            //Here Camera id become from findCameraId
            val characteristics = manager.getCameraCharacteristics(mCameraId)

            //get available resolution
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            //Now set best resolution
            var largest: Size = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),CompareSizeByArea())
            val screenAspectRation = SCREEN_WIDTH.toFloat() / SCREEN_HEIGHT.toFloat()
            val sizes = mutableListOf<Size>()
            //Iterate available resolution and found the largest resolution and capable of device
            for (size in map.getOutputSizes(ImageFormat.JPEG)) {

                var temp = size.width.toFloat() / size.height.toFloat()

                if (temp > (screenAspectRation - screenAspectRation * ASPECT_RATIO_ERROR_RANGE)
                    && temp < (screenAspectRation + screenAspectRation * ASPECT_RATIO_ERROR_RANGE)
                ) {
                    sizes.add(size)
                    Log.i(TAG, "setupCameraOutputs : found a valid size : w ${size.width}")
                    Log.i(TAG, "setupCameraOutputs : found a valid size : h ${size.height}")
                }

            }

            //After found best resolution add in largest
            if (sizes.size > 0) {
                largest = Collections.max(sizes, CompareSizeByArea())
                Log.i(TAG, "largest width = ${largest.width}")
                Log.i(TAG, "largest width = ${largest.height}")
            }


            //set screen orientation
            val displayRotation = activity!!.windowManager.defaultDisplay.rotation
            mSensorsOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            val swappedDimenions = areSwappedDimensions(displayRotation)

            val displaySize = Point()
            activity!!.windowManager.defaultDisplay.getSize(displaySize)
            Log.i(TAG,"Display size = $displaySize")
            val rotatedPreviewWidth = if (swappedDimenions) height else width
            val rotatedPreviewHeight = if (swappedDimenions) width else height
            var maxPreviewWidth = if (swappedDimenions) displaySize.y else displaySize.x
            var maxPreviewHeight = if (swappedDimenions) displaySize.x else displaySize.y


            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT


            //init Camera preview size ::
            mPreviewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java)
                , rotatedPreviewWidth, rotatedPreviewHeight,
                maxPreviewWidth, maxPreviewHeight, largest
            )

            Log.i(TAG, " mPreviewSize w :> ${mPreviewSize.width}")
            Log.i(TAG, " mPreviewSize w :> ${mPreviewSize.height}")


            // We fit the aspect ratio of TextureView to the size of preview we picked.
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRation(mPreviewSize.width, mPreviewSize.height)
            } else {
                mTextureView.setAspectRation(mPreviewSize.height, mPreviewSize.width)
            }


        } catch (e: CameraAccessException) {
            Log.d("main", "Camera throw exception on setupCameraOutputs ${e.message}")
        } catch (e: NullPointerException) {
            Log.d("main", "Camera throw null pointer exception on setupCameraOutputs ${e.printStackTrace()}")
        }

    }

    /**
     * Determines if the dimension are swapped given the phones current rotation .
     * @param displayRotation the current rotation of display
     * @return true if dimension swapped , false otherwise
     */
    private fun areSwappedDimensions(displayRotation: Int): Boolean {
        var swapped = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (mSensorsOrientation == 90 || mSensorsOrientation == 270) {
                    swapped = true
                    Log.i(TAG, " Rotation display are =  $displayRotation and value of boolean = $swapped ")
                }

            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (mSensorsOrientation == 0 || mSensorsOrientation == 180) {
                    swapped = true
                    Log.i(TAG, " Rotation display are =  $displayRotation and value of second condition  of boolean = $swapped ")
                }
            }
            else -> {
                swapped = false
                Log.i(TAG, " Rotation display are invalid  $displayRotation ")
            }
        }
        return swapped
    }

    //To select which camera use front or back
    private fun findCameraId() {
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                var facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
//                    mCameraId = cameraId
                    continue
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId
                }
            }
        } catch (e: CameraAccessException) {
            Log.d(TAG, " findCamera throw exception ${e.message}")
        }
    }



    //Call this function in inOnCreateView
    //This function solve stretching image to full screen for any phones
    private fun setMaxRatio(){
        val display = Point()
        activity!!.windowManager.defaultDisplay.getSize(display)
        SCREEN_WIDTH = display.y
        SCREEN_HEIGHT = display.x
        Log.i(TAG," Now width = $SCREEN_WIDTH And height = $SCREEN_HEIGHT")
    }
    private fun configTransForm(viewWidth: Int, viewHeight: Int) {
        activity ?: return

        //when device rotated , Here in my app it's protrit
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, mPreviewSize.height.toFloat(), mPreviewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / mPreviewSize.height,
                viewWidth.toFloat() / mPreviewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }//End rotated issued

        // fit camera preview with almost device ratio like Samsung s8 = 17.3/9
        var screenAspectRatio = SCREEN_WIDTH.toFloat() / SCREEN_HEIGHT.toFloat()
        var previewAspectRatio = mPreviewSize.width.toFloat() / mPreviewSize.height.toFloat()
        var roundScreenAspectRatio = String.format("%.2f",screenAspectRatio)
        var roundPreviewRatio = String.format("%.2f",previewAspectRatio)
        if (!roundPreviewRatio.equals(roundScreenAspectRatio)){
            var scaleFactory = (screenAspectRatio / previewAspectRatio)
            Log.i(TAG,"scale ratio = $scaleFactory")
            matrix.postScale(scaleFactory,1f) // Here we don't need height
            val heightCorrection = (SCREEN_WIDTH.toFloat()*scaleFactory) - (SCREEN_HEIGHT.toFloat()) / 2
            Log.i(TAG," height correction = $heightCorrection")
            Log.i(TAG," height correction negitave  = ${-heightCorrection}")

            matrix.postTranslate(-heightCorrection,0f)
        }


        mTextureView.setTransform(matrix)

    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */

    private fun createCameraPreviewSession() {

        try {
            val texture = mTextureView.surfaceTexture
            //We configure the size of default  buffer to be the size camera preview
            texture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
            //This is the output surface we need to start preview
            val surface = Surface(texture)

            mCaptureRequestBuilder = mCameraDevie!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mCaptureRequestBuilder.addTarget(surface)

            //Now create camera capture session
            mCameraDevie?.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigureFailed(p0: CameraCaptureSession?) {
                        activity!!.showToast("Errooroooroorororooo  fuck android")
                    }

                    override fun onConfigured(p0: CameraCaptureSession?) {
                        //Camera already close
                        if (mCameraDevie == null) return

                        //When the session ready , start displaying  preview
                        mCaptureSession = p0
                        try {

                            //Auto focus should be continuous
                            mCaptureRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            //todo : enable flash when camera have it .


                            //finally start displaying the camera preview
                            mCaptureRequest = mCaptureRequestBuilder.build()
                            mCaptureSession?.setRepeatingRequest(mCaptureRequest, captureCallback, mBackgroundHandler)

                        } catch (e: CameraAccessException) {
                            Log.d(TAG, "onConfigured camera preview ${e.message}")
                        }


                    }

                },
                null
            )


        } catch (e: CameraAccessException) {
            Log.d(TAG, " createCameraPreview throw exception ${e.reason}")
        }

    }

    //End region

    //Thread and Handler region
    private var mBackgroundThread: HandlerThread? = null
    private lateinit var mBackgroundHandler: Handler
    private fun startBackgroundHandler() {
        mBackgroundThread = HandlerThread("Camera2Api").also { it.start() }
        mBackgroundHandler = Handler(mBackgroundThread?.looper)
        Log.i("main", "Looper = ${mBackgroundHandler.looper}")
    }
    //End region


    companion object {
        fun newInstance(): Fragment {
            return Camera2Fragment()
        }


        //Compare preview camera size.
        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // choices of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizeByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizeByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }
    }


//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?= inflater.inflate(
//        R.layout.fragment_camera2,container,false)


}