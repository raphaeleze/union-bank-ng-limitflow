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
      // Task 6's auth context listens for this via its own 401 handling on each query,
      // matching the web app's redirect-to-login-on-401 behavior without a global router
      // reference here (there isn't a `window.location` equivalent to reach for).
    }

    const message =
      error.response?.data?.message ??
      (error.code === "ECONNABORTED" || (error.message ?? "").includes("Network")
        ? "We couldn't reach LimitFlow. Check your connection and try again."
        : "Something went wrong. Please try again.");

    return Promise.reject(new ApiError(message, error.response?.status));
  },
);
