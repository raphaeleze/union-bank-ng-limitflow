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
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-accent to-accent-deep p-5 text-white shadow-lg shadow-accent/20">
      <div
        aria-hidden
        className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10 blur-2xl"
      />
      <p className="text-sm text-white/70">Daily transfer limit</p>
      <p className="font-tabular mt-1 text-3xl font-semibold">{formatCurrency(dailyLimit)}</p>

      <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-white/20">
        <div className="h-full rounded-full bg-white transition-[width]" style={{ width: `${usedPct}%` }} />
      </div>
      <div className="font-tabular mt-2 flex justify-between text-xs text-white/70">
        <span>{formatCurrency(usedToday)} used today</span>
        <span>{formatCurrency(remaining)} remaining</span>
      </div>
    </div>
  );
}
