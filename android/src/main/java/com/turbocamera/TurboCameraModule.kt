package com.turbocamera

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class TurboCameraModule(reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext), PermissionListener {
    
    private val cameraService: CameraService = CameraService(reactContext)
    
    override fun getName() = "TurboCamera"
    
    override fun canOverrideExistingModule(): Boolean {
        return true
    }
    
    @ReactMethod
    fun takePicture(options: ReadableMap, promise: Promise) {
        cameraService.takePicture(options, promise)
    }
    
    @ReactMethod
    fun checkPermission(promise: Promise) {
        cameraService.checkPermission(promise)
    }
    
    @ReactMethod
    fun requestPermission(promise: Promise) {
        cameraService.requestPermission(promise)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        return cameraService.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}