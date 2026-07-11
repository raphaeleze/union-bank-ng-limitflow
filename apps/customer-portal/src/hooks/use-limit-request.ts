import { useMutation, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest } from "@/lib/types";

interface SubmitPayload {
  accountId: string;
  requestedLimit: number;
  reason: string;
  knownDevice: boolean;
}

function invalidateLimits(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: ["limits"] });
}

export function useSubmitLimitRequestMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: SubmitPayload) => {
      const response = await apiClient.post<LimitRequest>("/limits/request", payload);
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}

export function useVerifyOtpMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ requestId, code }: { requestId: string; code: string }) => {
      const response = await apiClient.post<LimitRequest>(`/limits/${requestId}/otp/verify`, { code });
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}

export function useVerifyBiometricMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ requestId, success }: { requestId: string; success: boolean }) => {
      const response = await apiClient.post<LimitRequest>(`/limits/${requestId}/biometric/verify`, {
        success,
      });
      return response.data;
    },
    onSuccess: () => invalidateLimits(queryClient),
  });
}
