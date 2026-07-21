"use client";

import { CheckCircle2, Fingerprint, ShieldCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import {
  useCancelLimitRequestMutation,
  useSubmitLimitRequestMutation,
  useVerifyBiometricMutation,
  useVerifyOtpMutation,
} from "@/hooks/use-limit-request";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

type Step = "amount" | "review" | "otp" | "biometric" | "done";

const RESUMABLE_STEP: Partial<Record<LimitRequest["status"], Step>> = {
  OTP_PENDING: "otp",
  BIOMETRIC_PENDING: "biometric",
};

export default function IncreaseLimitPage() {
  const router = useRouter();
  const { toast } = useToast();
  const { data: current, isLoading } = useCurrentLimitQuery();

  const [step, setStep] = useState<Step>("amount");
  const [requestedLimit, setRequestedLimit] = useState("");
  const [reason, setReason] = useState("");
  const [newDevice, setNewDevice] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [request, setRequest] = useState<LimitRequest | null>(null);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);

  const submitMutation = useSubmitLimitRequestMutation();
  const otpMutation = useVerifyOtpMutation();
  const biometricMutation = useVerifyBiometricMutation();
  const cancelMutation = useCancelLimitRequestMutation();

  // Reachable directly via the bottom nav even with a request already in flight. A request
  // still awaiting OTP or biometric verification resumes right where it left off instead of
  // stranding the customer with no way back into the flow; anything else (already submitted
  // for review) has nothing left to resume, so send them to its status page instead.
  const activeRequest = current?.activeRequest ?? null;
  const resumableStep = activeRequest ? RESUMABLE_STEP[activeRequest.status] : undefined;
  const shouldRedirectToDetail = Boolean(activeRequest) && !resumableStep && !request;

  const effectiveRequest = request ?? (resumableStep ? activeRequest : null);
  const effectiveStep: Step = request ? step : (resumableStep ?? step);

  useEffect(() => {
    if (shouldRedirectToDetail) {
      router.replace(`/requests/${activeRequest!.id}`);
    }
  }, [shouldRedirectToDetail, activeRequest, router]);

  async function handleCancel() {
    if (!effectiveRequest) return;
    try {
      await cancelMutation.mutateAsync(effectiveRequest.id);
      setCancelConfirmOpen(false);
      setRequest(null);
      setOtpCode("");
      setStep("amount");
      toast("Request cancelled.");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't cancel this request.", "error");
    }
  }

  if (isLoading || !current || shouldRedirectToDetail) {
    return <p className="text-sm text-ink-muted">Loading…</p>;
  }

  const amount = Number(requestedLimit);
  const amountValid = amount > current.dailyLimit;

  async function handleSubmit() {
    try {
      const result = await submitMutation.mutateAsync({
        accountId: current!.accountId,
        requestedLimit: amount,
        reason,
        knownDevice: !newDevice,
      });
      setRequest(result);
      setStep("otp");
      toast("We sent a verification code. Check Notifications for the demo code.");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't submit your request.", "error");
    }
  }

  async function handleOtpVerify() {
    if (!effectiveRequest) return;
    try {
      const result = await otpMutation.mutateAsync({ requestId: effectiveRequest.id, code: otpCode });
      setRequest(result);
      setStep("biometric");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "That code didn't work.", "error");
    }
  }

  async function handleBiometricConfirm() {
    if (!effectiveRequest) return;
    try {
      const result = await biometricMutation.mutateAsync({ requestId: effectiveRequest.id, success: true });
      setRequest(result);
      setStep("done");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Biometric verification failed.", "error");
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Increase your limit</h1>

      {effectiveStep === "amount" && (
        <Card>
          <CardContent className="space-y-4 p-5">
            <p className="text-sm text-ink-muted">
              Current daily limit:{" "}
              <span className="font-tabular font-medium text-ink">{formatCurrency(current.dailyLimit)}</span>
            </p>

            <div className="space-y-1.5">
              <Label htmlFor="amount">New daily limit (₦)</Label>
              <Input
                id="amount"
                type="number"
                inputMode="numeric"
                min={current.dailyLimit + 1}
                value={requestedLimit}
                onChange={(e) => setRequestedLimit(e.target.value)}
                placeholder={`More than ${current.dailyLimit}`}
                className="font-tabular"
              />
              {requestedLimit && !amountValid && (
                <p className="text-xs text-danger">Must be more than your current limit.</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="reason">Reason for increase</Label>
              <Textarea
                id="reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="e.g. Paying a supplier for a large order"
                rows={3}
              />
            </div>

            <label className="flex items-center gap-2 text-sm text-ink-muted">
              <input
                type="checkbox"
                checked={newDevice}
                onChange={(e) => setNewDevice(e.target.checked)}
                className="h-4 w-4 rounded border-border accent-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
              />
              I&apos;m on a new or unrecognized device
            </label>

            <Button
              className="w-full"
              disabled={!amountValid || reason.trim().length === 0}
              onClick={() => setStep("review")}
            >
              Continue
            </Button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "review" && (
        <Card>
          <CardContent className="space-y-4 p-5">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-medium text-ink">Review your request</h2>
              <button
                type="button"
                onClick={() => setStep("amount")}
                className="text-sm font-medium text-accent"
              >
                Edit
              </button>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-ink-muted">New limit</span>
                <span className="font-tabular font-medium text-ink">{formatCurrency(amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink-muted">Reason</span>
                <span className="max-w-[60%] text-right font-medium text-ink">{reason}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink-muted">Device</span>
                <span className="font-medium text-ink">{newDevice ? "New device" : "Trusted device"}</span>
              </div>
            </div>
            <Button className="w-full" disabled={submitMutation.isPending} onClick={handleSubmit}>
              {submitMutation.isPending ? "Submitting…" : "Confirm and submit"}
            </Button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "otp" && (
        <Card>
          <CardContent className="space-y-4 p-5 text-center">
            <ShieldCheck className="mx-auto h-10 w-10 text-accent" />
            <div>
              <p className="font-medium text-ink">Enter the verification code</p>
              <p className="text-sm text-ink-muted">Sent to your registered number. Check Notifications for the demo code.</p>
            </div>
            <Input
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              placeholder="6-digit code"
              inputMode="numeric"
              className="font-tabular text-center text-lg tracking-widest"
            />
            <Button className="w-full" disabled={otpCode.length === 0 || otpMutation.isPending} onClick={handleOtpVerify}>
              {otpMutation.isPending ? "Verifying…" : "Verify code"}
            </Button>
            <button
              type="button"
              onClick={() => setCancelConfirmOpen(true)}
              className="text-sm font-medium text-danger"
            >
              Cancel request
            </button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "biometric" && (
        <Card>
          <CardContent className="space-y-4 p-5 text-center">
            <Fingerprint className="mx-auto h-10 w-10 text-accent" />
            <div>
              <p className="font-medium text-ink">Confirm it&apos;s you</p>
              <p className="text-sm text-ink-muted">Use your fingerprint or face to finish verifying this request.</p>
            </div>
            <Button className="w-full" disabled={biometricMutation.isPending} onClick={handleBiometricConfirm}>
              {biometricMutation.isPending ? "Confirming…" : "Confirm biometric"}
            </Button>
            <button
              type="button"
              onClick={() => setCancelConfirmOpen(true)}
              className="text-sm font-medium text-danger"
            >
              Cancel request
            </button>
          </CardContent>
        </Card>
      )}

      <Dialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel this request?</DialogTitle>
            <DialogDescription>
              You&apos;ll need to start a new request from scratch if you change your mind. This can&apos;t be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setCancelConfirmOpen(false)}>
              Keep request
            </Button>
            <Button variant="destructive" disabled={cancelMutation.isPending} onClick={handleCancel}>
              {cancelMutation.isPending ? "Cancelling…" : "Cancel request"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {step === "done" && request && (
        <Card>
          <CardContent className="space-y-4 p-5 text-center">
            <CheckCircle2 className="mx-auto h-10 w-10 text-success" />
            <div>
              <p className="font-medium text-ink">
                {request.status === "APPROVED" ? "Limit increased" : "Request submitted for review"}
              </p>
              <p className="text-sm text-ink-muted">
                {request.status === "APPROVED" ? (
                  <>
                    Your new daily limit is{" "}
                    <span className="font-tabular">{formatCurrency(request.requestedLimit)}</span>.
                  </>
                ) : (
                  "We'll notify you once a review is complete."
                )}
              </p>
            </div>
            <Button className="w-full" onClick={() => router.replace(`/requests/${request.id}`)}>
              View request status
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
