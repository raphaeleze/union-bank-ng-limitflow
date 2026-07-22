import { Check, Loader2 } from "lucide-react-native";
import { Text, View } from "react-native";

import { cn } from "@/lib/utils";
import type { TimelineStepStatus } from "@/lib/types";

export function Timeline({ steps }: { steps: { label: string; status: TimelineStepStatus }[] }) {
  return (
    <View>
      {steps.map((step, index) => {
        const isComplete = step.status === "COMPLETE";
        const isCurrent = step.status === "CURRENT";
        const isLast = index === steps.length - 1;

        return (
          <View key={step.label} className="flex-row gap-3">
            <View className="items-center">
              <View
                className={cn(
                  "h-6 w-6 items-center justify-center rounded-full border-2",
                  isComplete && "border-success bg-success dark:border-success-dark dark:bg-success-dark",
                  isCurrent && "border-accent bg-accent dark:border-accent-dark dark:bg-accent-dark",
                  !isComplete && !isCurrent && "border-border bg-card dark:border-border-dark dark:bg-card-dark",
                )}
              >
                {isComplete && <Check size={14} color="#ffffff" />}
                {isCurrent && <Loader2 size={14} color="#ffffff" />}
              </View>
              {!isLast && (
                <View className={cn("w-0.5 flex-1", isComplete ? "bg-success dark:bg-success-dark" : "bg-border dark:bg-border-dark")} />
              )}
            </View>
            <Text
              className={cn(
                "pb-6 text-sm",
                isCurrent ? "font-semibold text-ink dark:text-ink-dark" : "text-ink-muted dark:text-ink-muted-dark",
              )}
            >
              {step.label}
            </Text>
          </View>
        );
      })}
    </View>
  );
}
