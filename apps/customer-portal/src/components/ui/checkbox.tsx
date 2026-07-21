import { Check } from "lucide-react";
import * as React from "react";

import { cn } from "@/lib/utils";

export const Checkbox = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <span className="relative inline-flex h-4 w-4 shrink-0">
      <input
        type="checkbox"
        ref={ref}
        className={cn(
          "peer h-4 w-4 shrink-0 appearance-none rounded border border-border bg-card checked:border-accent checked:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-accent disabled:cursor-not-allowed disabled:opacity-50",
          className,
        )}
        {...props}
      />
      <Check className="pointer-events-none absolute inset-0 h-4 w-4 p-px text-white opacity-0 peer-checked:opacity-100" />
    </span>
  ),
);
Checkbox.displayName = "Checkbox";
