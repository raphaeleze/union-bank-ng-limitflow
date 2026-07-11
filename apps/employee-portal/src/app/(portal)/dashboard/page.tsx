"use client";

import { formatDistanceToNow } from "date-fns";
import { CheckCircle2, Clock, ListChecks, XCircle } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { StatCard } from "@/components/dashboard/stat-card";
import { useMetrics } from "@/hooks/use-metrics";
import { useAuth } from "@/lib/auth";

export default function DashboardPage() {
  const { user } = useAuth();
  const { metrics, isLoading, isError, refetch } = useMetrics();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          Welcome back, {user?.firstName}
        </h1>
        <p className="text-sm text-slate-500">
          {user?.role === "MANAGER"
            ? "Here's what's waiting in the high-risk queue."
            : "Here's what's waiting in your review queue."}
        </p>
      </div>

      {isError ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-slate-500">We couldn&apos;t load your metrics.</p>
            <button onClick={refetch} className="text-sm font-medium text-blue-600">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-24" />)
            ) : (
              <>
                <StatCard label="Pending requests" value={metrics.pendingRequests} icon={ListChecks} />
                <StatCard
                  label="Approved today"
                  value={metrics.approvedToday}
                  icon={CheckCircle2}
                  accent="text-emerald-600"
                />
                <StatCard
                  label="Rejected today"
                  value={metrics.rejectedToday}
                  icon={XCircle}
                  accent="text-red-600"
                />
                <StatCard label="Avg. resolution time" value={metrics.averageResolutionTime} icon={Clock} />
              </>
            )}
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Recent activity</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Skeleton key={i} className="h-10" />
                  ))}
                </div>
              ) : metrics.recentActivity.length === 0 ? (
                <p className="text-sm text-slate-500">No activity yet.</p>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {metrics.recentActivity.map((entry) => (
                    <li key={entry.id} className="flex items-center justify-between py-3 text-sm">
                      <div>
                        <span className="font-medium text-slate-900">{entry.actorName}</span>{" "}
                        <span className="text-slate-500">{entry.action.replaceAll("_", " ").toLowerCase()}</span>
                      </div>
                      <span className="text-xs text-slate-500">
                        {formatDistanceToNow(new Date(entry.createdAt), { addSuffix: true })}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
