import axios, { AxiosError } from "axios";
import Cookies from "js-cookie";

import type { ApiErrorBody } from "./types";

export const AUTH_COOKIE = "limitflow_portal_token";
export const USER_COOKIE = "limitflow_portal_user";

export class ApiError extends Error {
  status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.status = status;
  }
}

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config) => {
  const token = Cookies.get(AUTH_COOKIE);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    if (error.response?.status === 401) {
      Cookies.remove(AUTH_COOKIE);
      Cookies.remove(USER_COOKIE);
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }

    const message =
      error.response?.data?.message ??
      (error.code === "ECONNABORTED" || error.message.includes("Network")
        ? "We couldn't reach LimitFlow. Check your connection and try again."
        : "Something went wrong. Please try again.");

    return Promise.reject(new ApiError(message, error.response?.status));
  },
);
