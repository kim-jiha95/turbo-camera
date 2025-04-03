package com.turbocamera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import android.view.ViewGroup
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.os.Handler
import android.os.HandlerThread
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.media.Image
import android.hardware.camera2.CaptureResult
import android.media.ImageReader
import android.graphics.ImageFormat
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.util.Log
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TurboCameraView(context: ThemedReactContext) : ViewGroup(context) {
    private val eventEmitter = context.getJSModule(RCTEventEmitter::class.java)
    private val surfaceView: SurfaceView = SurfaceView(context)
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val backgroundHandler: Handler
    private val backgroundThread: HandlerThread = HandlerThread("CameraBackground").apply { start() }
    private val imageReader: ImageReader = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 2)
    private var isScanning = false
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        backgroundHandler = Handler(backgroundThread.looper)
        addView(surfaceView)
        setupCamera()
        
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // 카메라 프리뷰 시작
                startPreview()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // 프리뷰 크기가 변경될 때 처리
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // 카메라 리소스 해제
                stopPreview()
            }
        })
    }

    private fun setupCamera() {
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            eventEmitter.receiveEvent(id, "onError", null)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startPreview() {
        if (!checkCameraPermission()) {
            val activity = (context as ThemedReactContext).currentActivity
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            } else {
                eventEmitter.receiveEvent(id, "onError", null)
            }
            return
        }

        val cameraId = this.cameraId ?: return
        
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    eventEmitter.receiveEvent(id, "onError", null)
                }
            }, null)
        } catch (e: SecurityException) {
            eventEmitter.receiveEvent(id, "onError", null)
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val surface = surfaceView.holder.surface
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)
            previewRequestBuilder?.addTarget(imageReader.surface)

            previewRequestBuilder?.apply {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            }

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    if (!isScanning) {
                        isScanning = true
                        processImage(image)
                    } else {
                        image.close()
                    }
                }
            }, backgroundHandler)

            cameraDevice?.createCaptureSession(
                listOf(surface, imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            previewRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            session.setRepeatingRequest(previewRequestBuilder?.build()!!, null, null)
                        } catch (e: Exception) {
                            eventEmitter.receiveEvent(id, "onError", null)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        eventEmitter.receiveEvent(id, "onError", null)
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            sendErrorEvent("Failed to create camera preview session: ${e.message}")
        }
    }

    private fun processImage(image: Image) {
        try {
            if (image.width <= 0 || image.height <= 0) {
                Log.e("TurboCameraView", "Invalid image dimensions: ${image.width}x${image.height}")
                image.close()
                isScanning = false
                return
            }

            Log.d("TurboCameraView", "Processing image: ${image.width}x${image.height}")
            val inputImage = InputImage.fromMediaImage(image, 0)
            
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    Log.d("TurboCameraView", "Text recognition success: ${visionText.text}")
                    if (visionText.text.isNotEmpty()) {
                        val event = Arguments.createMap().apply {
                            putString("text", visionText.text)
                            val blocksArray = Arguments.createArray()
                            for (block in visionText.textBlocks) {
                                val blockMap = Arguments.createMap().apply {
                                    putString("text", block.text)
                                    putInt("left", block.boundingBox?.left ?: 0)
                                    putInt("top", block.boundingBox?.top ?: 0)
                                    putInt("right", block.boundingBox?.right ?: 0)
                                    putInt("bottom", block.boundingBox?.bottom ?: 0)
                                }
                                blocksArray.pushMap(blockMap)
                            }
                            putArray("blocks", blocksArray)
                        }
                        eventEmitter.receiveEvent(id, "onTextDetected", event)
                    }
                    isScanning = false
                    image.close()
                }
                .addOnFailureListener { e ->
                    Log.e("TurboCameraView", "Text recognition failed", e)
                    sendErrorEvent("Failed to recognize text: ${e.message}")
                    isScanning = false
                    image.close()
                }
        } catch (e: Exception) {
            Log.e("TurboCameraView", "Process image error", e)
            e.printStackTrace()
            isScanning = false
            image.close()
        }
    }

    private fun stopPreview() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            eventEmitter.receiveEvent(id, "onError", null)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        surfaceView.layout(0, 0, width, height)
    }

    private fun sendErrorEvent(error: String) {
        (context as? ThemedReactContext)?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            ?.emit("cameraError", error)
    }

    fun cleanup() {
        try {
            stopPreview()
            backgroundThread.quitSafely()
            textRecognizer.close()
        } catch (e: Exception) {
            sendErrorEvent("Cleanup failed: ${e.message}")
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
} 