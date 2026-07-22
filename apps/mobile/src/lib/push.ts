import * as Device from "expo-device";
import * as Notifications from "expo-notifications";
import { Platform } from "react-native";

import { apiClient } from "./api-client";

/** Best-effort: a denied permission, a simulator with no push capability, or a network
 * failure here must never block login — this mirrors the backend's own
 * "push failures are swallowed" rule from NotificationService. */
export async function registerForPushNotifications(): Promise<void> {
  try {
    if (!Device.isDevice) {
      return; // simulators/emulators can't receive real push tokens
    }
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    if (existingStatus !== "granted") {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    if (finalStatus !== "granted") {
      return;
    }

    const { data: expoPushToken } = await Notifications.getExpoPushTokenAsync();
    await apiClient.post("/devices/push-token", {
      expoPushToken,
      platform: Platform.OS,
    });
  } catch {
    // Deliberately swallowed — see the function comment above.
  }
}

export async function unregisterForPushNotifications(): Promise<void> {
  try {
    if (!Device.isDevice) {
      return;
    }
    const { data: expoPushToken } = await Notifications.getExpoPushTokenAsync();
    await apiClient.delete("/devices/push-token", { data: { expoPushToken, platform: Platform.OS } });
  } catch {
    // Same reasoning as above — logout must always succeed locally regardless.
  }
}
