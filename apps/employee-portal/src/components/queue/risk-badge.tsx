import { Badge } from "@/components/ui/badge";
import type { RiskLevel } from "@/lib/types";

const VARIANTS: Record<RiskLevel, "green" | "orange" | "red"> = {
  LOW: "green",
  MEDIUM: "orange",
  HIGH: "red",
};

export function RiskBadge({ riskLevel }: { riskLevel: RiskLevel | null }) {
  if (!riskLevel) return null;
  return <Badge variant={VARIANTS[riskLevel]}>{riskLevel} RISK</Badge>;
}
