import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // 카메라 지원 여부 확인
  isSupported(): Promise<boolean>;

  // QR 코드 스캔 시작
  startScanning(): Promise<void>;

  // QR 코드 스캔 중지
  stopScanning(): void;

  // 이벤트 관련 메서드들
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('TurboCamera');
