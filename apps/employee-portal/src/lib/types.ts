export type Role = "CUSTOMER" | "SUPPORT_AGENT" | "MANAGER";

export type RequestStatus =
  | "PENDING"
  | "OTP_PENDING"
  | "BIOMETRIC_PENDING"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";

export type TimelineStepStatus = "COMPLETE" | "CURRENT" | "PENDING";

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface LoginResponse {
  token: string;
  user: UserSummary;
}

export interface TimelineStep {
  label: string;
  status: TimelineStepStatus;
}

export interface LimitRequest {
  id: string;
  accountId: string;
  currentLimit: number;
  requestedLimit: number;
  reason: string;
  status: RequestStatus;
  riskLevel: RiskLevel | null;
  createdAt: string;
  updatedAt: string;
  timeline: TimelineStep[];
}

export interface SupportQueueItem {
  id: string;
  customerName: string;
  currentLimit: number;
  requestedLimit: number;
  riskLevel: RiskLevel | null;
  status: RequestStatus;
  createdAt: string;
}

export interface SupportNote {
  id: string;
  authorName: string;
  note: string;
  createdAt: string;
}

export interface AuditLogEntry {
  id: string;
  actorName: string;
  action: string;
  entityType: string;
  entityId: string | null;
  metadata: string | null;
  createdAt: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
