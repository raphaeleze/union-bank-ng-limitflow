"use client";

import { LogOut } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";

export function Topbar() {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4">
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-600 text-xs font-bold text-white">
          LF
        </div>
        <div>
          <p className="text-sm font-medium text-slate-900">
            Hi, {user?.firstName}
          </p>
        </div>
      </div>
      <Button variant="ghost" size="icon" aria-label="Log out" onClick={logout}>
        <LogOut className="h-4 w-4" />
      </Button>
    </header>
  );
}
