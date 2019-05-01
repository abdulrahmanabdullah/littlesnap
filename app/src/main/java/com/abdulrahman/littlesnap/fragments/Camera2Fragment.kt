package com.abdulrahman.littlesnap.fragments

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import com.abdulrahman.littlesnap.*
import com.abdulrahman.littlesnap.callbacks.CameraIdCallback
import com.abdulrahman.littlesnap.callbacks.SaveImageCallback
import com.abdulrahman.littlesnap.callbacks.StickerView
import com.abdulrahman.littlesnap.utlities.PIC_FILE_NAME
import com.abdulrahman.littlesnap.utlities.TAG
import com.abdulrahman.littlesnap.utlities.showSnackBar
import com.abdulrahman.littlesnap.utlities.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_camera2.*
import java.io.*
import java.lang.Exception
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

//todo : facing camera not take picture in galaxy phones
class Camera2Fragment : BaseFragment(), View.OnClickListener, View.OnTouchListener,
    VerticalSlideColorPicker.OnColorChangeListener {


//    //This element to show SnackBar
//    private lateinit var mView:View

    private var mIsDrawingEnable = false


    //View region
    /** Object of [AutoFitTextureView] and init in inOnCreateView*/
    private lateinit var mTextureView: AutoFitTextureView

    //This button for switch front and back cam
    lateinit var mSwitchCamImageButton: ImageButton

    private lateinit var mStillshotContainer: RelativeLayout

    private lateinit var mSwitchToggleContainer: RelativeLayout

    private lateinit var mFlashContainer: RelativeLayout

    //End View region

    //Camera2 Callback  it's = 4 interfaces and abstract classes  region
    //Preparing Texture view
    //Create surface listener of texture from layout and set preview
    private val mTextViewSurface = object : TextureView.SurfaceTextureListener {

        //setup preview coming from camera
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, width: Int, height: Int) {
            configTransForm(width, height)
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean = true

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, width: Int, height: Int) {
            openCamera(width, height)
        }
    }
    // Checking camera device -> Open or close
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

    //Preparing camera capture session AND capturing image from CameraCaptureSession
    private var mCaptureSession: CameraCaptureSession? = null
    //Capture image from CameraCaptureSession.StateCallback
    //Whenever a focus or still picture  is requested from user call this callback
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (mState) {
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> {

                    capturePicture(result)
                }
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        mState = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
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
    //Init in setupCameraOutputs func
    private var mImageReader: ImageReader? = null
    //Retrieving  data from mCaptureSession through mImageReader
    private var onImageAvailableListener =
        ImageReader.OnImageAvailableListener {
            if (!mIsImageAvailable) {
                mCapturedImage = it!!.acquireNextImage()
//            Log.i(TAG, "Image listener take picture ${mCapturedImage?.timestamp}")
                //todo : Solve slow take and save image -_- > Problem not here
                //Save image in memory but not solve my problem
                if (activity != null) {
                    activity!!.runOnUiThread {
                        Glide.with(activity!!)
                            .load(mCapturedImage)
                            .into(stillShot_imageView)

                        showStillShotContainer()
                    }
                }

                saveTempImageToStorage()
            }
        }
    //End Camera2 callback region

    //Listener for which camera used front or back  init this interface in onAttach
    private lateinit var mCameraIdCallback: CameraIdCallback

    //Listener for show and hide Sticker fragment init this interface int onAttach
    private lateinit var stickerView: StickerView

    //Listener for SaveImageCallback interface
    //Check value of this variable onImageAvailable
    private var mIsImageAvailable = false

    //This variable init inside asyncTask then uploaded into mStillshotImageView
    private var mCapturedBitmap: Bitmap? = null
    //AsyncTask
    private var mBackgroundImageTask: BackgroundImagerTask? = null

    override fun onClick(viewId: View) {
        when (viewId.id) {
            R.id.stillShot_imageButton -> {
                if (!mIsImageAvailable) {
                    takePicture()
                }
            }
            R.id.switchCamOrient -> {
                toggleCameraDisplayOrientation()
            }
            R.id.close_image_imageView -> {
                hideStillShotContainer()
            }

            R.id.pen_draw_imageButton -> {
                sticker_camera2_imageview.visibility = INVISIBLE
                toggleEnableDraw()
            }

            R.id.undo_draw_imageButton -> {
                undoAction()
            }

            R.id.save_picture_imageView -> {
                savePictureToDisk()
            }

            R.id.sticker_camera2_imageview ->{
                view?.showSnackBar("stickers clicked >> ",1)
            }
        }
    }


    override fun onTouch(p0: View?, motionEvent: MotionEvent): Boolean {
        if (mIsImageAvailable && mIsDrawingEnable) {
            Log.i(TAG, "Start draw ... ")
            return stillShot_imageView.touchEvent(motionEvent)
        }

        return true
    }


    override fun onColorChange(selectedColor: Int) {
        stillShot_imageView.setBrushColor(selectedColor)
    }


    //Redfined width and height in onViewCreate
    private var SCREEN_WIDTH = 0
    private var SCREEN_HEIGHT = 0
    private val ASPECT_RATIO_ERROR_RANGE = 0.1F


    private val MAX_PREVIEW_WIDTH = 1920
    private val MAX_PREVIEW_HEIGHT = 1080


    //Reference to open camera device
    private var mCameraDevie: CameraDevice? = null

    //Init this variable in onActivityCreated
    private lateinit var mFile: File


    /**
     * This function for  ImageSaver class and create image bitmap
     * And call asyc task to save image
     */
    //Todo : remove this function and replace it with savePictureToDisk
    private fun saveTempImageToStorage() {
        //Check Save image Callback interface ...
        val callBack = object : SaveImageCallback {
            override fun done(e: Exception?) {
                //This mean no any error or exception when take pictures
                if (e == null) {
                    mBackgroundImageTask = BackgroundImagerTask(activity!!)
                    mBackgroundImageTask?.execute()
                    mIsImageAvailable = true
                    mCapturedImage?.close()
                } else {
                    //Todo call SnackBar here
                    activity!!.showToast(" Some error occured ${e.message}")
                }
            }

        }

        //Call ImageSave class
        val imageSaver = ImageSaver(mCapturedImage, activity!!.getExternalFilesDir(null), callBack)
        mBackgroundHandler?.post(imageSaver)

    }

    //[CaptureRequestBuild] for the camera preview
    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder
    //[CaptureRequest ] generated by mCaptureRequestBuilder
    private lateinit var mCaptureRequest: CaptureRequest


    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private val ORIENTATIONS = SparseIntArray()

    private var mSensorsOrientation = 0

    //init it in setupCameraOutputs
    private var mCapturedImage: Image? = null

    /**
     * Camera mState: Showing camera preview.
     */
    private val STATE_PREVIEW = 0

    /**
     * Camera mState: Waiting for the focus to be locked.
     */
    private val STATE_WAITING_LOCK = 1

    /**
     * Camera mState: Waiting for the exposure to be precapture mState.
     */
    private val STATE_WAITING_PRECAPTURE = 2

    /**
     * Camera mState: Waiting for the exposure mState to be something other than precapture.
     */
    private val STATE_WAITING_NON_PRECAPTURE = 3

    /**
     * Camera mState: Picture was taken.
     */
    private val STATE_PICTURE_TAKEN = 4

    /** The current mState of camera mState for taking picture  */
    private var mState = STATE_PREVIEW


    /**
     *  @see Camera2Fragment.mCaptureCallback  */
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
                mState = STATE_PICTURE_TAKEN
                captureStillPicture()
            } else {
                runPrecaptureSequence()
            }
        }
    }

    private fun captureStillPicture() {
        if (activity == null || null == mCameraDevie) return
        try {
            //Take picture from CaptureRequestBuilder
            val captureBuilder =

                mCameraDevie!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(mImageReader?.surface)
                }
            //use the same AE and AF mode as the preview
            captureBuilder
                .set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            //Rotate the image from screen orientation to orientation
            val rotation = activity!!.windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))


            val captureCallBack = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(
                    session: CameraCaptureSession?,
                    request: CaptureRequest?,
                    result: TotalCaptureResult?
                ) {
                    unLockFocus()
                }
            }

            mCaptureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder.build(), captureCallBack, null)
            }
        } catch (e: CameraAccessException) {
            Log.d(TAG, "captureStillPicture throw exception ${e.message}")
        }
    }

    private fun runPrecaptureSequence() {
        try {
            //This is how to tell the camera trigger
            mCaptureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            mState = STATE_WAITING_PRECAPTURE
            mCaptureSession?.capture(mCaptureRequestBuilder.build(), mCaptureCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "runPrecaptureSequence throw this exception ${e.message}")
        }

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

    //To get camera Id
    private lateinit var mCameraId: String

    //Implementation Region
    override fun getLayoutResId(): Int {
        return R.layout.fragment_camera2
    }

    override fun inOnCreateView(view: View, container: ViewGroup?, bundle: Bundle?) {
        //Relative layout
//         view.findViewById<RelativeLayout>(R.id.switch_toggle_container)
//        view.findViewById<RelativeLayout>(R.id.flash_toggle_container)
//        view.findViewById<RelativeLayout>(R.id.capture_button_container)
//        mTextureView = view.findViewById(R.id.camera_textureView)
//        view.findViewById<RelativeLayout>(R.id.stillShot_container)
        view.findViewById<ImageView>(R.id.stillShot_imageButton).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.switchCamOrient).setOnClickListener(this)
        view.findViewById<ImageView>(R.id.close_image_imageView).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.pen_draw_imageButton).setOnClickListener(this)
        view.findViewById<ImageView>(R.id.stillShot_imageView).setOnTouchListener(this)
        view.findViewById<VerticalSlideColorPicker>(R.id.color_picker).setOnColorChangeListener(this)
        view.findViewById<ImageButton>(R.id.undo_draw_imageButton).setOnClickListener(this)
        view.findViewById<ImageView>(R.id.save_picture_imageView).setOnClickListener(this)

        view.findViewById<ImageView>(R.id.sticker_camera2_imageview).setOnClickListener(this)

    }

    //Call this function in inOnCreateView
    //This function solve stretching image to full screen for any phones
    private fun setMaxRatio() {
        val display = Point()
        activity!!.windowManager.defaultDisplay.getSize(display)
        SCREEN_WIDTH = display.y
        SCREEN_HEIGHT = display.x
        Log.i(TAG, " Now width = $SCREEN_WIDTH And height = $SCREEN_HEIGHT")
    }
    //End Implementation region


    //Fragment Lifecycle region
    //todo : resolve open and close camera, This fragment always onAttach and not destroyed
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mCameraIdCallback = activity as CameraIdCallback
        stickerView = activity as StickerView
        Log.i("xyz", "Camera fragment is attach")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetIconVisibilties()
        setMaxRatio()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundHandler()

        if (mIsImageAvailable) {
            mCameraIdCallback.hideTabLayoutIcons()
        } else {
            mCameraIdCallback.showTabLayoutIcons()
            reopenCamera()
        }
