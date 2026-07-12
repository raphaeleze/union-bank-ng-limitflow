"use client";

import { formatDistanceToNow } from "date-fns";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { NotesPanel } from "@/components/queue/notes-panel";
import { RiskBadge } from "@/components/queue/risk-badge";
import { ReviewActions } from "@/components/queue/review-actions";
import { StatusBadge } from "@/components/queue/status-badge";
import { Timeline } from "@/components/queue/timeline";
import { useAuditQuery } from "@/hooks/use-audit";
import { useRequestDetailQuery } from "@/hooks/use-request-detail";
import { formatCurrency } from "@/lib/currency";

export function RequestDetailClient({ requestId }: { requestId: string }) {
  const { data: request, isLoading, isError, refetch } = useRequestDetailQuery(requestId);
  const { data: auditEntries } = useAuditQuery();

  const relatedAudit = (auditEntries ?? []).filter((entry) => entry.entityId === requestId);

  return (
    <div className="space-y-6">
      <Link href="/queue" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700">
        <ArrowLeft className="h-4 w-4" />
        Back to queue
      </Link>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-32" />
          <Skeleton className="h-64" />
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
            <CardContent className="p-6">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-sm text-slate-500">Requested limit</p>
                  <p className="text-2xl font-semibold text-slate-900">
                    {formatCurrency(request.requestedLimit)}
                  </p>
                  <p className="mt-1 text-sm text-slate-500">
                    Current limit: {formatCurrency(request.currentLimit)}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-2">
                  <StatusBadge status={request.status} />
                  <RiskBadge riskLevel={request.riskLevel} />
                </div>
              </div>

              <div className="mt-4 border-t border-slate-100 pt-4">
                <p className="text-sm text-slate-500">Reason given</p>
                <p className="mt-1 text-sm text-slate-800">{request.reason}</p>
              </div>

              {request.status === "UNDER_REVIEW" && (
                <div className="mt-6 border-t border-slate-100 pt-6">
                  <ReviewActions requestId={request.id} />
                </div>
              )}
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Timeline</CardTitle>
              </CardHeader>
              <CardContent>
                <Timeline steps={request.timeline} />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Support notes</CardTitle>
              </CardHeader>
              <CardContent>
                <NotesPanel requestId={request.id} />
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Audit trail</CardTitle>
            </CardHeader>
            <CardContent>
              {relatedAudit.length === 0 ? (
                <p className="text-sm text-slate-500">No audit events for this request yet.</p>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {relatedAudit.map((entry) => (
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
