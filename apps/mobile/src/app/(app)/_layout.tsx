import { Redirect, Slot } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, Text, View } from "react-native";

import { useAuth } from "@/lib/auth";

export default function AppGateLayout() {
  const { user, isReady, isUnlocked, unlock } = useAuth();
  const [unlockFailed, setUnlockFailed] = useState(false);

  async function attemptUnlock() {
    setUnlockFailed(false);
    const success = await unlock();
    if (!success) {
      setUnlockFailed(true);
    }
  }

  useEffect(() => {
    if (isReady && user && !isUnlocked) {
      attemptUnlock();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- attemptUnlock intentionally
    // omitted: it's re-created every render, and including it would re-run this effect (and
    // re-trigger the biometric prompt) on every unrelated re-render of this component.
  }, [isReady, user, isUnlocked]);

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
        {unlockFailed && (
          <Pressable onPress={attemptUnlock}>
            <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
          </Pressable>
        )}
      </View>
    );
  }

  // A layout's default export renders its matched nested route via <Slot />, not a `children`
  // prop — Expo Router (React Navigation underneath) never passes one; that's a Next.js
  // App Router convention, not this one. Task 10 replaces this whole function with a real
  // <Tabs> navigator, which renders its matched screen itself and won't need <Slot /> either.
  return <Slot />;
}
