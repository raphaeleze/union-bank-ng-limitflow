import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { CurrentLimitResponse } from "@/lib/types";

export function useCurrentLimitQuery() {
  return useQuery({
    queryKey: ["limits", "current"],
    queryFn: async () => {
      const response = await apiClient.get<CurrentLimitResponse>("/limits/current");
      return response.data;
    },
  });
}