//        if (camera_textureView.isAvailable) {
//            openCamera(camera_textureView.width, camera_textureView.height)
//        } else {
//            camera_textureView.surfaceTextureListener = mTextViewSurface
//        }

    }


    override fun onPause() {
        super.onPause()
        closeCamera()
        stopBackgroundHnadler()
        //Avoid memory leak
        if (mBackgroundImageTask != null) {
            mBackgroundImageTask?.cancel(true)
        }
    }
    //End Lifecycle region


    //Open and setup camera region
    private fun openCamera(width: Int, height: Int) {

        if (ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        setupCameraOutput(width, height)
        configTransForm(width, height)
        Log.i(TAG, " Test mPreviewSize = $mPreviewSize")
        //todo : write new class for Open and setup camera
        // Like this link  https://proandroiddev.com/understanding-camera2-api-from-callbacks-part-1-5d348de65950
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


    private fun reopenCamera() {
        if (camera_textureView.isAvailable) {
            openCamera(camera_textureView.width, camera_textureView.height)
        } else {
            camera_textureView.surfaceTextureListener = mTextViewSurface
        }
    }

    private fun setupCameraOutput(width: Int, height: Int) {


        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            if (!mCameraIdCallback.isCameraBackFacing() && !mCameraIdCallback.isCameraFrontFacing()) {
                findCameraId()
            }
            //Here Camera id become from findCameraId
            val characteristics = manager.getCameraCharacteristics(mCameraId)

            //get available resolution
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            //Now set best resolution
            var largest: Size =
                Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizeByArea())
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
            Log.i(TAG, "Display size = $displaySize")
            val rotatedPreviewWidth = if (swappedDimenions) height else width
            val rotatedPreviewHeight = if (swappedDimenions) width else height
            var maxPreviewWidth = if (swappedDimenions) displaySize.y else displaySize.x
            var maxPreviewHeight = if (swappedDimenions) displaySize.x else displaySize.y


            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

            //Init mCapturedImage
            mImageReader = ImageReader.newInstance(
                largest.width,
                largest.height, ImageFormat.JPEG, 2
            )
            mImageReader?.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler)

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
                camera_textureView.setAspectRation(mPreviewSize.width, mPreviewSize.height)
            } else {
                camera_textureView.setAspectRation(mPreviewSize.height, mPreviewSize.width)
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
                    Log.i(
                        TAG,
                        " Rotation display are =  $displayRotation and value of second condition  of boolean = $swapped "
                    )
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
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraIdCallback.setFrontCameraId(cameraId)
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraIdCallback.setBackCameraId(cameraId)
                }
            }
            //Set front facing when app start
