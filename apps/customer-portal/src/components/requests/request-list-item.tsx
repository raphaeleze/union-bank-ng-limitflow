import { formatDistanceToNow } from "date-fns";
import Link from "next/link";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function RequestListItem({ request }: { request: LimitRequest }) {
  return (
    <Link
      href={`/requests/${request.id}`}
      className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
    >
      <div>
        <p className="font-tabular text-sm font-medium text-ink">{formatCurrency(request.requestedLimit)}</p>
        <p className="text-xs text-ink-muted">
          {formatDistanceToNow(new Date(request.createdAt), { addSuffix: true })}
        </p>
      </div>
      <StatusBadge status={request.status} />
    </Link>
  );
}
