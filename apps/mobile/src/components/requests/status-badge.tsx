import { Badge } from "@/components/ui/badge";
import type { RequestStatus } from "@/lib/types";

const LABELS: Record<RequestStatus, string> = {
  PENDING: "Pending",
  OTP_PENDING: "Verifying code",
  BIOMETRIC_PENDING: "Verifying identity",
  UNDER_REVIEW: "Under review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

const VARIANTS: Record<RequestStatus, "neutral" | "blue" | "green" | "orange" | "red"> = {
  PENDING: "blue",
  OTP_PENDING: "blue",
  BIOMETRIC_PENDING: "blue",
  UNDER_REVIEW: "orange",
  APPROVED: "green",
  REJECTED: "red",
  CANCELLED: "neutral",
};

export function StatusBadge({ status }: { status: RequestStatus }) {
  return <Badge variant={VARIANTS[status]}>{LABELS[status]}</Badge>;
}
