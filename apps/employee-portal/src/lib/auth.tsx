"use client";

import Cookies from "js-cookie";
import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { apiClient, AUTH_COOKIE, USER_COOKIE } from "./api-client";
import type { LoginResponse, UserSummary } from "./types";

interface AuthContextValue {
  user: UserSummary | null;
  isReady: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readCachedUser(): UserSummary | null {
  const raw = Cookies.get(USER_COOKIE);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSummary;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isReady, setIsReady] = useState(false);

  // Cookies only exist in the browser, so hydrate after mount rather than during
  // the server render — reading `document.cookie` during SSR would throw, and
  // computing it in the initial render would cause a hydration mismatch.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- one-time hydration from a browser-only API, not a derived-state effect
    setUser(readCachedUser());
    setIsReady(true);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<LoginResponse>("/auth/login", { email, password });
    const { token, user: loggedInUser } = response.data;

    if (loggedInUser.role !== "SUPPORT_AGENT" && loggedInUser.role !== "MANAGER") {
      throw new Error("This portal is for support and manager accounts only.");
    }

    Cookies.set(AUTH_COOKIE, token, { expires: 1, sameSite: "lax" });
    Cookies.set(USER_COOKIE, JSON.stringify(loggedInUser), { expires: 1, sameSite: "lax" });
    setUser(loggedInUser);
  }, []);

  const logout = useCallback(() => {
    Cookies.remove(AUTH_COOKIE);
    Cookies.remove(USER_COOKIE);
    setUser(null);
    router.push("/login");
  }, [router]);

  const value = useMemo(() => ({ user, isReady, login, logout }), [user, isReady, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
