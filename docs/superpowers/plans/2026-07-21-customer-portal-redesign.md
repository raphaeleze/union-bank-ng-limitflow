# Customer Portal Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-skin `apps/customer-portal` from the unstyled Next.js starter defaults to an indigo/violet, tabular-mono-figures design system, modeled on Revolut-style fintech UI, with zero logic changes.

**Architecture:** A token system in `globals.css` (light + dark via `prefers-color-scheme`) cascades through the shared `components/ui/*` primitives, which every page already consumes — so most of the 25 source files need only their hardcoded `slate`/`blue`/`emerald`/`amber`/`red` Tailwind classes swapped for the new token-backed utility classes, not structural changes. Three surfaces get bespoke treatment beyond the token swap: the login page (brand mark), the dashboard's limit summary card (the one signature gradient "hero" element), and the increase-limit wizard's OTP/biometric steps.

**Tech Stack:** Next.js 16 (App Router), Tailwind CSS v4 (`@theme inline`), existing dependencies only — no new packages.

## Global Constraints

- No new npm dependencies. No new fonts — Geist Sans/Mono are already loaded via `next/font/google` in `src/app/layout.tsx` and stay exactly as configured there.
- Every currency figure, account/OTP digit uses the `font-tabular` utility class (defined in this plan's Task 1) — Geist Mono + `font-variant-numeric: tabular-nums`. This is the one deliberate typographic signature, not decoration for its own sake.
- Only the dashboard's limit summary card gets the gradient "hero" treatment — every other surface stays flat (`rounded-2xl border border-border bg-card`, no shadow) per the design spec's "spend boldness in one place" principle.
- Dark mode via `@media (prefers-color-scheme: dark)` only — no theme toggle, no `next-themes` or similar dependency; none was requested.
- No change to any component's props, exported function signatures, hooks, or data-fetching logic in this plan — every task is a visual re-skin. If a step's diff touches anything beyond `className` strings and the two bespoke files named above, that's a signal the task has drifted out of scope.
- Scope is `apps/customer-portal` only. `apps/employee-portal` is untouched.

## Design Tokens (defined in Task 1, referenced by every later task)

| Token | Light | Dark | Use |
|---|---|---|---|
| `ink` | `#151222` | `#F2F0FA` | primary text |
| `ink-muted` | `#6B6580` | `#A199C2` | secondary text |
| `surface` | `#FAF9FE` | `#0F0B22` | page background |
| `card` | `#FFFFFF` | `#1B1533` | card surface |
| `border` | `#ECE8F6` | `#2A2350` | borders, dividers, neutral fills |
| `accent` | `#5B3DF5` | `#8B7CFF` | primary actions, brand, links |
| `accent-deep` | `#241653` | `#150C33` | hero gradient dark end |
| `accent-soft` | `#EFEBFF` | `#241C4A` | tinted accent backgrounds (badges, active-request banner) |
| `success` / `success-soft` | `#12B76A` / `#E7F9F0` | `#3DD68C` / `#123326` | approved |
| `danger` / `danger-soft` | `#E5484D` / `#FDEDEE` | `#FF6369` / `#3A1518` | rejected/errors |
| `warning` / `warning-soft` | `#F5A524` / `#FEF3E2` | `#FFC24D` / `#3A2A10` | pending/under review |

Every `--color-X` key defined in Tailwind v4's `@theme inline` automatically generates the full utility family for that name (`bg-X`, `text-X`, `border-X`, `ring-X`, `divide-X`, `accent-X`, `from-X`/`via-X`/`to-X`, `shadow-X`, all with opacity modifiers like `bg-accent/20`) — no per-utility configuration needed beyond the token definition itself.

---

### Task 1: Design tokens and base UI components

**Files:**
- Modify: `apps/customer-portal/src/app/globals.css`
- Modify: `apps/customer-portal/src/app/layout.tsx`
- Modify: `apps/customer-portal/src/components/ui/button.tsx`
- Modify: `apps/customer-portal/src/components/ui/card.tsx`
- Modify: `apps/customer-portal/src/components/ui/badge.tsx`
- Modify: `apps/customer-portal/src/components/ui/input.tsx`
- Modify: `apps/customer-portal/src/components/ui/textarea.tsx`
- Modify: `apps/customer-portal/src/components/ui/label.tsx`
- Modify: `apps/customer-portal/src/components/ui/dialog.tsx`
- Modify: `apps/customer-portal/src/components/ui/skeleton.tsx`
- Modify: `apps/customer-portal/src/components/ui/toast.tsx`

**Interfaces:**
- Produces: the token table above, available as Tailwind utilities (`bg-accent`, `text-ink`, etc.) and the `font-tabular` CSS class — consumed by every subsequent task. No component prop/export changes — every file in this task keeps its existing function signatures and exports, only `className` strings change.

- [ ] **Step 1: Rewrite `globals.css`**

Replace the full file:
```css
@import "tailwindcss";

:root {
  --ink: #151222;
  --ink-muted: #6b6580;
  --surface: #faf9fe;
  --card: #ffffff;
  --border: #ece8f6;
  --accent: #5b3df5;
  --accent-deep: #241653;
  --accent-soft: #efebff;
  --success: #12b76a;
  --success-soft: #e7f9f0;
  --danger: #e5484d;
  --danger-soft: #fdedee;
  --warning: #f5a524;
  --warning-soft: #fef3e2;
}

@theme inline {
  --color-ink: var(--ink);
  --color-ink-muted: var(--ink-muted);
  --color-surface: var(--surface);
  --color-card: var(--card);
  --color-border: var(--border);
  --color-accent: var(--accent);
  --color-accent-deep: var(--accent-deep);
  --color-accent-soft: var(--accent-soft);
  --color-success: var(--success);
  --color-success-soft: var(--success-soft);
  --color-danger: var(--danger);
  --color-danger-soft: var(--danger-soft);
  --color-warning: var(--warning);
  --color-warning-soft: var(--warning-soft);
  --font-sans: var(--font-geist-sans);
  --font-mono: var(--font-geist-mono);
}

@media (prefers-color-scheme: dark) {
  :root {
    --ink: #f2f0fa;
    --ink-muted: #a199c2;
    --surface: #0f0b22;
    --card: #1b1533;
    --border: #2a2350;
    --accent: #8b7cff;
    --accent-deep: #150c33;
    --accent-soft: #241c4a;
    --success: #3dd68c;
    --success-soft: #123326;
    --danger: #ff6369;
    --danger-soft: #3a1518;
    --warning: #ffc24d;
    --warning-soft: #3a2a10;
  }
}

body {
  background: var(--surface);
  color: var(--ink);
  font-family: var(--font-sans), system-ui, sans-serif;
}

.font-tabular {
  font-family: var(--font-mono), ui-monospace, monospace;
  font-variant-numeric: tabular-nums;
}
```

- [ ] **Step 2: Fix `layout.tsx`'s body classes**

In `apps/customer-portal/src/app/layout.tsx`, the `<body>` element currently hardcodes `bg-slate-50 text-slate-900`, which now conflicts with `globals.css`'s `body { background: var(--surface); color: var(--ink); }`. Replace:

```tsx
      <body className="min-h-full flex flex-col bg-slate-50 text-slate-900">
```
with:
```tsx
      <body className="min-h-full flex flex-col antialiased">
```

(The rest of the file — the `Geist`/`Geist_Mono` font loading, the `html` element's `className`, `metadata` — is untouched.)

- [ ] **Step 3: Re-theme `button.tsx`**

Replace the `buttonVariants` call's first two arguments (the base class string and the `variant` object) — everything else in the file (`ButtonProps`, the `forwardRef` implementation, `size` variants) is unchanged:

```tsx
const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium transition-colors active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-accent disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-accent text-white hover:bg-accent-deep",
        outline:
          "border border-border bg-card text-ink hover:bg-accent-soft",
        ghost: "text-ink-muted hover:bg-accent-soft hover:text-ink",
        destructive: "bg-danger text-white hover:opacity-90",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-8 px-3 text-xs",
        lg: "h-11 px-6",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);
```

(`active:scale-[0.98]` is the one deliberate micro-interaction the design spec calls for — button press feedback — added here since every button in the app goes through this one component.)

- [ ] **Step 4: Re-theme `card.tsx`**

Replace the full file:
```tsx
import * as React from "react";

import { cn } from "@/lib/utils";

export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-border bg-card",
        className,
      )}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("flex flex-col gap-1 p-6 pb-0", className)} {...props} />;
}

export function CardTitle({ className, ...props }: React.HTMLAttributes<HTMLHeadingElement>) {
  return <h3 className={cn("text-lg font-semibold text-ink", className)} {...props} />;
}

export function CardDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <p className={cn("text-sm text-ink-muted", className)} {...props} />;
}

export function CardContent({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("p-6", className)} {...props} />;
}
```

(Dropped `shadow-sm` — per the design spec, only the dashboard's hero card gets a shadow; every other card stays flat.)

- [ ] **Step 5: Re-theme `badge.tsx`**

Replace the `badgeVariants` variants object:
```tsx
const badgeVariants = cva(
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold",
  {
    variants: {
      variant: {
        neutral: "bg-border text-ink-muted",
        blue: "bg-accent-soft text-accent",
        green: "bg-success-soft text-success",
        orange: "bg-warning-soft text-warning",
        red: "bg-danger-soft text-danger",
      },
    },
    defaultVariants: { variant: "neutral" },
  },
);
```

- [ ] **Step 6: Re-theme `input.tsx`**

```tsx
import * as React from "react";

import { cn } from "@/lib/utils";

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => {
    return (
      <input
        ref={ref}
        className={cn(
          "flex h-10 w-full rounded-lg border border-border bg-card px-3 py-2 text-sm text-ink placeholder:text-ink-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:cursor-not-allowed disabled:opacity-50",
          className,
        )}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";
```

- [ ] **Step 7: Re-theme `textarea.tsx`**

```tsx
import * as React from "react";

import { cn } from "@/lib/utils";

export const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className, ...props }, ref) => {
  return (
    <textarea
      ref={ref}
      className={cn(
        "flex min-h-[90px] w-full rounded-lg border border-border bg-card px-3 py-2 text-sm text-ink placeholder:text-ink-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
});
Textarea.displayName = "Textarea";
```

- [ ] **Step 8: Re-theme `label.tsx`**

Change the one class string:
```tsx
    className={cn("text-sm font-medium text-ink", className)}
```

- [ ] **Step 9: Re-theme `dialog.tsx`**

Replace these class strings (everything else — the Radix wiring, exports, `displayName` assignments — is unchanged):
- `DialogPrimitive.Overlay`: `"fixed inset-0 z-50 bg-slate-950/40"` → `"fixed inset-0 z-50 bg-ink/40"`
- `DialogPrimitive.Content`: `"fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl bg-white p-6 shadow-xl"` → `"fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl bg-card p-6 shadow-xl"`
- `DialogPrimitive.Close`: `"absolute right-4 top-4 text-slate-500 hover:text-slate-600"` → `"absolute right-4 top-4 text-ink-muted hover:text-ink"`
- `DialogTitle`: `"text-lg font-semibold text-slate-900"` → `"text-lg font-semibold text-ink"`
- `DialogDescription`: `"text-sm text-slate-500"` → `"text-sm text-ink-muted"`

- [ ] **Step 10: Re-theme `skeleton.tsx`**

```tsx
import type { HTMLAttributes } from "react";

import { cn } from "@/lib/utils";

export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("animate-pulse rounded-md bg-border", className)} {...props} />;
}
```

- [ ] **Step 11: Re-theme `toast.tsx`**

Replace the toast variant class string:
```tsx
              t.variant === "success"
                ? "border-success/30 bg-success-soft text-success"
                : "border-danger/30 bg-danger-soft text-danger",
```

- [ ] **Step 12: Build and lint check**

Run: `npm run build` (from `apps/customer-portal`)
Expected: build succeeds with no TypeScript errors. This won't catch a wrong color value, but it does catch a broken import, a typo'd prop, or invalid JSX — the things this refactor could plausibly break.

Run: `grep -rn "slate-\|blue-600\|blue-100\|blue-50\|emerald-\|amber-\|red-600\|red-200\|shadow-sm" src/components/ui` (from `apps/customer-portal`)
Expected: no matches — confirms every base component's old palette classes are gone.

- [ ] **Step 13: Commit**

```bash
git add apps/customer-portal/src/app/globals.css apps/customer-portal/src/app/layout.tsx apps/customer-portal/src/components/ui
git commit -m "Add indigo/violet design tokens and re-theme base UI components"
```

---

### Task 2: Login page

**Files:**
- Modify: `apps/customer-portal/src/app/login/page.tsx`

**Interfaces:** None — same `LoginPage` component, same form logic (`react-hook-form`, `zod`, `useAuth`), only JSX/`className` changes.

- [ ] **Step 1: Replace the full file**

```tsx
"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Enter your password"),
});

type LoginValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "customer@limitflow.demo", password: "" },
  });

  const onSubmit = async (values: LoginValues) => {
    setServerError(null);
    try {
      await login(values.email, values.password);
      router.replace("/dashboard");
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : (error as Error).message);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-accent to-accent-deep text-xl font-bold text-white">
            LF
          </div>
          <h1 className="text-2xl font-semibold text-ink">LimitFlow</h1>
          <p className="text-sm text-ink-muted">Sign in to manage your transfer limit.</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" autoComplete="email" {...register("email")} />
            {errors.email && <p className="text-xs text-danger">{errors.email.message}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register("password")}
            />
            {errors.password && <p className="text-xs text-danger">{errors.password.message}</p>}
          </div>

          {serverError && <p className="text-sm text-danger">{serverError}</p>}

          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? "Signing in…" : "Sign in"}
          </Button>
        </form>

        <p className="mt-6 text-center text-xs text-ink-muted">
          Demo account: customer@limitflow.demo — password{" "}
          <span className="font-tabular">Password123!</span>
        </p>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Build check**

Run: `npm run build` (from `apps/customer-portal`)
Expected: succeeds.

- [ ] **Step 3: Commit**

```bash
git add apps/customer-portal/src/app/login/page.tsx
git commit -m "Redesign login page: gradient brand mark and new tokens"
```

---

### Task 3: Dashboard — the signature gradient hero card

**Files:**
- Modify: `apps/customer-portal/src/components/dashboard/limit-summary-card.tsx`
- Modify: `apps/customer-portal/src/components/dashboard/active-request-banner.tsx`
- Modify: `apps/customer-portal/src/app/(portal)/dashboard/page.tsx`

**Interfaces:** None — `LimitSummaryCard({ dailyLimit, usedToday, remaining })` and `ActiveRequestBanner({ request })` keep their exact prop signatures; only their internal JSX/markup changes.

This is the one place in the whole redesign spending real visual boldness — everywhere else in the app stays flat per Task 1's `Card` component.

- [ ] **Step 1: Replace `limit-summary-card.tsx`**

```tsx
import { formatCurrency } from "@/lib/currency";

export function LimitSummaryCard({
  dailyLimit,
  usedToday,
  remaining,
}: {
  dailyLimit: number;
  usedToday: number;
  remaining: number;
}) {
  const usedPct = dailyLimit > 0 ? Math.min(100, Math.round((usedToday / dailyLimit) * 100)) : 0;

  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-accent to-accent-deep p-5 text-white shadow-lg shadow-accent/20">
      <div
        aria-hidden
        className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10 blur-2xl"
      />
      <p className="text-sm text-white/70">Daily transfer limit</p>
      <p className="font-tabular mt-1 text-3xl font-semibold">{formatCurrency(dailyLimit)}</p>

      <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-white/20">
        <div className="h-full rounded-full bg-white transition-[width]" style={{ width: `${usedPct}%` }} />
      </div>
      <div className="font-tabular mt-2 flex justify-between text-xs text-white/70">
        <span>{formatCurrency(usedToday)} used today</span>
        <span>{formatCurrency(remaining)} remaining</span>
      </div>
    </div>
  );
}
```

(This drops the shared `Card`/`CardContent` wrapper entirely — it's a bespoke standalone treatment, per the design spec, not the generic flat card every other surface uses.)

- [ ] **Step 2: Replace `active-request-banner.tsx`**

A smaller echo of the same color language (tinted accent background, tabular-mono figure), not a repeat of the full gradient — the gradient stays unique to the dashboard hero:

```tsx
import Link from "next/link";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function ActiveRequestBanner({ request }: { request: LimitRequest }) {
  return (
    <Link href={`/requests/${request.id}`}>
      <div className="flex items-center justify-between gap-3 rounded-2xl border border-accent/20 bg-accent-soft p-4 transition-colors hover:bg-accent-soft/70">
        <div>
          <p className="font-tabular text-sm font-medium text-ink">
            Request for {formatCurrency(request.requestedLimit)}
          </p>
          <p className="text-xs text-ink-muted">Tap to see progress</p>
        </div>
        <StatusBadge status={request.status} />
      </div>
    </Link>
  );
}
```

- [ ] **Step 3: Replace `dashboard/page.tsx`**

```tsx
"use client";

import { ArrowUpCircle } from "lucide-react";
import Link from "next/link";

import { ActiveRequestBanner } from "@/components/dashboard/active-request-banner";
import { LimitSummaryCard } from "@/components/dashboard/limit-summary-card";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import { useAuth } from "@/lib/auth";

export default function DashboardPage() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useCurrentLimitQuery();

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-ink">Hi, {user?.firstName}</h1>
        <p className="text-sm text-ink-muted">Here&apos;s your account at a glance.</p>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-32" />
          <Skeleton className="h-16" />
        </div>
      ) : isError || !data ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load your account.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : (
        <>
          <LimitSummaryCard dailyLimit={data.dailyLimit} usedToday={data.usedToday} remaining={data.remaining} />

          {data.activeRequest ? (
            <ActiveRequestBanner request={data.activeRequest} />
          ) : (
            <Button asChild size="lg" className="w-full">
              <Link href="/increase-limit">
                <ArrowUpCircle className="h-4 w-4" />
                Increase my limit
              </Link>
            </Button>
          )}
        </>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Build check**

Run: `npm run build` (from `apps/customer-portal`)
Expected: succeeds.

- [ ] **Step 5: Commit**

```bash
git add apps/customer-portal/src/components/dashboard apps/customer-portal/src/app/\(portal\)/dashboard/page.tsx
git commit -m "Redesign dashboard: gradient hero card, accent-tinted active request banner"
```

---

### Task 4: Increase-limit wizard

**Files:**
- Modify: `apps/customer-portal/src/app/(portal)/increase-limit/page.tsx`

**Interfaces:** None — same `IncreaseLimitPage` component, same step state machine and mutation hooks, only JSX/`className` changes. Currency and OTP figures gain `font-tabular`, per Global Constraints.

- [ ] **Step 1: Replace the full file**

```tsx
"use client";

import { CheckCircle2, Fingerprint, ShieldCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { useCurrentLimitQuery } from "@/hooks/use-dashboard";
import {
  useSubmitLimitRequestMutation,
  useVerifyBiometricMutation,
  useVerifyOtpMutation,
} from "@/hooks/use-limit-request";
import { ApiError } from "@/lib/api-client";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

type Step = "amount" | "review" | "otp" | "biometric" | "done";

export default function IncreaseLimitPage() {
  const router = useRouter();
  const { toast } = useToast();
  const { data: current, isLoading } = useCurrentLimitQuery();

  const [step, setStep] = useState<Step>("amount");
  const [requestedLimit, setRequestedLimit] = useState("");
  const [reason, setReason] = useState("");
  const [newDevice, setNewDevice] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [request, setRequest] = useState<LimitRequest | null>(null);

  const submitMutation = useSubmitLimitRequestMutation();
  const otpMutation = useVerifyOtpMutation();
  const biometricMutation = useVerifyBiometricMutation();

  // Reachable directly via the bottom nav even with a request already in flight —
  // redirect to it instead of letting the customer fill out the whole form only to
  // have the backend reject the submission (an account can only have one active
  // request at a time).
  const hasActiveRequest = Boolean(current?.activeRequest) && !request;
  useEffect(() => {
    if (hasActiveRequest) {
      router.replace(`/requests/${current!.activeRequest!.id}`);
    }
  }, [hasActiveRequest, current, router]);

  if (isLoading || !current || hasActiveRequest) {
    return <p className="text-sm text-ink-muted">Loading…</p>;
  }

  const amount = Number(requestedLimit);
  const amountValid = amount > current.dailyLimit;

  async function handleSubmit() {
    try {
      const result = await submitMutation.mutateAsync({
        accountId: current!.accountId,
        requestedLimit: amount,
        reason,
        knownDevice: !newDevice,
      });
      setRequest(result);
      setStep("otp");
      toast("We sent a verification code. Check Notifications for the demo code.");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Couldn't submit your request.", "error");
    }
  }

  async function handleOtpVerify() {
    if (!request) return;
    try {
      const result = await otpMutation.mutateAsync({ requestId: request.id, code: otpCode });
      setRequest(result);
      setStep("biometric");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "That code didn't work.", "error");
    }
  }

  async function handleBiometricConfirm() {
    if (!request) return;
    try {
      const result = await biometricMutation.mutateAsync({ requestId: request.id, success: true });
      setRequest(result);
      setStep("done");
    } catch (error) {
      toast(error instanceof ApiError ? error.message : "Biometric verification failed.", "error");
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Increase your limit</h1>

      {step === "amount" && (
        <Card>
          <CardContent className="space-y-4 p-5">
            <p className="text-sm text-ink-muted">
              Current daily limit:{" "}
              <span className="font-tabular font-medium text-ink">{formatCurrency(current.dailyLimit)}</span>
            </p>

            <div className="space-y-1.5">
              <Label htmlFor="amount">New daily limit (₦)</Label>
              <Input
                id="amount"
                type="number"
                inputMode="numeric"
                min={current.dailyLimit + 1}
                value={requestedLimit}
                onChange={(e) => setRequestedLimit(e.target.value)}
                placeholder={`More than ${current.dailyLimit}`}
                className="font-tabular"
              />
              {requestedLimit && !amountValid && (
                <p className="text-xs text-danger">Must be more than your current limit.</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="reason">Reason for increase</Label>
              <Textarea
                id="reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="e.g. Paying a supplier for a large order"
                rows={3}
              />
            </div>

            <label className="flex items-center gap-2 text-sm text-ink-muted">
              <input
                type="checkbox"
                checked={newDevice}
                onChange={(e) => setNewDevice(e.target.checked)}
                className="h-4 w-4 rounded border-border accent-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
              />
              I&apos;m on a new or unrecognized device
            </label>

            <Button
              className="w-full"
              disabled={!amountValid || reason.trim().length === 0}
              onClick={() => setStep("review")}
            >
              Continue
            </Button>
          </CardContent>
        </Card>
      )}

      {step === "review" && (
        <Card>
          <CardContent className="space-y-4 p-5">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-medium text-ink">Review your request</h2>
              <button
                type="button"
                onClick={() => setStep("amount")}
                className="text-sm font-medium text-accent"
              >
                Edit
              </button>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-ink-muted">New limit</span>
                <span className="font-tabular font-medium text-ink">{formatCurrency(amount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink-muted">Reason</span>
                <span className="max-w-[60%] text-right font-medium text-ink">{reason}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink-muted">Device</span>
                <span className="font-medium text-ink">{newDevice ? "New device" : "Trusted device"}</span>
              </div>
            </div>
            <Button className="w-full" disabled={submitMutation.isPending} onClick={handleSubmit}>
              {submitMutation.isPending ? "Submitting…" : "Confirm and submit"}
            </Button>
          </CardContent>
        </Card>
      )}

      {step === "otp" && (
        <Card>
          <CardContent className="space-y-4 p-5 text-center">
            <ShieldCheck className="mx-auto h-10 w-10 text-accent" />
            <div>
              <p className="font-medium text-ink">Enter the verification code</p>
              <p className="text-sm text-ink-muted">Sent to your registered number. Check Notifications for the demo code.</p>
            </div>
            <Input
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              placeholder="6-digit code"
              inputMode="numeric"
              className="font-tabular text-center text-lg tracking-widest"
            />
            <Button className="w-full" disabled={otpCode.length === 0 || otpMutation.isPending} onClick={handleOtpVerify}>
              {otpMutation.isPending ? "Verifying…" : "Verify code"}
            </Button>
          </CardContent>
        </Card>
      )}

      {step === "biometric" && (
        <Card>
          <CardContent className="space-y-4 p-5 text-center">
            <Fingerprint className="mx-auto h-10 w-10 text-accent" />
            <div>
              <p className="font-medium text-ink">Confirm it&apos;s you</p>
              <p className="text-sm text-ink-muted">Use your fingerprint or face to finish verifying this request.</p>
            </div>
            <Button className="w-full" disabled={biometricMutation.isPending} onClick={handleBiometricConfirm}>
              {biometricMutation.isPending ? "Confirming…" : "Confirm biometric"}
            </Button>
          </CardContent>
        </Card>
      )}

      {step === "done" && request && (
        <Card>
          <CardContent className="space-y-4 p-5 text-center">
            <CheckCircle2 className="mx-auto h-10 w-10 text-success" />
            <div>
              <p className="font-medium text-ink">
                {request.status === "APPROVED" ? "Limit increased" : "Request submitted for review"}
              </p>
              <p className="text-sm text-ink-muted">
                {request.status === "APPROVED"
                  ? `Your new daily limit is ${formatCurrency(request.requestedLimit)}.`
                  : "We'll notify you once a review is complete."}
              </p>
            </div>
            <Button className="w-full" onClick={() => router.replace(`/requests/${request.id}`)}>
              View request status
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Build check**

Run: `npm run build` (from `apps/customer-portal`)
Expected: succeeds.

- [ ] **Step 3: Commit**

```bash
git add "apps/customer-portal/src/app/(portal)/increase-limit/page.tsx"
git commit -m "Redesign increase-limit wizard: new tokens, tabular-mono OTP and currency figures"
```

---

### Task 5: Requests list, detail, and timeline

**Files:**
- Modify: `apps/customer-portal/src/components/requests/timeline.tsx`
- Modify: `apps/customer-portal/src/components/requests/request-list-item.tsx`
- Modify: `apps/customer-portal/src/app/(portal)/requests/[id]/request-detail-client.tsx`
- Modify: `apps/customer-portal/src/app/(portal)/requests/page.tsx`

**Note:** `apps/customer-portal/src/components/requests/status-badge.tsx` needs **no changes** — it only references `Badge`'s own variant names (`"blue" | "green" | "orange" | "red" | "neutral"`), which Task 1 already re-themed. Don't touch it.

**Interfaces:** None — `Timeline({ steps })`, `RequestListItem({ request })`, `RequestDetailClient({ requestId })`, `RequestsPage` all keep their exact signatures.

- [ ] **Step 1: Replace `timeline.tsx`**

```tsx
import { Check } from "lucide-react";

import { cn } from "@/lib/utils";
import type { TimelineStepStatus } from "@/lib/types";

export function Timeline({ steps }: { steps: { label: string; status: TimelineStepStatus }[] }) {
  return (
    <ol className="space-y-0">
      {steps.map((step, index) => {
        const isComplete = step.status === "COMPLETE";
        const isCurrent = step.status === "CURRENT";
        const isLast = index === steps.length - 1;

        return (
          <li key={step.label} className="flex gap-3">
            <div className="flex flex-col items-center">
              <div
                className={cn(
                  "flex h-6 w-6 items-center justify-center rounded-full border-2",
                  isComplete && "border-success bg-success text-white",
                  isCurrent && "border-accent bg-accent text-white",
                  !isComplete && !isCurrent && "border-border bg-card",
                )}
              >
                {isComplete && <Check className="h-3.5 w-3.5" />}
              </div>
              {!isLast && (
                <div
                  className={cn(
                    "w-0.5 flex-1",
                    isComplete ? "bg-success" : "bg-border",
                  )}
                />
              )}
            </div>
            <div className={cn("pb-6 text-sm", isCurrent ? "font-semibold text-ink" : "text-ink-muted")}>
              {step.label}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
```

- [ ] **Step 2: Replace `request-list-item.tsx`**

```tsx
import { formatDistanceToNow } from "date-fns";
import Link from "next/link";

import { StatusBadge } from "@/components/requests/status-badge";
import { formatCurrency } from "@/lib/currency";
import type { LimitRequest } from "@/lib/types";

export function RequestListItem({ request }: { request: LimitRequest }) {
  return (
    <Link
      href={`/requests/${request.id}`}
      className="flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
    >
      <div>
        <p className="font-tabular text-sm font-medium text-ink">{formatCurrency(request.requestedLimit)}</p>
        <p className="text-xs text-ink-muted">
          {formatDistanceToNow(new Date(request.createdAt), { addSuffix: true })}
        </p>
      </div>
      <StatusBadge status={request.status} />
    </Link>
  );
}
```

- [ ] **Step 3: Replace `request-detail-client.tsx`**

```tsx
"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";

import { Timeline } from "@/components/requests/timeline";
import { StatusBadge } from "@/components/requests/status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useRequestDetailQuery } from "@/hooks/use-request-detail";
import { formatCurrency } from "@/lib/currency";

export function RequestDetailClient({ requestId }: { requestId: string }) {
  const { data: request, isLoading, isError, refetch } = useRequestDetailQuery(requestId);

  return (
    <div className="space-y-4">
      <Link href="/requests" className="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <ArrowLeft className="h-4 w-4" />
        Back to requests
      </Link>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-48" />
        </div>
      ) : isError || !request ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load this request.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent className="p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm text-ink-muted">Requested limit</p>
                  <p className="font-tabular text-2xl font-semibold text-ink">{formatCurrency(request.requestedLimit)}</p>
                  <p className="font-tabular mt-1 text-sm text-ink-muted">Current limit: {formatCurrency(request.currentLimit)}</p>
                </div>
                <StatusBadge status={request.status} />
              </div>
              <div className="mt-4 border-t border-border pt-4">
                <p className="text-sm text-ink-muted">Reason given</p>
                <p className="mt-1 text-sm text-ink">{request.reason}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Progress</CardTitle>
            </CardHeader>
            <CardContent>
              <Timeline steps={request.timeline} />
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Replace `requests/page.tsx`**

```tsx
"use client";

import { Card, CardContent } from "@/components/ui/card";
import { RequestListItem } from "@/components/requests/request-list-item";
import { Skeleton } from "@/components/ui/skeleton";
import { useHistoryQuery } from "@/hooks/use-history";

export default function RequestsPage() {
  const { data, isLoading, isError, refetch } = useHistoryQuery();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Your requests</h1>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </div>
      ) : isError ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load your requests.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="p-6 text-center text-sm text-ink-muted">
            You haven&apos;t requested a limit increase yet.
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="divide-y divide-border p-5">
            {data.map((request) => (
              <RequestListItem key={request.id} request={request} />
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
```

- [ ] **Step 5: Build check**

Run: `npm run build` (from `apps/customer-portal`)
Expected: succeeds.

- [ ] **Step 6: Commit**

```bash
git add apps/customer-portal/src/components/requests "apps/customer-portal/src/app/(portal)/requests"
git commit -m "Redesign requests list, detail, and timeline with new tokens"
```

---

### Task 6: Notifications, profile, support pages, and shared chrome

**Files:**
- Modify: `apps/customer-portal/src/app/(portal)/notifications/page.tsx`
- Modify: `apps/customer-portal/src/app/(portal)/profile/page.tsx`
- Modify: `apps/customer-portal/src/app/(portal)/support/page.tsx`
- Modify: `apps/customer-portal/src/components/layout/topbar.tsx`
- Modify: `apps/customer-portal/src/components/layout/bottom-nav.tsx`

**Interfaces:** None — every component keeps its exact signature; only `className` strings change. Purely mechanical token substitution, no bespoke treatment needed for any of these five files.

- [ ] **Step 1: Replace `notifications/page.tsx`**

```tsx
"use client";

import { formatDistanceToNow } from "date-fns";
import { Bell, CheckCircle2, MessageCircle, ShieldCheck, XCircle } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useNotificationsQuery } from "@/hooks/use-notifications";
import { cn } from "@/lib/utils";

const ICONS: Record<string, LucideIcon> = {
  OTP_SENT: ShieldCheck,
  VERIFICATION_COMPLETED: ShieldCheck,
  VERIFICATION_REQUESTED: ShieldCheck,
  LIMIT_APPROVED: CheckCircle2,
  LIMIT_REJECTED: XCircle,
  SUPPORT_COMMENT: MessageCircle,
};

export default function NotificationsPage() {
  const { data, isLoading, isError, refetch } = useNotificationsQuery();

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-ink">Notifications</h1>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-14" />
          ))}
        </div>
      ) : isError ? (
        <Card>
          <CardContent className="flex items-center justify-between p-6">
            <p className="text-sm text-ink-muted">We couldn&apos;t load your notifications.</p>
            <button onClick={() => refetch()} className="text-sm font-medium text-accent">
              Try again
            </button>
          </CardContent>
        </Card>
      ) : !data || data.length === 0 ? (
        <Card>
          <CardContent className="p-6 text-center text-sm text-ink-muted">
            <Bell className="mx-auto mb-2 h-8 w-8 text-border" />
            Nothing here yet.
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="divide-y divide-border p-5">
            {data.map((item) => {
              const Icon = ICONS[item.type] ?? Bell;
              return (
                <div key={item.id} className="flex gap-3 py-3 first:pt-0 last:pb-0">
                  <div
                    className={cn(
                      "flex h-9 w-9 shrink-0 items-center justify-center rounded-full",
                      item.read ? "bg-border text-ink-muted" : "bg-accent-soft text-accent",
                    )}
                  >
                    <Icon className="h-4 w-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink">{item.title}</p>
                    <p className="text-sm text-ink-muted">{item.message}</p>
                    <p className="mt-1 text-xs text-ink-muted">
                      {formatDistanceToNow(new Date(item.createdAt), { addSuffix: true })}
                    </p>
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Replace `profile/page.tsx`**

```tsx
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
```

- [ ] **Step 3: Replace `support/page.tsx`**

```tsx
import { Mail, MessageCircle, Phone } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";

const CHANNELS = [
  { icon: Phone, label: "Call us", value: "0700-LIMITFLOW", href: "tel:0700546483569" },
  { icon: Mail, label: "Email us", value: "support@limitflow.demo", href: "mailto:support@limitflow.demo" },
];

export default function SupportPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-ink">Support</h1>
        <p className="text-sm text-ink-muted">Need help with a request? Reach out any time.</p>
      </div>

      <Card>
        <CardContent className="divide-y divide-border p-5">
          {CHANNELS.map((channel) => (
            <a
              key={channel.label}
              href={channel.href}
              className="flex items-center gap-3 py-3 first:pt-0 last:pb-0"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent-soft text-accent">
                <channel.icon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-medium text-ink">{channel.label}</p>
                <p className="text-sm text-ink-muted">{channel.value}</p>
              </div>
            </a>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex items-start gap-3 p-5">
          <MessageCircle className="mt-0.5 h-5 w-5 shrink-0 text-ink-muted" />
          <p className="text-sm text-ink-muted">
            If a request lands in review, our team follows up using the details on your profile —
            no need to call in just to check on it. Track progress any time from the Requests tab.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 4: Replace `topbar.tsx`**

```tsx
"use client";

import { LogOut } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";

export function Topbar() {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-border bg-card px-4">
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-accent text-xs font-bold text-white">
          LF
        </div>
        <div>
          <p className="text-sm font-medium text-ink">
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
```

- [ ] **Step 5: Replace `bottom-nav.tsx`**

```tsx
"use client";

import { ArrowUpCircle, Bell, ClipboardList, LayoutDashboard, User } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { href: "/dashboard", label: "Home", icon: LayoutDashboard },
  { href: "/increase-limit", label: "Increase", icon: ArrowUpCircle },
  { href: "/requests", label: "Requests", icon: ClipboardList },
  { href: "/notifications", label: "Alerts", icon: Bell },
  { href: "/profile", label: "Profile", icon: User },
];

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="fixed inset-x-0 bottom-0 z-10 border-t border-border bg-card">
      <div className="mx-auto flex max-w-md items-stretch justify-between px-2">
        {NAV_ITEMS.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "flex flex-1 flex-col items-center gap-1 py-2.5 text-xs font-medium transition-colors",
                isActive ? "text-accent" : "text-ink-muted hover:text-ink",
              )}
            >
              <Icon className="h-5 w-5" />
              {item.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
```

- [ ] **Step 6: Build and final token-leak check**

Run: `npm run build` (from `apps/customer-portal`)
Expected: succeeds.

Run: `grep -rln "slate-\|blue-600\|blue-100\|blue-50\|blue-700\|emerald-\|amber-\|red-600\|red-200" src` (from `apps/customer-portal`)
Expected: no matches anywhere in `src` — confirms every file in the redesign's scope (Tasks 1–6) is fully migrated to the new token classes.

- [ ] **Step 7: Commit**

```bash
git add "apps/customer-portal/src/app/(portal)/notifications/page.tsx" "apps/customer-portal/src/app/(portal)/profile/page.tsx" "apps/customer-portal/src/app/(portal)/support/page.tsx" apps/customer-portal/src/components/layout
git commit -m "Redesign notifications, profile, support pages and shared chrome with new tokens"
```

---

### Task 7: Final visual verification

**Files:** None modified — this task only verifies, in a real browser, per this project's convention for UI changes (type-checking and a clean build prove the code compiles, not that it looks right).

- [ ] **Step 1: Start the dev server**

Run: `npm run dev` (from `apps/customer-portal`), confirm it's serving on `http://localhost:3001` (or whatever port it binds to — check the terminal output) before proceeding. If the backend API isn't already running, data-dependent pages (dashboard, requests, notifications) will show their error states rather than real data — that's fine for a pure visual check of the redesign; if you want live data, `cd docker && docker compose up -d postgres backend-api` first (see the repo root `README.md`), then log in as `customer@limitflow.demo` / `Password123!`.

- [ ] **Step 2: Visually check every redesigned surface at mobile width (390px, this app's actual target — it's a mobile-first single-column layout)**

Using a real browser (not just reading the code), view each of:
- `/login` — gradient brand mark renders, indigo focus rings on the inputs, no leftover blue/slate colors.
- `/dashboard` — the gradient hero card renders with the radial glow, progress bar, and tabular-mono currency figures; either the accent-tinted active-request banner or the "Increase my limit" button shows depending on account state.
- `/increase-limit` — walk through at least the "amount" step (tabular-mono digit input) and confirm the OTP step's input renders with the mono/tracking-widest treatment.
- `/requests` and a request detail page — timeline renders with the new accent/success colors, no leftover blue-600/emerald-600.
- `/notifications`, `/profile`, `/support` — confirm the shared token migration reads consistently (no page looks visually orphaned from the rest).
- Bottom nav and top bar — active tab shows in accent color.

Confirm nothing regressed: every interactive element (buttons, inputs, links) is still clickable and legible, focus rings are visible on keyboard tab, and no layout is broken (no text overflowing a card, no misaligned icons).

- [ ] **Step 3: Confirm the dark-mode token block is correct (code-level, since this app has no in-browser theme toggle to trigger it live)**

Read `apps/customer-portal/src/app/globals.css` and confirm the `@media (prefers-color-scheme: dark)` block defines all 14 tokens with the dark values from this plan's Global Constraints table — this is what a user with system dark mode enabled will actually see; there's no toggle to click, so this is a direct code check rather than a live one.

- [ ] **Step 4: Final grep confirmation**

Run: `grep -rln "slate-\|blue-600\|blue-100\|blue-50\|blue-700\|emerald-\|amber-\|red-600\|red-200" apps/customer-portal/src` (from the repo root)
Expected: no matches. If anything remains, that file was missed by an earlier task — fix it there and re-run this check.

- [ ] **Step 5: Commit** (only if Step 4 found and fixed something; otherwise this task produces no diff)

```bash
git add -A
git commit -m "Final cleanup pass after customer portal redesign"
```

## Self-Review Notes

- Every task's diff is scoped to `className` strings (plus two intentionally bespoke files: `limit-summary-card.tsx` and `active-request-banner.tsx`, which drop the shared `Card` wrapper for their bespoke treatment) — no hook, prop, or data-fetching logic changes anywhere, matching the Global Constraints.
- `status-badge.tsx` deliberately has no task — it was already fully covered by Task 1's `badge.tsx` re-theme, and touching it would be a no-op edit.
- No new dependencies, no new fonts, no dark-mode toggle — all per Global Constraints.
