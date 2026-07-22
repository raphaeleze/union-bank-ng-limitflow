import * as LocalAuthentication from "expo-local-authentication";
import { useRouter } from "expo-router";
import { CheckCircle2, Fingerprint, ShieldCheck } from "lucide-react-native";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
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
import { cn } from "@/lib/utils";

type Step = "amount" | "review" | "otp" | "biometric" | "done";

const RESUMABLE_STEP: Partial<Record<LimitRequest["status"], Step>> = {
  OTP_PENDING: "otp",
  BIOMETRIC_PENDING: "biometric",
};

const WIZARD_STEPS: Step[] = ["amount", "review", "otp", "biometric"];

function WizardProgress({ step }: { step: Step }) {
  const currentIndex = WIZARD_STEPS.indexOf(step);
  if (currentIndex === -1) return null;

  return (
    <View className="gap-1.5">
      <Text className="text-xs font-medium text-ink-muted dark:text-ink-muted-dark">
        Step {currentIndex + 1} of {WIZARD_STEPS.length}
      </Text>
      <View className="flex-row gap-1.5">
        {WIZARD_STEPS.map((s, index) => (
          <View
            key={s}
            className={cn(
              "h-1 flex-1 rounded-full",
              index <= currentIndex ? "bg-accent dark:bg-accent-dark" : "bg-border dark:bg-border-dark",
            )}
          />
        ))}
      </View>
    </View>
  );
}

