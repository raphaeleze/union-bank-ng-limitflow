"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";

import { Timeline } from "@/components/requests/timeline";
import { StatusBadge } from "@/components/requests/status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useRequestDetailQuery } from "@/hooks/use-request-detail";
import { formatCurrency } from "@/lib/currency";

export function RequestDetailClient({ requestId }: { requestId: string }) {
  const { data: request, isLoading, isError, refetch } = useRequestDetailQuery(requestId);

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
    </div>
  );
}
