import { Redirect } from "expo-router";
import { useEffect } from "react";
import { Text, View } from "react-native";

import { useAuth } from "@/lib/auth";

export default function AppGateLayout({ children }: { children: React.ReactNode }) {
  const { user, isReady, isUnlocked, unlock } = useAuth();

  useEffect(() => {
    if (isReady && user && !isUnlocked) {
      unlock();
    }
  }, [isReady, user, isUnlocked, unlock]);

  if (!isReady) {
    return (
      <View className="flex-1 items-center justify-center bg-surface dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Loading…</Text>
      </View>
    );
  }

  if (!user) {
    return <Redirect href="/login" />;
  }

  if (!isUnlocked) {
    return (
      <View className="flex-1 items-center justify-center gap-4 bg-surface px-4 dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">
          Unlock LimitFlow to continue.
        </Text>
      </View>
    );
  }

  return children as React.ReactElement;
}
