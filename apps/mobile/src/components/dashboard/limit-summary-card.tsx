import { LinearGradient } from "expo-linear-gradient";
import { Text, View } from "react-native";

import { formatCurrency } from "@/lib/currency";

export function LimitSummaryCard({
  dailyLimit,
  usedToday,
  remaining,
}: {
  dailyLimit: number;
  usedToday: number;
  remaining: number;
}) {
  const usedPct = dailyLimit > 0 ? Math.min(100, Math.round((usedToday / dailyLimit) * 100)) : 0;

  return (
    <LinearGradient
      colors={["#5b3df5", "#241653"]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={{ borderRadius: 16, padding: 20 }}
    >
      <Text className="text-sm text-white/70">Daily transfer limit</Text>
      <Text className="mt-1 text-3xl font-semibold text-white">{formatCurrency(dailyLimit)}</Text>

      <View className="mt-4 h-2 w-full overflow-hidden rounded-full bg-white/20">
        <View className="h-full rounded-full bg-white" style={{ width: `${usedPct}%` }} />
      </View>
      <View className="mt-2 flex-row justify-between">
        <Text className="text-xs text-white/70">{formatCurrency(usedToday)} used today</Text>
        <Text className="text-xs text-white/70">{formatCurrency(remaining)} remaining</Text>
      </View>
    </LinearGradient>
  );
}
