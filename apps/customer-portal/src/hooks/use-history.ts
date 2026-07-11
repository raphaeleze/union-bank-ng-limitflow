import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { LimitRequest } from "@/lib/types";

export function useHistoryQuery() {
  return useQuery({
    queryKey: ["limits", "history"],
    queryFn: async () => {
      const response = await apiClient.get<LimitRequest[]>("/limits/history");
      return response.data;
    },
  });
}
