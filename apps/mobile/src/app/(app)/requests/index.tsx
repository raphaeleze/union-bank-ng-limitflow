import { Pressable, ScrollView, Text, View } from "react-native";

import { RequestListItem } from "@/components/requests/request-list-item";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useHistoryQuery } from "@/hooks/use-history";

export default function RequestsScreen() {
  const { data, isLoading, isError, refetch } = useHistoryQuery();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Your requests</Text>

      {isLoading ? (
        <View className="gap-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </View>
      ) : isError ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load your requests.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent>
            <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
              You haven't requested a limit increase yet.
            </Text>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="divide-y divide-border dark:divide-border-dark">
            {data.map((request) => (
              <RequestListItem key={request.id} request={request} />
            ))}
          </CardContent>
        </Card>
      )}
    </ScrollView>
  );
}
