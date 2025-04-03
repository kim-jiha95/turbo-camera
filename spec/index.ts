// index.ts
import type TurboCameraModule from './NativeTurboCamera';
import { TurboCameraView, requestCameraPermission } from '../TurboCameraView';

export type { TurboCameraModule };
export { TurboCameraView };
export { default as TurboCamera } from './NativeTurboCamera';
export { requestCameraPermission };
