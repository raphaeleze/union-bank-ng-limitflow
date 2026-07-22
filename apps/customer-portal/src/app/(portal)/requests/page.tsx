"use client";

import { Card, CardContent } from "@/components/ui/card";
import { RequestListItem } from "@/components/requests/request-list-item";
import { Skeleton } from "@/components/ui/skeleton";
import { useHistoryQuery } from "@/hooks/use-history";

export default function RequestsPage() {
  const { data, isLoading, isError, refetch } = useHistoryQuery();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Your requests</h1>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </div>
      ) : isError ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load your requests.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="p-6 text-center text-sm text-ink-muted">
            You haven&apos;t requested a limit increase yet.
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="divide-y divide-border p-5">
            {data.map((request) => (
              <RequestListItem key={request.id} request={request} />
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
