import { Bell, ClipboardList, LayoutDashboard, ArrowUpCircle, User } from "lucide-react-native";
import { Redirect, Tabs } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, Text, View } from "react-native";

import { Header } from "@/components/layout/header";
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

  return (
    <View className="flex-1 bg-surface dark:bg-surface-dark">
      <Header />
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarActiveTintColor: "#5b3df5",
          tabBarInactiveTintColor: "#6b6580",
        }}
      >
        <Tabs.Screen name="index" options={{ title: "Home", tabBarIcon: ({ color, size }) => <LayoutDashboard color={color} size={size} /> }} />
        <Tabs.Screen name="increase-limit" options={{ title: "Increase", tabBarIcon: ({ color, size }) => <ArrowUpCircle color={color} size={size} /> }} />
        <Tabs.Screen name="requests" options={{ title: "Requests", tabBarIcon: ({ color, size }) => <ClipboardList color={color} size={size} /> }} />
        <Tabs.Screen name="notifications" options={{ title: "Alerts", tabBarIcon: ({ color, size }) => <Bell color={color} size={size} /> }} />
        <Tabs.Screen name="profile" options={{ title: "Profile", tabBarIcon: ({ color, size }) => <User color={color} size={size} /> }} />
        <Tabs.Screen name="support" options={{ href: null }} />
        <Tabs.Screen name="requests/[id]" options={{ href: null }} />
      </Tabs>
    </View>
  );
}
