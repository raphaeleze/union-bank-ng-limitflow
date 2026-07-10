"use client";

import { format } from "date-fns";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useAuditQuery } from "@/hooks/use-audit";

export default function AuditPage() {
  const { data, isLoading, isError, refetch } = useAuditQuery();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Audit log</h1>
        <p className="text-sm text-slate-500">Every action taken across the LimitFlow journey.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>All events</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-10" />
              ))}
            </div>
          ) : isError ? (
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">We couldn&apos;t load the audit log.</p>
              <button onClick={() => refetch()} className="text-sm font-medium text-blue-600">
                Try again
              </button>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Actor</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead>When</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(data ?? []).map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell className="font-medium">{entry.actorName}</TableCell>
                    <TableCell>{entry.action.replaceAll("_", " ").toLowerCase()}</TableCell>
                    <TableCell className="text-slate-500">
                      {entry.entityType}
                      {entry.entityId ? ` #${entry.entityId.slice(0, 8)}` : ""}
                    </TableCell>
                    <TableCell className="text-slate-500">
                      {format(new Date(entry.createdAt), "MMM d, yyyy h:mm a")}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
