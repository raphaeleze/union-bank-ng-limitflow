import { formatDistanceToNow } from "date-fns";
import { Bell, CheckCircle2, MessageCircle, ShieldCheck, XCircle, type LucideIcon } from "lucide-react-native";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useNotificationsQuery } from "@/hooks/use-notifications";
import { cn } from "@/lib/utils";

const ICONS: Record<string, LucideIcon> = {
  OTP_SENT: ShieldCheck,
  VERIFICATION_COMPLETED: ShieldCheck,
  VERIFICATION_REQUESTED: ShieldCheck,
  LIMIT_APPROVED: CheckCircle2,
  LIMIT_REJECTED: XCircle,
  SUPPORT_COMMENT: MessageCircle,
};

export default function NotificationsScreen() {
  const { data, isLoading, isError, refetch } = useNotificationsQuery();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Notifications</Text>

      {isLoading ? (
        <View className="gap-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-14" />
          ))}
        </View>
      ) : isError ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load your notifications.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="items-center gap-2">
            <Bell size={32} color="#ece8f6" />
            <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">Nothing here yet.</Text>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="gap-0">
            {data.map((item) => {
              const Icon = ICONS[item.type] ?? Bell;
              return (
                <View key={item.id} className="flex-row gap-3 py-3">
                  <View
                    className={cn(
                      "h-9 w-9 items-center justify-center rounded-full",
                      item.read ? "bg-border dark:bg-border-dark" : "bg-accent-soft dark:bg-accent-soft-dark",
                    )}
                  >
                    <Icon size={16} color={item.read ? "#6b6580" : "#5b3df5"} />
                  </View>
                  <View className="flex-1">
                    <Text className="text-sm font-medium text-ink dark:text-ink-dark">{item.title}</Text>
                    <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{item.message}</Text>
                    <Text className="mt-1 text-xs text-ink-muted dark:text-ink-muted-dark">
                      {formatDistanceToNow(new Date(item.createdAt), { addSuffix: true })}
                    </Text>
                  </View>
                </View>
              );
            })}
          </CardContent>
        </Card>
      )}
    </ScrollView>
  );
}
