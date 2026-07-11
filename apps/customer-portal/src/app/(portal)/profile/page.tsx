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
      <h1 className="text-xl font-semibold text-slate-900">Profile</h1>

      <Card>
        <CardContent className="flex items-center gap-4 p-5">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-blue-600 text-lg font-semibold text-white">
            {user?.firstName?.[0]}
            {user?.lastName?.[0]}
          </div>
          <div>
            <p className="text-base font-medium text-slate-900">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="flex items-center gap-1 text-sm text-slate-500">
              <Mail className="h-3.5 w-3.5" />
              {user?.email}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="divide-y divide-slate-100 p-5">
          <Link href="/support" className="flex items-center gap-3 py-3 first:pt-0">
            <User className="h-4 w-4 text-slate-500" />
            <span className="text-sm font-medium text-slate-900">Contact support</span>
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