export default function IncreaseLimitScreen() {
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
  const [biometricError, setBiometricError] = useState<string | null>(null);

  const submitMutation = useSubmitLimitRequestMutation();
  const otpMutation = useVerifyOtpMutation();
  const biometricMutation = useVerifyBiometricMutation();
  const cancelMutation = useCancelLimitRequestMutation();

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
    return (
      <View className="flex-1 items-center justify-center bg-surface dark:bg-surface-dark">
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Loading…</Text>
      </View>
    );
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
    setBiometricError(null);
    const authResult = await LocalAuthentication.authenticateAsync({
      promptMessage: "Confirm it's you",
      fallbackLabel: "Use passcode",
    });
    if (!authResult.success) {
      setBiometricError("Biometric confirmation was cancelled or failed.");
      return;
    }
    try {
      const result = await biometricMutation.mutateAsync({ requestId: effectiveRequest.id, success: true });
      setRequest(result);
      setStep("done");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Biometric verification failed.", "error");
    }
  }

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Increase your limit</Text>

      <WizardProgress step={effectiveStep} />

      {effectiveStep === "amount" && (
        <Card>
          <CardContent className="gap-4">
            <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">
              Current daily limit: <Text className="font-medium text-ink dark:text-ink-dark">{formatCurrency(current.dailyLimit)}</Text>
            </Text>

            <View className="gap-1.5">
              <Label>New daily limit (₦)</Label>
              <Input
                keyboardType="numeric"
                value={requestedLimit}
                onChangeText={setRequestedLimit}
                placeholder={`More than ${current.dailyLimit}`}
                invalid={Boolean(requestedLimit) && !amountValid}
              />
              {requestedLimit.length > 0 && !amountValid && (
                <Text className="text-xs text-danger dark:text-danger-dark">Must be more than your current limit.</Text>
              )}
            </View>

            <View className="gap-1.5">
              <Label>Reason for increase</Label>
              <Textarea value={reason} onChangeText={setReason} placeholder="e.g. Paying a supplier for a large order" />
            </View>

            <Pressable className="flex-row items-center gap-2" onPress={() => setNewDevice((v) => !v)}>
              <Checkbox checked={newDevice} onValueChange={setNewDevice} />
              <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">I'm on a new or unrecognized device</Text>
            </Pressable>

            <Button disabled={!amountValid || reason.trim().length === 0} onPress={() => setStep("review")}>
              Continue
            </Button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "review" && (
        <Card>
          <CardContent className="gap-4">
            <View className="flex-row items-center justify-between">
              <Text className="text-sm font-medium text-ink dark:text-ink-dark">Review your request</Text>
              <Pressable onPress={() => setStep("amount")}>
                <Text className="text-sm font-medium text-accent dark:text-accent-dark">Edit</Text>
              </Pressable>
            </View>
            <View className="gap-2">
              <View className="flex-row justify-between">
                <Text className="text-ink-muted dark:text-ink-muted-dark">New limit</Text>
                <Text className="font-medium text-ink dark:text-ink-dark">{formatCurrency(amount)}</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-ink-muted dark:text-ink-muted-dark">Reason</Text>
                <Text className="max-w-[60%] text-right font-medium text-ink dark:text-ink-dark">{reason}</Text>
              </View>
              <View className="flex-row justify-between">
                <Text className="text-ink-muted dark:text-ink-muted-dark">Device</Text>
                <Text className="font-medium text-ink dark:text-ink-dark">{newDevice ? "New device" : "Trusted device"}</Text>
              </View>
            </View>
            <Button loading={submitMutation.isPending} onPress={handleSubmit}>
              {submitMutation.isPending ? "Submitting…" : "Confirm and submit"}
            </Button>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "otp" && (
        <Card>
          <CardContent className="items-center gap-4">
            <ShieldCheck color="#5b3df5" size={40} />
            <View className="items-center">
              <Text className="font-medium text-ink dark:text-ink-dark">Enter the verification code</Text>
              <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
                Sent to your registered number. Check Notifications for the demo code.
              </Text>
            </View>
            <Input
              value={otpCode}
              onChangeText={setOtpCode}
              placeholder="6-digit code"
              keyboardType="numeric"
              className="w-full text-center text-lg tracking-widest"
            />
            <Button disabled={otpCode.length === 0} loading={otpMutation.isPending} onPress={handleOtpVerify}>
              {otpMutation.isPending ? "Verifying…" : "Verify code"}
            </Button>
            <Pressable onPress={() => setCancelConfirmOpen(true)}>
              <Text className="text-sm font-medium text-danger dark:text-danger-dark">Cancel request</Text>
            </Pressable>
          </CardContent>
        </Card>
      )}

      {effectiveStep === "biometric" && (
        <Card>
          <CardContent className="items-center gap-4">
            <Fingerprint color="#5b3df5" size={40} />
            <View className="items-center">
              <Text className="font-medium text-ink dark:text-ink-dark">Confirm it's you</Text>
              <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
                Use your fingerprint or face to finish verifying this request.
              </Text>
            </View>
            {biometricError && <Text className="text-xs text-danger dark:text-danger-dark">{biometricError}</Text>}
            <Button loading={biometricMutation.isPending} onPress={handleBiometricConfirm}>
              {biometricMutation.isPending ? "Confirming…" : "Confirm biometric"}
            </Button>
            <Pressable onPress={() => setCancelConfirmOpen(true)}>
              <Text className="text-sm font-medium text-danger dark:text-danger-dark">Cancel request</Text>
            </Pressable>
          </CardContent>
        </Card>
      )}

      <Dialog open={cancelConfirmOpen} onOpenChange={setCancelConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel this request?</DialogTitle>
            <DialogDescription>
              You'll need to start a new request from scratch if you change your mind. This can't be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onPress={() => setCancelConfirmOpen(false)}>
              Keep request
            </Button>
            <Button variant="destructive" loading={cancelMutation.isPending} onPress={handleCancel}>
              {cancelMutation.isPending ? "Cancelling…" : "Cancel request"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {step === "done" && request && (
        <Card>
          <CardContent className="items-center gap-4">
            <CheckCircle2 color="#12b76a" size={40} />
            <View className="items-center">
              <Text className="font-medium text-ink dark:text-ink-dark">
                {request.status === "APPROVED" ? "Limit increased" : "Request submitted for review"}
              </Text>
              <Text className="text-center text-sm text-ink-muted dark:text-ink-muted-dark">
                {request.status === "APPROVED"
                  ? `Your new daily limit is ${formatCurrency(request.requestedLimit)}.`
                  : "We'll notify you once a review is complete."}
              </Text>
            </View>
            <Button onPress={() => router.replace(`/requests/${request.id}`)}>View request status</Button>
          </CardContent>
        </Card>
      )}
    </ScrollView>
  );
}
