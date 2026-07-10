"use client";

import { LogOut } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";

export function Topbar() {
  const { user, logout } = useAuth();

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
      <div>
        <p className="text-sm font-medium text-slate-900">{user?.firstName} {user?.lastName}</p>
        <p className="text-xs text-slate-500">
          {user?.role === "MANAGER" ? "Manager" : "Support agent"}
        </p>
      </div>
      <Button variant="ghost" size="sm" onClick={logout}>
        <LogOut className="h-4 w-4" />
        Log out
      </Button>
    </header>
  );
}
