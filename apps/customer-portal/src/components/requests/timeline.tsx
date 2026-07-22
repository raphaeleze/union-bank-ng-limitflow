import { Check, Loader2 } from "lucide-react";

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
                {isCurrent && <Loader2 className="h-3.5 w-3.5 motion-safe:animate-spin" />}
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
