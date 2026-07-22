import { formatDistanceToNow } from "date-fns";
import { Link } from "expo-router";
import { Pressable, Text, View } from "react-native";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function RequestListItem({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`} asChild>
      <Pressable className="flex-row items-center justify-between gap-3 py-3">
        <View>
          <Text className="text-sm font-medium text-ink dark:text-ink-dark">{formatCurrency(request.requestedLimit)}</Text>
          <Text className="text-xs text-ink-muted dark:text-ink-muted-dark">
            {formatDistanceToNow(new Date(request.createdAt), { addSuffix: true })}
          </Text>
        </View>
        <StatusBadge status={request.status} />
      </Pressable>
    </Link>
  );
}
