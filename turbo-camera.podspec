require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name            = "turbo-camera"
  s.version         = package["version"]
  s.summary         = package["description"]
  s.license         = package["license"]
  s.author          = package["author"]
  s.homepage        = package["homepage"] || "https://github.com/username/turbo-camera"
  s.platforms       = { :ios => "13.0" }
  s.source          = { :git => "https://github.com/username/turbo-camera.git", :tag => "v#{s.version}" }
  s.source_files    = "ios/**/*.{swift,h,m,mm}"
  
  install_modules_dependencies(s)
end
