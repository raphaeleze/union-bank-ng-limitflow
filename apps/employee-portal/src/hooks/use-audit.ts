import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { AuditLogEntry } from "@/lib/types";

export function useAuditQuery() {
  return useQuery({
    queryKey: ["audit"],
    queryFn: async () => {
      const response = await apiClient.get<AuditLogEntry[]>("/audit");
      return response.data;
    },
  });
}
