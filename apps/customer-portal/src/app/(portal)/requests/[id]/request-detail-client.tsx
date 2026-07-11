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
      <Link href="/requests" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700">
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
            <p className="text-sm text-slate-500">We couldn&apos;t load this request.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-blue-600">
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
                  <p className="text-sm text-slate-500">Requested limit</p>
                  <p className="text-2xl font-semibold text-slate-900">{formatCurrency(request.requestedLimit)}</p>
                  <p className="mt-1 text-sm text-slate-500">Current limit: {formatCurrency(request.currentLimit)}</p>
                </div>
                <StatusBadge status={request.status} />
              </div>
              <div className="mt-4 border-t border-slate-100 pt-4">
                <p className="text-sm text-slate-500">Reason given</p>
                <p className="mt-1 text-sm text-slate-800">{request.reason}</p>
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
