package com.turbocamera

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Size

class CameraService(private val reactContext: ReactApplicationContext) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun startScanning(promise: Promise) {
        if (!hasPermission()) {
            promise.reject("PERMISSION_DENIED", "Camera permission not granted")
            return
        }

        val owner = reactContext.currentActivity as? LifecycleOwner
        if (owner == null) {
            promise.reject("ACTIVITY_NOT_FOUND", "Activity is null")
            return
        }
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(reactContext)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindAnalyzer(owner)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("CAMERA_ERROR", "Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(reactContext))
    }

    fun stopScanning() {
        try {
            cameraProvider?.unbindAll()
            imageAnalyzer = null
            camera = null
        } catch (e: Exception) {
            Log.e("CameraService", "Error stopping camera: ${e.message}")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAnalyzer(owner: LifecycleOwner) {
        Log.d("CameraService", "Starting bindAnalyzer")
        val scanner = BarcodeScanning.getClient()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                Log.d("CameraService", "Setting up image analyzer")
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        Log.d("CameraService", "Processing image frame")
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                Log.d("CameraService", "Scanner process completed. Found ${barcodes.size} barcodes")
                                if (barcodes.isNotEmpty()) {
                                    Log.d("CameraService", "Detected ${barcodes.size} barcodes")
                                    barcodes.firstOrNull()?.rawValue?.let { qrCode ->
                                        Log.d("CameraService", "QR Code detected: $qrCode")
                                        sendEvent("onQRCodeDetected", qrCode)
                                        stopScanning()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraService", "Barcode scanning failed: ${e.message}", e)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        Log.w("CameraService", "Received null mediaImage")
                        imageProxy.close()
                    }
                }
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            Log.d("CameraService", "Attempting to bind use cases to lifecycle")
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                owner,
                cameraSelector,
                imageAnalyzer
            )
            Log.d("CameraService", "Successfully bound camera use cases")
        } catch (e: Exception) {
            Log.e("CameraService", "Use case binding failed", e)
        }
    }

    private fun hasPermission(): Boolean {
        return reactContext.checkSelfPermission(android.Manifest.permission.CAMERA) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun checkPermission(promise: Promise) {
        promise.resolve(hasPermission())
    }
    
    fun requestPermission(promise: Promise) {
        val activity = reactContext.currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_NOT_FOUND", "Activity is null")
            return
        }

        if (hasPermission()) {
            promise.resolve(true)
            return
        }

        permissionPromise = promise
        activity.requestPermissions(
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && 
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            permissionPromise?.resolve(granted)
            permissionPromise = null
            return true
        }
        return false
    }

    private fun sendEvent(eventName: String, data: String) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, mapOf(
                "success" to true,
                "code" to data
            ))
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private var permissionPromise: Promise? = null

    fun cleanup() {
        cameraExecutor.shutdown()
    }

    fun takePicture(options: ReadableMap, promise: Promise) {
    }
}