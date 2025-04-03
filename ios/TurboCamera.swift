import AVFoundation
import UIKit
import Vision

@objc(TurboCamera)
class TurboCamera: RCTEventEmitter {
    
    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    
    override static func requiresMainQueueSetup() -> Bool {
        return false
    }
    
    override func supportedEvents() -> [String] {
        return ["onQRCodeDetected", "onTextDetected"]
    }
    
    @objc
    static func isSupported() -> Bool {
        return true
    }
    
    @objc
    func startScanning(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            let captureSession = AVCaptureSession()
            self.captureSession = captureSession
            
            guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
                reject("CAMERA_ERROR", "Failed to access camera", nil)
                return
            }
            
            let videoInput: AVCaptureDeviceInput
            
            do {
                videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
            } catch {
                reject("CAMERA_ERROR", "Failed to create camera input", error)
                return
            }
            
            if (captureSession.canAddInput(videoInput)) {
                captureSession.addInput(videoInput)
            } else {
                reject("CAMERA_ERROR", "Failed to add camera input to session", nil)
                return
            }
            
            let metadataOutput = AVCaptureMetadataOutput()
            
            if (captureSession.canAddOutput(metadataOutput)) {
                captureSession.addOutput(metadataOutput)
                
                metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                metadataOutput.metadataObjectTypes = [.qr]
            } else {
                reject("CAMERA_ERROR", "Failed to add metadata output", nil)
                return
            }
            
            captureSession.startRunning()
            resolve(nil)
        }
    }
    
    @objc
    func stopScanning() {
        captureSession?.stopRunning()
    }
}

extension TurboCamera: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let metadataObject = metadataObjects.first,
              let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject,
              let stringValue = readableObject.stringValue else { return }
        
        sendEvent(withName: "onQRCodeDetected", body: ["data": stringValue])
  }
}
