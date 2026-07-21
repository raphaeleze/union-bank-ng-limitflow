"use client";

import { ArrowUpCircle } from "lucide-react";
import Link from "next/link";

import { ActiveRequestBanner } from "@/components/dashboard/active-request-banner";
import { LimitSummaryCard } from "@/components/dashboard/limit-summary-card";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import { useAuth } from "@/lib/auth";

export default function DashboardPage() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useCurrentLimitQuery();

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-ink">Hi, {user?.firstName}</h1>
        <p className="text-sm text-ink-muted">Here&apos;s your account at a glance.</p>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-32" />
          <Skeleton className="h-16" />
        </div>
      ) : isError || !data ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load your account.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : (
        <>
          <LimitSummaryCard dailyLimit={data.dailyLimit} usedToday={data.usedToday} remaining={data.remaining} />

          {data.activeRequest ? (
            <ActiveRequestBanner request={data.activeRequest} />
          ) : (
            <Button asChild size="lg" className="w-full">
              <Link href="/increase-limit">
                <ArrowUpCircle className="h-4 w-4" />
                Increase my limit
              </Link>
            </Button>
          )}
        </>
      )}
    </div>
  );
}
