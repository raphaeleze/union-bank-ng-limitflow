"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { QueueTable } from "@/components/queue/queue-table";
import { useQueueQuery } from "@/hooks/use-queue";
import { useAuth } from "@/lib/auth";

export default function QueuePage() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useQueueQuery();

  const isManager = user?.role === "MANAGER";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Review queue</h1>
        <p className="text-sm text-slate-500">
          {isManager
            ? "High-risk requests escalated for manager review."
            : "Medium-risk requests awaiting a support decision."}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{isManager ? "High risk" : "Medium risk"}</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-12" />
              ))}
            </div>
          ) : isError ? (
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">We couldn&apos;t load the queue.</p>
              <button onClick={() => refetch()} className="text-sm font-medium text-blue-600">
                Try again
              </button>
            </div>
          ) : (
            <QueueTable items={data ?? []} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
