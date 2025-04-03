package com.turbocamera

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.common.MapBuilder

class TurboCameraViewManager : SimpleViewManager<TurboCameraView>() {
    override fun getName() = "TurboCameraView"

    override fun createViewInstance(context: ThemedReactContext): TurboCameraView {
        return TurboCameraView(context)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return MapBuilder.builder<String, Any>()
            .put("onTextDetected", 
                MapBuilder.of(
                    "registrationName",
                    "onTextDetected"
                )
            )
            .put("onError",
                MapBuilder.of(
                    "registrationName",
                    "onError"
                )
            )
            .build()
    }
} 