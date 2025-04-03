# turbo-camera

A React Native TurboModule for camera functionality and QR code scanning.

## Installation

```sh
npm install turbo-camera
# or
yarn add turbo-camera
```

## Publishing

To publish this package to npm, follow these steps:

1. Ensure you have an npm account and are logged in:
   ```
   npm login
   ```

2. Build the package:
   ```
   npm run build
   ```

3. Publish the package:
   ```
   npm publish
   ```

If you want to test the package locally without publishing:

1. Create a tarball:
   ```
   npm pack
   ```

2. Install the tarball in your project:
   ```
   npm install /path/to/turbo-camera-1.0.0.tgz
   ```

### iOS

Add the following to your `Podfile`:

```ruby
pod 'turbo-camera', :path => '../node_modules/turbo-camera'
```

Then run:

```sh
cd ios && pod install
```

### Android

No additional setup required for Android as it's linked automatically through React Native's auto-linking.

## Usage

```javascript
import { TurboCameraView, TurboCamera, requestCameraPermission } from 'turbo-camera';
import React, { useEffect, useState } from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';

const CameraExample = () => {
  const [hasPermission, setHasPermission] = useState(false);
  const [scanned, setScanned] = useState(false);
  const [qrData, setQrData] = useState('');

  useEffect(() => {
    (async () => {
      const isSupported = await TurboCamera.isSupported();
      if (isSupported) {
        const granted = await requestCameraPermission();
        setHasPermission(granted);
      }
    })();
  }, []);

  const handleQRCodeDetected = (event) => {
    setScanned(true);
    setQrData(event.nativeEvent.data);
    TurboCamera.stopScanning();
  };

  const startScanning = async () => {
    setScanned(false);
    setQrData('');
    await TurboCamera.startScanning();
  };

  if (hasPermission === null) {
    return <Text>Requesting camera permission...</Text>;
  }
  if (hasPermission === false) {
    return <Text>No access to camera</Text>;
  }

  return (
    <View style={styles.container}>
      <TurboCameraView
        style={styles.camera}
        onQRCodeDetected={!scanned ? handleQRCodeDetected : undefined}
      />
      {scanned && (
        <View style={styles.result}>
          <Text>QR Code data: {qrData}</Text>
          <Button title="Scan Again" onPress={startScanning} />
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
  },
  camera: {
    flex: 1,
  },
  result: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'white',
    padding: 15,
    alignItems: 'center',
  },
});

export default CameraExample;
```

## API

### TurboCamera

- `isSupported()`: Returns a promise that resolves to a boolean indicating whether the camera is supported.
- `startScanning()`: Starts scanning for QR codes.
- `stopScanning()`: Stops scanning for QR codes.

### TurboCameraView

A React Native component that displays the camera feed.

Props:
- `onQRCodeDetected`: Callback function that is called when a QR code is detected.
- `onBarCodeRead`: Callback function that is called when a barcode is read (alias for onQRCodeDetected).
- `onTextDetected`: Callback function that is called when text is detected.
- `onBackButtonPressed`: Callback function that is called when the back button is pressed.
- `backgroundImageUrl`: URL of the background image to display behind the camera feed.

### requestCameraPermission

A function that requests camera permission from the user.

## License

MIT