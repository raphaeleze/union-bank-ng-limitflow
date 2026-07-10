import { isToday } from "date-fns";
import { useMemo } from "react";

import type { AuditLogEntry } from "@/lib/types";
import { useAuditQuery } from "./use-audit";
import { useQueueQuery } from "./use-queue";

const APPROVED_ACTIONS = new Set(["LIMIT_APPROVED", "MANUAL_APPROVED"]);
const REJECTED_ACTIONS = new Set(["MANUAL_REJECTED"]);
const RESOLVED_ACTIONS = new Set([...APPROVED_ACTIONS, ...REJECTED_ACTIONS]);

function averageResolutionMinutes(auditEntries: AuditLogEntry[]): number | null {
  const requestedAt = new Map<string, number>();
  const durations: number[] = [];

  // Audit entries come back newest-first; walk oldest-first so the "requested"
  // timestamp for an entity is always seen before its resolution.
  const chronological = [...auditEntries].reverse();

  for (const entry of chronological) {
    if (entry.entityType !== "LimitRequest" || !entry.entityId) continue;
    const timestamp = new Date(entry.createdAt).getTime();

    if (entry.action === "LIMIT_REQUESTED") {
      requestedAt.set(entry.entityId, timestamp);
    } else if (RESOLVED_ACTIONS.has(entry.action)) {
      const start = requestedAt.get(entry.entityId);
      if (start !== undefined) {
        durations.push((timestamp - start) / 60_000);
      }
    }
  }

  if (durations.length === 0) return null;
  return durations.reduce((sum, value) => sum + value, 0) / durations.length;
}

function formatMinutes(minutes: number): string {
  if (minutes < 1) return "< 1 min";
  if (minutes < 60) return `${Math.round(minutes)} min`;
  const hours = Math.floor(minutes / 60);
  const remainder = Math.round(minutes % 60);
  return remainder > 0 ? `${hours}h ${remainder}m` : `${hours}h`;
}

export function useMetrics() {
  const queue = useQueueQuery();
  const audit = useAuditQuery();

  const metrics = useMemo(() => {
    const auditEntries = audit.data ?? [];
    const todaysEntries = auditEntries.filter((entry) => isToday(new Date(entry.createdAt)));

    const approvedToday = todaysEntries.filter((entry) => APPROVED_ACTIONS.has(entry.action)).length;
    const rejectedToday = todaysEntries.filter((entry) => REJECTED_ACTIONS.has(entry.action)).length;
    const averageMinutes = averageResolutionMinutes(auditEntries);

    return {
      pendingRequests: queue.data?.length ?? 0,
      approvedToday,
      rejectedToday,
      averageResolutionTime: averageMinutes === null ? "—" : formatMinutes(averageMinutes),
      recentActivity: auditEntries.slice(0, 8),
    };
  }, [queue.data, audit.data]);

  return {
    metrics,
    isLoading: queue.isLoading || audit.isLoading,
    isError: queue.isError || audit.isError,
    refetch: () => {
      queue.refetch();
      audit.refetch();
    },
  };
}
