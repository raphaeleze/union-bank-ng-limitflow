"use client";

import { LogOut, Mail, User } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/lib/auth";

export default function ProfilePage() {
  const { user, logout } = useAuth();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Profile</h1>

      <Card>
        <CardContent className="flex items-center gap-4 p-5">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-accent text-lg font-semibold text-white">
            {user?.firstName?.[0]}
            {user?.lastName?.[0]}
          </div>
          <div>
            <p className="text-base font-medium text-ink">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="flex items-center gap-1 text-sm text-ink-muted">
              <Mail className="h-3.5 w-3.5" />
              {user?.email}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="divide-y divide-border p-5">
          <Link href="/support" className="flex items-center gap-3 py-3 first:pt-0">
            <User className="h-4 w-4 text-ink-muted" />
            <span className="text-sm font-medium text-ink">Contact support</span>
          </Link>
        </CardContent>
      </Card>

      <Button variant="outline" className="w-full" onClick={logout}>
        <LogOut className="h-4 w-4" />
        Log out
      </Button>
    </div>
  );
}
