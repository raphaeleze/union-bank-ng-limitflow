import type { ExpoConfig } from "expo/config";

const config: ExpoConfig = {
  name: "LimitFlow",
  slug: "limitflow-mobile",
  scheme: "limitflow",
  extra: {
    apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  },
};

export default config;
