import Link from "next/link";

import { StatusBadge } from "@/components/requests/status-badge";
import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function ActiveRequestBanner({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`}>
      <Card className="border-blue-200 bg-blue-50 transition-colors hover:bg-blue-100">
        <CardContent className="flex items-center justify-between p-4">
          <div>
            <p className="text-sm font-medium text-slate-900">
              Request for {formatCurrency(request.requestedLimit)}
            </p>
            <p className="text-xs text-slate-500">Tap to see progress</p>
          </div>
          <StatusBadge status={request.status} />
        </CardContent>
      </Card>
    </Link>
  );
}
