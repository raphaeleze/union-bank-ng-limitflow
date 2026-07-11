import { Card, CardContent } from "@/components/ui/card";
import { formatCurrency } from "@/lib/currency";

export function LimitSummaryCard({
  dailyLimit,
  usedToday,
  remaining,
}: {
  dailyLimit: number;
  usedToday: number;
  remaining: number;
}) {
  const usedPct = dailyLimit > 0 ? Math.min(100, Math.round((usedToday / dailyLimit) * 100)) : 0;

  return (
    <Card>
      <CardContent className="p-5">
        <p className="text-sm text-slate-500">Daily transfer limit</p>
        <p className="mt-1 text-3xl font-semibold text-slate-900">{formatCurrency(dailyLimit)}</p>

        <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-slate-100">
          <div className="h-full rounded-full bg-blue-600" style={{ width: `${usedPct}%` }} />
        </div>
        <div className="mt-2 flex justify-between text-xs text-slate-500">
          <span>{formatCurrency(usedToday)} used today</span>
          <span>{formatCurrency(remaining)} remaining</span>
        </div>
      </CardContent>
    </Card>
  );
}
