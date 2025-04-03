import {
  requireNativeComponent,
  ViewProps,
  PermissionsAndroid,
} from 'react-native';

interface TurboCameraViewProps extends ViewProps {
  onQRCodeDetected?: (event: { nativeEvent: { data: string } }) => void;
  onBarCodeRead?: (event: { nativeEvent: { data: string } }) => void;
  onTextDetected?: (event: { nativeEvent: { text: string } }) => void;
  onBackButtonPressed?: () => void;
  backgroundImageUrl?: string;
}

export const TurboCameraView =
  requireNativeComponent<TurboCameraViewProps>('TurboCameraView');

export const requestCameraPermission = async () => {
  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.CAMERA,
      {
        title: 'Camera Permission',
        message: 'App needs camera permission',
        buttonNeutral: 'Ask Me Later',
        buttonNegative: 'Cancel',
        buttonPositive: 'OK',
      },
    );
    return granted === PermissionsAndroid.RESULTS.GRANTED;
  } catch (err) {
    console.warn(err);
    return false;
  }
};
