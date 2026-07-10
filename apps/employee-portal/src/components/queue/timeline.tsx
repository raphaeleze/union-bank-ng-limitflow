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
                  isComplete && "border-emerald-500 bg-emerald-500 text-white",
                  isCurrent && "border-blue-600 bg-blue-600 text-white",
                  !isComplete && !isCurrent && "border-slate-300 bg-white",
                )}
              >
                {isComplete && <Check className="h-3.5 w-3.5" />}
              </div>
              {!isLast && (
                <div
                  className={cn(
                    "w-0.5 flex-1",
                    isComplete ? "bg-emerald-500" : "bg-slate-200",
                  )}
                />
              )}
            </div>
            <div className={cn("pb-6 text-sm", isCurrent ? "font-semibold text-slate-900" : "text-slate-600")}>
              {step.label}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
