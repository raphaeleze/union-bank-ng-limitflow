import { ArrowUpCircle } from "lucide-react-native";
import { Link } from "expo-router";
import { Pressable, ScrollView, Text, View } from "react-native";

import { ActiveRequestBanner } from "@/components/dashboard/active-request-banner";
import { LimitSummaryCard } from "@/components/dashboard/limit-summary-card";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import { useAuth } from "@/lib/auth";

export default function DashboardScreen() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useCurrentLimitQuery();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <View>
        <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Hi, {user?.firstName}</Text>
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Here's your account at a glance.</Text>
      </View>

      {isLoading ? (
        <View className="gap-4">
          <Skeleton className="h-32" />
          <Skeleton className="h-16" />
        </View>
      ) : isError || !data ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load your account.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : (
        <>
          <LimitSummaryCard dailyLimit={data.dailyLimit} usedToday={data.usedToday} remaining={data.remaining} />

          {data.activeRequest ? (
            <ActiveRequestBanner request={data.activeRequest} />
          ) : (
            <Link href="/increase-limit" asChild>
              <Pressable className="h-11 flex-row items-center justify-center gap-2 rounded-lg bg-accent dark:bg-accent-dark">
                <ArrowUpCircle color="#ffffff" size={16} />
                <Text className="text-sm font-medium text-white">Increase my limit</Text>
              </Pressable>
            </Link>
          )}
        </>
      )}
    </ScrollView>
  );
}
