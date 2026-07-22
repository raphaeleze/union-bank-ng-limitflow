import axios, { AxiosError } from "axios";
import Constants from "expo-constants";
import * as SecureStore from "expo-secure-store";

import type { ApiErrorBody } from "./types";

export const AUTH_TOKEN_KEY = "limitflow_customer_token";
export const AUTH_USER_KEY = "limitflow_customer_user";

export class ApiError extends Error {
  status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.status = status;
  }
}

let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export const apiClient = axios.create({
  baseURL: (Constants.expoConfig?.extra?.apiBaseUrl as string) ?? "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorBody>) => {
    if (error.response?.status === 401) {
      await SecureStore.deleteItemAsync(AUTH_TOKEN_KEY);
      await SecureStore.deleteItemAsync(AUTH_USER_KEY);
      // Storage is cleared above, but the auth context's in-memory `user` state lives
      // outside this module. AuthProvider registers a callback here (see
      // setUnauthorizedHandler in auth.tsx) so it can reset that state and redirect —
      // there's no `window.location` equivalent to reach for directly from here.
      unauthorizedHandler?.();
    }

    const message =
      error.response?.data?.message ??
      (error.code === "ECONNABORTED" || (error.message ?? "").includes("Network")
        ? "We couldn't reach LimitFlow. Check your connection and try again."
        : "Something went wrong. Please try again.");

    return Promise.reject(new ApiError(message, error.response?.status));
  },
);
