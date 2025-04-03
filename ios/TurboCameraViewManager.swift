import Foundation
import React
import UIKit

@objc(TurboCameraViewManager)
class TurboCameraViewManager: RCTViewManager {
    override func view() -> UIView! {
        let cameraView = TurboCameraView()
        cameraView.delegate = self
        
        return cameraView
    }
    
    override class func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    @objc
    func supportedEvents() -> [String]! {
        return ["onQRCodeDetected", "onTextDetected", "onBackButtonPressed"]
    }
    
    // 이미지 ID를 사용하여 배경 이미지 설정 (React Native의 require에 대응)
    @objc
    func setBackgroundImage(_ reactView: UIView, imageId: NSNumber) {
        guard let cameraView = reactView as? TurboCameraView else { return }
        
        // React-Native 이미지 매니저를 통해 이미지 로드
        DispatchQueue.main.async {
            if let image = UIImage(named: "qr_background") {
                cameraView.setBackgroundImage(image: image)
            } else {
                let yellowColor = UIColor(red: 1.0, green: 0.8, blue: 0.0, alpha: 0.3)
                let size = CGSize(width: 1, height: 1)
                UIGraphicsBeginImageContext(size)
                let context = UIGraphicsGetCurrentContext()
                context?.setFillColor(yellowColor.cgColor)
                context?.fill(CGRect(origin: .zero, size: size))
                let image = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                
                if let colorImage = image {
                    cameraView.setBackgroundImage(image: colorImage)
                }
            }
        }
    }
}

extension TurboCameraViewManager: TurboCameraViewDelegate {
    func cameraView(_ view: TurboCameraView, didDetectQRCode code: String) {
        if let bridge = self.bridge {
            bridge.eventDispatcher().sendAppEvent(withName: "onQRCodeDetected", body: ["data": code])
        }
    }
    
    func cameraView(_ view: TurboCameraView, didDetectText text: String) {
        if let bridge = self.bridge {
            bridge.eventDispatcher().sendAppEvent(withName: "onTextDetected", body: ["text": text])
        }
    }
    
    func cameraViewDidPressBackButton(_ view: TurboCameraView) {
        if let bridge = self.bridge {
            bridge.eventDispatcher().sendAppEvent(withName: "onBackButtonPressed", body: nil)
        }
    }
}