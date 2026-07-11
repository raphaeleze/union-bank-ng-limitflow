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

export interface AccountSummary {
  id: string;
  accountNumber: string;
  dailyLimit: number;
  usedToday: number;
  remaining: number;
  status: "ACTIVE" | "SUSPENDED";
}

export interface CurrentLimitResponse {
  accountId: string;
  dailyLimit: number;
  usedToday: number;
  remaining: number;
  activeRequest: LimitRequest | null;
}

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

const ACTIVE_STATUSES: RequestStatus[] = ["PENDING", "OTP_PENDING", "BIOMETRIC_PENDING", "UNDER_REVIEW"];

export function isActiveStatus(status: RequestStatus): boolean {
  return ACTIVE_STATUSES.includes(status);
}
