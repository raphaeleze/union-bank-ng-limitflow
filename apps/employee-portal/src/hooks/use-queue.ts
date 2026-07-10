import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { SupportQueueItem } from "@/lib/types";

export function useQueueQuery() {
  return useQuery({
    queryKey: ["queue"],
    queryFn: async () => {
      const response = await apiClient.get<SupportQueueItem[]>("/support/requests");
      return response.data;
    },
    refetchInterval: 15_000,
  });
}
