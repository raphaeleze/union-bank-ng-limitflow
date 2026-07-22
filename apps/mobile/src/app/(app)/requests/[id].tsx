import { useLocalSearchParams, useRouter } from "expo-router";
import { ArrowLeft } from "lucide-react-native";
import { useState } from "react";
import { Pressable, ScrollView, Text, View } from "react-native";

import { StatusBadge } from "@/components/requests/status-badge";
import { Timeline } from "@/components/requests/timeline";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { useCancelLimitRequestMutation } from "@/hooks/use-limit-request";
import { useRequestDetailQuery } from "@/hooks/use-request-detail";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/currency";
import { isActiveStatus, type RequestStatus } from "@/lib/types";

const RESUMABLE_STATUSES: RequestStatus[] = ["OTP_PENDING", "BIOMETRIC_PENDING"];

export default function RequestDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const { data: request, isLoading, isError, refetch } = useRequestDetailQuery(id);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const cancelMutation = useCancelLimitRequestMutation();

  async function handleCancel() {
    try {
      await cancelMutation.mutateAsync(id);
      setCancelConfirmOpen(false);
      toast("Request cancelled.");
      refetch();
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't cancel this request.", "error");
    }
  }

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Pressable className="flex-row items-center gap-1" onPress={() => router.back()}>
        <ArrowLeft size={16} color="#6b6580" />
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Back to requests</Text>
      </Pressable>

      {isLoading ? (
        <View className="gap-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-48" />
        </View>
      ) : isError || !request ? (
        <Card>
          <CardContent className="flex-row items-center justify-between">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">We couldn't load this request.</Text>
            <Pressable onPress={() => refetch()}>
              <Text className="text-sm font-medium text-accent dark:text-accent-dark">Try again</Text>
            </Pressable>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent>
              <View className="flex-row items-start justify-between gap-4">
                <View>
                  <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Requested limit</Text>
                  <Text className="text-2xl font-semibold text-ink dark:text-ink-dark">{formatCurrency(request.requestedLimit)}</Text>
                  <Text className="mt-1 text-sm text-ink-muted dark:text-ink-muted-dark">
                    Current limit: {formatCurrency(request.currentLimit)}
                  </Text>
                </View>
                <StatusBadge status={request.status} />
              </View>
              <View className="mt-4 border-t border-border pt-4 dark:border-border-dark">
                <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Reason given</Text>
                <Text className="mt-1 text-sm text-ink dark:text-ink-dark">{request.reason}</Text>
              </View>

              {RESUMABLE_STATUSES.includes(request.status) && (
                <View className="mt-4 border-t border-border pt-4 dark:border-border-dark">
                  <Button onPress={() => router.push("/increase-limit")}>Resume verification</Button>
                </View>
              )}

              {isActiveStatus(request.status) && (
                <View className="mt-2 items-center">
                  <Pressable onPress={() => setCancelConfirmOpen(true)}>
                    <Text className="text-sm font-medium text-danger dark:text-danger-dark">Cancel request</Text>
                  </Pressable>
                </View>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Progress</CardTitle>
            </CardHeader>
            <CardContent>
              <Timeline steps={request.timeline} />
            </CardContent>
          </Card>
        </>
      )}

      <Dialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel this request?</DialogTitle>
            <DialogDescription>
              You'll need to start a new request from scratch if you change your mind. This can't be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onPress={() => setCancelConfirmOpen(false)}>
              Keep request
            </Button>
            <Button variant="destructive" loading={cancelMutation.isPending} onPress={handleCancel}>
              {cancelMutation.isPending ? "Cancelling…" : "Cancel request"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ScrollView>
  );
}
