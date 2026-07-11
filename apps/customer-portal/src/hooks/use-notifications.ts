import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { NotificationItem } from "@/lib/types";

export function useNotificationsQuery() {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const response = await apiClient.get<NotificationItem[]>("/notifications");
      return response.data;
    },
    refetchInterval: 15_000,
  });
}