//            mCameraIdCallback.setCameraFrontFacing()
//            mCameraId = mCameraIdCallback.getFrontCameraId()
            //Move to Back lens when app start ..
            mCameraIdCallback.setCameraBackFacing()
            mCameraId = mCameraIdCallback.getBackCameraId()

        } catch (e: CameraAccessException) {
            Log.d(TAG, " findCamera throw exception ${e.message}")
        }
    }


    private fun toggleCameraDisplayOrientation() {
        if (mCameraId == mCameraIdCallback.getBackCameraId()) {
            mCameraId = mCameraIdCallback.getFrontCameraId()
            mCameraIdCallback.setCameraFrontFacing()
            closeCamera()
            reopenCamera()
        } else if (mCameraId == mCameraIdCallback.getFrontCameraId()) {
            mCameraId = mCameraIdCallback.getBackCameraId()
            mCameraIdCallback.setCameraBackFacing()
            closeCamera()
            reopenCamera()
        }

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
        var roundScreenAspectRatio = String.format("%.2f", screenAspectRatio)
        var roundPreviewRatio = String.format("%.2f", previewAspectRatio)
        if (!roundPreviewRatio.equals(roundScreenAspectRatio)) {
            var scaleFactory = (screenAspectRatio / previewAspectRatio)
            Log.i(TAG, "scale ratio = $scaleFactory")
            matrix.postScale(scaleFactory, 1f) // Here we don't need height
            val heightCorrection = (SCREEN_WIDTH.toFloat() * scaleFactory) - (SCREEN_HEIGHT.toFloat()) / 2
            Log.i(TAG, " height correction = $heightCorrection")
            Log.i(TAG, " height correction negitave  = ${-heightCorrection}")

            matrix.postTranslate(-heightCorrection, 0f)
        }


        camera_textureView.setTransform(matrix)

    }


    private fun closeCamera() {
        try {
            mOpenCameraCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession?.close()
                mCaptureSession = null
            }

            if (null != mCameraDevie) {
                mCameraDevie?.close()
                mCameraDevie = null
            }

            if (null != mImageReader) {
                mImageReader!!.close()
                mImageReader = null
            }
        } catch (e: InterruptedException) {
            Log.d(TAG, " close camera throw this exception ${e.message}")
        } finally {
            mOpenCameraCloseLock.release()
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */

    private fun createCameraPreviewSession() {

        try {
            val texture = camera_textureView.surfaceTexture
            //We configure the size of default  buffer to be the size camera preview
            texture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
            //This is the output surface we need to start preview
            val surface = Surface(texture)

            mCaptureRequestBuilder = mCameraDevie!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }
//            mCaptureRequestBuilder.addTarget(surface)

            //Now create camera capture session
            mCameraDevie?.createCaptureSession(
                Arrays.asList(surface, mImageReader?.surface),
                //This interface for configuring capture session from a camera .
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
                            mCaptureSession?.setRepeatingRequest(
                                mCaptureRequest,
                                mCaptureCallback, mBackgroundHandler
                            )
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


    //Take Camera Picture region
    private fun takePicture() {
        lockFocus()
    }

    /**
     * lock focus as the first step for a still image capture
     */
    private fun lockFocus() {
        try {
            //This is to tell the camera to lock focus
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            //Tell mCaptureCallback to wait for the lock
            mState = STATE_WAITING_LOCK
            mCaptureSession?.capture(
                mCaptureRequestBuilder.build(),
                mCaptureCallback, mBackgroundHandler
            )

        } catch (e: CameraAccessException) {

            Log.i(TAG, "lockFocus throw exception ${e.message}")
        }

    }


    //This functions will be called when capture image finish
    private fun unLockFocus() {
        try {
            //Reset auto-focus trigger
            mCaptureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            mCaptureSession?.capture(mCaptureRequestBuilder.build(), mCaptureCallback, mBackgroundHandler)
            //After this camera will  go the normal mode
            mState = STATE_PREVIEW
            // Back to Camera Preview , Don't hold image after take picture
            mCaptureSession?.setRepeatingRequest(mCaptureRequest, mCaptureCallback, mBackgroundHandler)

        } catch (e: CameraAccessException) {
            Log.d(TAG, "unLockfocus throw exception ${e.message}")
        }
    }
    //End region


    //Thread and Handler region
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null

    private fun startBackgroundHandler() {
        mBackgroundThread = HandlerThread("Camera2Api").also { it.start() }
        mBackgroundHandler = Handler(mBackgroundThread?.looper)
        Log.i("main", "Looper = ${mBackgroundHandler?.looper}")
    }

    private fun stopBackgroundHnadler() {

        if (mBackgroundThread != null) {
            mBackgroundThread?.quitSafely()
            try {
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: Exception) {

            }
        }

    }
    //End Thread region


    //Static functions
    companion object {
        fun newInstance(): Fragment {
            return Camera2Fragment()
        }


        //Compare preview camera size, Google forum
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

    //Background task region
    //todo : convert this part to Rx
    private inner class BackgroundImagerTask(context: Context?) : AsyncTask<Void, Int, Int>() {

        override fun doInBackground(vararg p0: Void?): Int {

            val file = File(context?.getExternalFilesDir(null), PIC_FILE_NAME)
            val tempImageUri: Uri = Uri.fromFile(file)
            Log.d(TAG, "Check file path ${tempImageUri.path}")
            var bitMap: Bitmap? = null
            try {
                val exif = ExifInterface(tempImageUri.path)
                bitMap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, tempImageUri)
                val orientation =
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                //Init Bitmap
                mCapturedBitmap = rotateBitmap(orientation, bitMap)!!

            } catch (e: IOException) {
                Log.d(TAG, "doInBackground ${e.message}")
                return 0
            }
            return 1
        }

        override fun onPostExecute(result: Int?) {
            super.onPostExecute(result)
            if (result == 1) {
                displayCaptureImage()
            } else {
                Log.d(TAG, " Error in doInBackground result != 1")
            }
        }

    }
    //End Background task region

    //Show image after take picture
    fun displayCaptureImage() {
        if (activity != null) {
            activity!!.runOnUiThread {
                val options = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()

                Glide.with(activity!!)
                    .setDefaultRequestOptions(options)
                    .load(mCapturedBitmap)
                    .into(stillShot_imageView)
                showStillShotContainer()
            }
        }
    }



    //Call this function when take picture and display it on stillShot_imageView
    private fun showStillShotContainer() {
        switch_toggle_container.visibility = INVISIBLE
        capture_button_container.visibility = INVISIBLE
        flash_toggle_container.visibility = INVISIBLE
        //Set Still shot container visible
        stillShot_container.visibility = VISIBLE
        mCameraIdCallback.hideTabLayoutIcons()
        stickerView.toggleViewStickersFragment()
        closeCamera()

    }

    //Call this function in background task - doInBackground to check image rotation
    private fun rotateBitmap(orientation: Int, bitmap: Bitmap): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_NORMAL -> {
                return bitmap
            }

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.setRotate(180f)
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.setRotate(90f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.setRotate(-90f)
            }

        }

        return try {
            //solve mirror picture
            if (mCameraIdCallback.isCameraFrontFacing()) {
                matrix.setScale(-1f, 1f)
            }

            val bitmapRotater = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bitmapRotater
        } catch (e: Exception) {
            null
        }

    }

    //Class for save image
    class ImageSaver : Runnable {

        var image: Image? = null
        var file: File
        var callback: SaveImageCallback? = null
        var bitmap: Bitmap? = null

        constructor(image: Image?, file: File, callback: SaveImageCallback?) {
            this.image = image
            this.file = file
            this.callback = callback
        }

        constructor(bitmap: Bitmap, file: File, callback: SaveImageCallback?) {
            this.bitmap = bitmap
            this.file = file
            this.callback = callback
        }


        override fun run() {
            if (image != null) {
                val buffer = image!!.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var outputStream: FileOutputStream? = null
                try {
                    val f = File(file, PIC_FILE_NAME)
                    outputStream = FileOutputStream(f).apply {
                        write(bytes)
                    }

                } catch (e: IOException) {
                    Log.d(TAG, " ImageSaver throw exception ${e.message}")
                    callback!!.done(e)
                } finally {
                    image?.close()
                    outputStream?.let {
                        try {
                            it.close()
                        } catch (e: IOException) {
                            Log.d(TAG, "finally  ImageSaver throw exception ${e.message}")
                        }
                    }

                    callback!!.done(null)
                }

            }
            //Save image to disk after draw
            else if (bitmap != null) {
                var stream: ByteArrayOutputStream? = null
                var imageByteArray: ByteArray? = null
                stream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                imageByteArray = stream.toByteArray()
                val dateFormate = SimpleDateFormat("ddMMyyyyhhmmss")
                val formate = dateFormate.format(Date())
                val f = File(file, "_image$formate.jpg")
                //Save mirrored array
                var output: FileOutputStream? = null
                try {
                    output = FileOutputStream(f)
                    output.write(imageByteArray)
                } catch (e: IOException) {
                    callback?.done(e)
                } finally {
                    if(null != output){
                        try{
                            output.close()
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                    callback?.done(null)
                }
            }
        }

    }


    //Edit Image after captured region

    private fun hideStillShotContainer() {
        //Hide TabLayout icons when captured .
        mCameraIdCallback.showTabLayoutIcons()
        if (mIsImageAvailable) {
            mIsImageAvailable = false
            mCapturedBitmap = null

            mIsDrawingEnable = false
            stillShot_imageView.reset()
            stillShot_imageView.setDrawingEnable(mIsDrawingEnable)
            stillShot_imageView.setImageBitmap(null)

            resetIconVisibilties()
            reopenCamera()
        }
    }


    private fun resetIconVisibilties() {
        switch_toggle_container.visibility = VISIBLE
        capture_button_container.visibility = VISIBLE
        flash_toggle_container.visibility = VISIBLE
        //Still shot container visible contain pen_draw_imageButton and close_image
        stillShot_container.visibility = INVISIBLE
        colors_picker_container.visibility = INVISIBLE
        undo_draw_container.visibility = INVISIBLE
    }

    //End edit image region


    //Draw on Image

    fun toggleEnableDraw() {
        if (colors_picker_container.visibility == VISIBLE) {
            colors_picker_container.visibility = INVISIBLE
            undo_draw_container.visibility = INVISIBLE
            mIsDrawingEnable = false
        } else if (colors_picker_container.visibility == INVISIBLE) {
            colors_picker_container.visibility = VISIBLE
            undo_draw_container.visibility = VISIBLE
            if (stillShot_imageView.getBrushColor() == 0) {
                stillShot_imageView.setBrushColor(Color.WHITE)
            }
            mIsDrawingEnable = true
        }
        stillShot_imageView.setDrawingEnable(mIsDrawingEnable)
    }

    private fun undoAction() {
        if (colors_picker_container.visibility == VISIBLE) {
            stillShot_imageView.removeLastDraw()
        }
    }
    //End draw region


    //Save picture to disk region
    fun savePictureToDisk() {
        if (mIsImageAvailable) {

            val callback = object : SaveImageCallback {
                override fun done(e: Exception?) {
                    if (e == null) {
                        view?.showSnackBar("Image Saved ", 0)
                    } else {
                        view?.showSnackBar("Image Saved ", 0)
                    }
                }
            }

            if (mCapturedImage != null) {
                try{
                    stillShot_imageView.invalidate()
                    stillShot_imageView.buildDrawingCache()
                    val bitMap = Bitmap.createBitmap(stillShot_imageView.getDrawingCache())
                    val imageSave = ImageSaver(bitMap, activity!!.getExternalFilesDir(null), callback)
                    mBackgroundHandler?.post(imageSave)
                }catch (e:NullPointerException){
                    Log.d("xzy","this cause ${e.message}")
                }
            }

        }
    }
}


