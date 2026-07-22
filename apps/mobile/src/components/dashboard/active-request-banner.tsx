import { Link } from "expo-router";
import { Pressable, Text, View } from "react-native";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function ActiveRequestBanner({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`} asChild>
      <Pressable className="flex-row items-center justify-between gap-3 rounded-2xl border border-accent/20 bg-accent-soft p-4 dark:bg-accent-soft-dark">
        <View>
          <Text className="text-sm font-medium text-ink dark:text-ink-dark">
            Request for {formatCurrency(request.requestedLimit)}
          </Text>
          <Text className="text-xs text-ink-muted dark:text-ink-muted-dark">Tap to see progress</Text>
        </View>
        <StatusBadge status={request.status} />
      </Pressable>
    </Link>
  );
}
