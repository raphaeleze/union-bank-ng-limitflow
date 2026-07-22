import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest } from "@/lib/types";

export function useRequestDetailQuery(requestId: string) {
  return useQuery({
    queryKey: ["limits", requestId],
    queryFn: async () => {
      const response = await apiClient.get<LimitRequest>(`/limits/${requestId}`);
      return response.data;
    },
  });
}
