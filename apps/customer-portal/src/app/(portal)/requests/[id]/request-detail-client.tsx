"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { Timeline } from "@/components/requests/timeline";
import { StatusBadge } from "@/components/requests/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { useCancelLimitRequestMutation } from "@/hooks/use-limit-request";
import { useRequestDetailQuery } from "@/hooks/use-request-detail";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/currency";
import { isActiveStatus, type RequestStatus } from "@/lib/types";

const RESUMABLE_STATUSES: RequestStatus[] = ["OTP_PENDING", "BIOMETRIC_PENDING"];

export function RequestDetailClient({ requestId }: { requestId: string }) {
  const router = useRouter();
  const { toast } = useToast();
  const { data: request, isLoading, isError, refetch } = useRequestDetailQuery(requestId);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const cancelMutation = useCancelLimitRequestMutation();

  async function handleCancel() {
    try {
      await cancelMutation.mutateAsync(requestId);
      setCancelConfirmOpen(false);
      toast("Request cancelled.");
      refetch();
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't cancel this request.", "error");
    }
  }

  return (
    <div className="space-y-4">
      <Link href="/requests" className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <ArrowLeft className="h-4 w-4" />
        Back to requests
      </Link>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-48" />
        </div>
      ) : isError || !request ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load this request.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent className="p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm text-ink-muted">Requested limit</p>
                  <p className="font-tabular text-2xl font-semibold text-ink">{formatCurrency(request.requestedLimit)}</p>
                  <p className="font-tabular mt-1 text-sm text-ink-muted">Current limit: {formatCurrency(request.currentLimit)}</p>
                </div>
                <StatusBadge status={request.status} />
              </div>
              <div className="mt-4 border-t border-border pt-4">
                <p className="text-sm text-ink-muted">Reason given</p>
                <p className="mt-1 text-sm text-ink">{request.reason}</p>
              </div>

              {RESUMABLE_STATUSES.includes(request.status) && (
                <div className="mt-4 border-t border-border pt-4">
                  <Button className="w-full" onClick={() => router.push("/increase-limit")}>
                    Resume verification
                  </Button>
                </div>
              )}

              {isActiveStatus(request.status) && (
                <div className={RESUMABLE_STATUSES.includes(request.status) ? "mt-2 text-center" : "mt-4 border-t border-border pt-4 text-center"}>
                  <button
                    type="button"
                    onClick={() => setCancelConfirmOpen(true)}
                    className="text-sm font-medium text-danger"
                  >
                    Cancel request
                  </button>
                </div>
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
              You&apos;ll need to start a new request from scratch if you change your mind. This can&apos;t be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setCancelConfirmOpen(false)}>
              Keep request
            </Button>
            <Button variant="destructive" disabled={cancelMutation.isPending} onClick={handleCancel}>
              {cancelMutation.isPending ? "Cancelling…" : "Cancel request"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
