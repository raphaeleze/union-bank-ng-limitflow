import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest, SupportNote } from "@/lib/types";

export function useRequestDetailQuery(requestId: string) {
  return useQuery({
    queryKey: ["request", requestId],
    queryFn: async () => {
      const response = await apiClient.get<LimitRequest>(`/support/requests/${requestId}`);
      return response.data;
    },
  });
}

export function useRequestNotesQuery(requestId: string) {
  return useQuery({
    queryKey: ["request", requestId, "notes"],
    queryFn: async () => {
      const response = await apiClient.get<SupportNote[]>(`/support/requests/${requestId}/notes`);
      return response.data;
    },
  });
}

function useReviewAction(action: "approve" | "reject" | "request-verification") {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ requestId, note }: { requestId: string; note?: string }) => {
      const response = await apiClient.post<LimitRequest>(`/support/requests/${requestId}/${action}`, {
        note,
      });
      return response.data;
    },
    onSuccess: (_, { requestId }) => {
      queryClient.invalidateQueries({ queryKey: ["queue"] });
      queryClient.invalidateQueries({ queryKey: ["request", requestId] });
      queryClient.invalidateQueries({ queryKey: ["request", requestId, "notes"] });
      queryClient.invalidateQueries({ queryKey: ["audit"] });
    },
  });
}

export function useApproveMutation() {
  return useReviewAction("approve");
}

export function useRejectMutation() {
  return useReviewAction("reject");
}

export function useRequestVerificationMutation() {
  return useReviewAction("request-verification");
}

export function useAddNoteMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ requestId, note }: { requestId: string; note: string }) => {
      const response = await apiClient.post<SupportNote>(`/support/requests/${requestId}/notes`, {
        note,
      });
      return response.data;
    },
    onSuccess: (_, { requestId }) => {
      queryClient.invalidateQueries({ queryKey: ["request", requestId, "notes"] });
    },
  });
}
