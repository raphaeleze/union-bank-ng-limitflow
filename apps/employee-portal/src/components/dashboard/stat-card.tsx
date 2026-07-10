import type { LucideIcon } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";

export function StatCard({
  label,
  value,
  icon: Icon,
  accent = "text-slate-900",
}: {
  label: string;
  value: string | number;
  icon: LucideIcon;
  accent?: string;
}) {
  return (
    <Card>
      <CardContent className="flex items-center justify-between p-5">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className={`mt-1 text-2xl font-semibold ${accent}`}>{value}</p>
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100">
          <Icon className="h-5 w-5 text-slate-500" />
        </div>
      </CardContent>
    </Card>
  );
}
