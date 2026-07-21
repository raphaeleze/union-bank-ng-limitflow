import Link from "next/link";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function ActiveRequestBanner({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`}>
      <div className="flex items-center justify-between gap-3 rounded-2xl border border-accent/20 bg-accent-soft p-4 transition-colors hover:bg-accent-soft/70">
        <div>
          <p className="font-tabular text-sm font-medium text-ink">
            Request for {formatCurrency(request.requestedLimit)}
          </p>
          <p className="text-xs text-ink-muted">Tap to see progress</p>
        </div>
        <StatusBadge status={request.status} />
      </div>
    </Link>
  );
}
