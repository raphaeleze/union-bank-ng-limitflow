"use client";

import { formatDistanceToNow } from "date-fns";
import { Bell, CheckCircle2, MessageCircle, ShieldCheck, XCircle } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useNotificationsQuery } from "@/hooks/use-notifications";
import { cn } from "@/lib/utils";

const ICONS: Record<string, LucideIcon> = {
  OTP_SENT: ShieldCheck,
  VERIFICATION_COMPLETED: ShieldCheck,
  VERIFICATION_REQUESTED: ShieldCheck,
  LIMIT_APPROVED: CheckCircle2,
  LIMIT_REJECTED: XCircle,
  SUPPORT_COMMENT: MessageCircle,
};

export default function NotificationsPage() {
  const { data, isLoading, isError, refetch } = useNotificationsQuery();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Notifications</h1>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-14" />
          ))}
        </div>
      ) : isError ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load your notifications.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="p-6 text-center text-sm text-ink-muted">
            <Bell className="mx-auto mb-2 h-8 w-8 text-border" />
            Nothing here yet.
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="divide-y divide-border p-5">
            {data.map((item) => {
              const Icon = ICONS[item.type] ?? Bell;
              return (
                <div key={item.id} className="flex gap-3 py-3 first:pt-0 last:pb-0">
                  <div
                    className={cn(
                      "flex h-9 w-9 shrink-0 items-center justify-center rounded-full",
                      item.read ? "bg-border text-ink-muted" : "bg-accent-soft text-accent",
                    )}
                  >
                    <Icon className="h-4 w-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink">{item.title}</p>
                    <p className="text-sm text-ink-muted">{item.message}</p>
                    <p className="mt-1 text-xs text-ink-muted">
                      {formatDistanceToNow(new Date(item.createdAt), { addSuffix: true })}
                    </p>
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
