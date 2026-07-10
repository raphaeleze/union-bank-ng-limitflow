"use client";

import { formatDistanceToNow } from "date-fns";
import { useRouter } from "next/navigation";

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatCurrency } from "@/lib/currency";
import type { SupportQueueItem } from "@/lib/types";
import { RiskBadge } from "./risk-badge";
import { StatusBadge } from "./status-badge";

export function QueueTable({ items }: { items: SupportQueueItem[] }) {
  const router = useRouter();

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-16 text-center">
        <p className="text-sm font-medium text-slate-700">Queue is empty</p>
        <p className="text-sm text-slate-500">Nothing needs your review right now.</p>
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Customer</TableHead>
          <TableHead>Current limit</TableHead>
          <TableHead>Requested limit</TableHead>
          <TableHead>Risk</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Time waiting</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((item) => (
          <TableRow
            key={item.id}
            className="cursor-pointer"
            onClick={() => router.push(`/queue/${item.id}`)}
          >
            <TableCell className="font-medium">{item.customerName}</TableCell>
            <TableCell>{formatCurrency(item.currentLimit)}</TableCell>
            <TableCell>{formatCurrency(item.requestedLimit)}</TableCell>
            <TableCell>
              <RiskBadge riskLevel={item.riskLevel} />
            </TableCell>
            <TableCell>
              <StatusBadge status={item.status} />
            </TableCell>
            <TableCell className="text-slate-500">
              {formatDistanceToNow(new Date(item.createdAt), { addSuffix: true })}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
