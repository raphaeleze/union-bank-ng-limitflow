import { LogOut } from "lucide-react-native";
import { Pressable, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { useAuth } from "@/lib/auth";

export function Header() {
  const { user, logout } = useAuth();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={{ paddingTop: insets.top }}
      className="flex-row items-center justify-between border-b border-border bg-card px-4 pb-3 dark:border-border-dark dark:bg-card-dark"
    >
      <View className="flex-row items-center gap-2">
        <View className="h-8 w-8 items-center justify-center rounded-xl bg-accent dark:bg-accent-dark">
          <Text className="text-xs font-bold text-white">LF</Text>
        </View>
        <Text className="text-sm font-medium text-ink dark:text-ink-dark">Hi, {user?.firstName}</Text>
      </View>
      <Pressable accessibilityLabel="Log out" onPress={logout}>
        <LogOut size={18} color="#6b6580" />
      </Pressable>
    </View>
  );
}
