require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name            = "TurboCamera"
  s.version         = package["version"]
  s.summary         = package["description"]
  s.license         = package["license"]
  s.author          = package["author"]
  s.homepage        = package["homepage"] || "https://github.com/kim-jiha95/turbo-camera"
  s.platforms       = { :ios => "13.0" }
  s.source          = { :git => "https://github.com/kim-jiha95/turbo-camera.git", :tag => "v#{s.version}" }
  s.source_files    = "ios/**/*.{swift,h,m,mm}"
  s.frameworks = "AVFoundation", "CoreImage"
  s.dependency "React-Core"

  install_modules_dependencies(s)
end
