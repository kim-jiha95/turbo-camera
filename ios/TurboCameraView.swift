import AVFoundation
import UIKit
import Vision
import VisionKit

protocol TurboCameraViewDelegate: AnyObject {
    func cameraView(_ view: TurboCameraView, didDetectQRCode code: String)
    func cameraView(_ view: TurboCameraView, didDetectText text: String)
    func cameraViewDidPressBackButton(_ view: TurboCameraView)
}

class TurboCameraView: UIView {
    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    weak var delegate: TurboCameraViewDelegate?
    
    private let headerView = UIView()
    private let titleLabel = UILabel()
    private let backButton = UIButton(type: .system)
    private let backgroundImageView = UIImageView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCamera()
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupCamera()
        setupUI()
    }
    
    private func setupUI() {
        backgroundImageView.frame = bounds
        backgroundImageView.contentMode = .scaleAspectFill
        backgroundImageView.clipsToBounds = true
        addSubview(backgroundImageView)
        
        headerView.backgroundColor = UIColor(white: 0, alpha: 0.5)
        headerView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(headerView)
    }
    
    @objc private func backButtonTapped() {
        delegate?.cameraViewDidPressBackButton(self)
    }
    
    func setBackgroundImage(image: UIImage) {
        backgroundImageView.image = image
    }
    
    private func setupCamera() {
        let captureSession = AVCaptureSession()
        self.captureSession = captureSession
        
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return }
        
        do {
            let videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
            if captureSession.canAddInput(videoInput) {
                captureSession.addInput(videoInput)
            }
            
            let metadataOutput = AVCaptureMetadataOutput()
            if captureSession.canAddOutput(metadataOutput) {
                captureSession.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                metadataOutput.metadataObjectTypes = [.qr]
                
                metadataOutput.rectOfInterest = CGRect(x: 0.3, y: 0.3, width: 0.4, height: 0.4)
            }
            
            let videoOutput = AVCaptureVideoDataOutput()
            videoOutput.setSampleBufferDelegate(self, queue: DispatchQueue.global(qos: .userInitiated))
            if captureSession.canAddOutput(videoOutput) {
                captureSession.addOutput(videoOutput)
            }
            
            let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
            previewLayer.frame = bounds
            previewLayer.videoGravity = .resizeAspectFill
            layer.insertSublayer(previewLayer, at: 0)
            self.previewLayer = previewLayer
            
            DispatchQueue.global(qos: .userInitiated).async {
                captureSession.startRunning()
            }
        } catch {
            print("Camera setup error: \(error)")
        }
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
        backgroundImageView.frame = bounds
        
        if let metadataOutput = captureSession?.outputs.first as? AVCaptureMetadataOutput,
           let previewLayer = self.previewLayer {
            let rectOfInterest = CGRect(x: 0.3, y: 0.3, width: 0.4, height: 0.4)
            metadataOutput.rectOfInterest = rectOfInterest
        }
    }
}

extension TurboCameraView: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let metadataObject = metadataObjects.first,
              let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject,
              let stringValue = readableObject.stringValue else { return }
        
        delegate?.cameraView(self, didDetectQRCode: stringValue)
    }
}

extension TurboCameraView: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        
        let request = VNRecognizeTextRequest { [weak self] request, error in
            guard let self = self,
                  let results = request.results as? [VNRecognizedTextObservation],
                  !results.isEmpty else { return }
            
            let text = results.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ")
            DispatchQueue.main.async {
                self.delegate?.cameraView(self, didDetectText: text)
            }
        }
        
        try? VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:]).perform([request])
    }
}