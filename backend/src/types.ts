export type NotificationType = "notification" | "alarm";

export interface AlarmConfig {
  fullScreen: boolean;
  sound: "default" | "alarm_classic" | "gentle";
  loop: boolean;
  vibrate: boolean;
  snoozeMinutes: number | null;
}

export interface NotifyRequestBody {
  title: string;
  body: string;
  type: NotificationType;
  alarm?: Partial<AlarmConfig>;
  metadata?: Record<string, unknown>;
  /** Optional: target a single device. Omit to broadcast to every connected device. */
  deviceId?: string;
}

export interface StoredNotification {
  id: string;
  sourceAppId: string;
  sourceAppName: string;
  title: string;
  body: string;
  type: NotificationType;
  alarmConfig: AlarmConfig | null;
  metadata: Record<string, unknown> | null;
  targetDeviceId: string | null;
  createdAt: string;
}

export interface DeviceEnvelope {
  event: "notification";
  data: {
    id: string;
    title: string;
    body: string;
    type: NotificationType;
    alarm: AlarmConfig | null;
    source: string;
    metadata: Record<string, unknown> | null;
    targetDeviceId: string | null;
    targetDeviceName: string | null;
    createdAt: string;
  };
}

export const DEFAULT_ALARM_CONFIG: AlarmConfig = {
  fullScreen: true,
  sound: "default",
  loop: true,
  vibrate: true,
  snoozeMinutes: null,
};
