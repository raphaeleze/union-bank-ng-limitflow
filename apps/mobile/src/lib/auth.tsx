import * as LocalAuthentication from "expo-local-authentication";
import * as SecureStore from "expo-secure-store";
import { useRouter } from "expo-router";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

import { apiClient, AUTH_TOKEN_KEY, AUTH_USER_KEY } from "./api-client";
import { registerForPushNotifications, unregisterForPushNotifications } from "./push";
import type { LoginResponse, UserSummary } from "./types";

interface AuthContextValue {
  user: UserSummary | null;
  isReady: boolean;
  isUnlocked: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  unlock: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isReady, setIsReady] = useState(false);
  const [isUnlocked, setIsUnlocked] = useState(false);

  useEffect(() => {
    (async () => {
      const raw = await SecureStore.getItemAsync(AUTH_USER_KEY);
      if (raw) {
        try {
          setUser(JSON.parse(raw) as UserSummary);
        } catch {
          setUser(null);
        }
      }
      setIsReady(true);
    })();
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<LoginResponse>("/auth/login", { email, password });
    const { token, user: loggedInUser } = response.data;

    if (loggedInUser.role !== "CUSTOMER") {
      throw new Error("This app is for customer accounts only.");
    }

    await SecureStore.setItemAsync(AUTH_TOKEN_KEY, token);
    await SecureStore.setItemAsync(AUTH_USER_KEY, JSON.stringify(loggedInUser));
    setUser(loggedInUser);
    setIsUnlocked(true);
    void registerForPushNotifications();
  }, []);

  const logout = useCallback(async () => {
    // Unregister before the token is cleared below — the DELETE call needs the
    // still-valid token to authenticate against the backend.
    await unregisterForPushNotifications();
    await SecureStore.deleteItemAsync(AUTH_TOKEN_KEY);
    await SecureStore.deleteItemAsync(AUTH_USER_KEY);
    setUser(null);
    setIsUnlocked(false);
    router.replace("/login");
  }, [router]);

  /** Returns whether the app is now unlocked. Falls back to "unlocked" if the device has no
   * enrolled biometrics/passcode at all — that's a real device state, not an edge case, and
   * there is nothing left to gate with in that case (see the design spec). */
  const unlock = useCallback(async () => {
    const hasHardware = await LocalAuthentication.hasHardwareAsync();
    const isEnrolled = await LocalAuthentication.isEnrolledAsync();
    if (!hasHardware || !isEnrolled) {
      setIsUnlocked(true);
      return true;
    }
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: "Unlock LimitFlow",
      });
      setIsUnlocked(result.success);
      return result.success;
    } catch {
      // A native-level throw is rare but must resolve to "not unlocked" rather than reject —
      // the caller (the (app) gate layout) has no catch of its own and needs a boolean it can
      // react to, the same as an ordinary failed/cancelled prompt.
      setIsUnlocked(false);
      return false;
    }
  }, []);

  const value = useMemo(
    () => ({ user, isReady, isUnlocked, login, logout, unlock }),
    [user, isReady, isUnlocked, login, logout, unlock],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
