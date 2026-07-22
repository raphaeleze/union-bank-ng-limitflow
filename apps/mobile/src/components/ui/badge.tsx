import { Text, View } from "react-native";

import { cn } from "@/lib/utils";

type Variant = "neutral" | "blue" | "green" | "orange" | "red";

const VARIANT_CLASSES: Record<Variant, string> = {
  neutral: "bg-border dark:bg-border-dark",
  blue: "bg-accent-soft dark:bg-accent-soft-dark",
  green: "bg-success-soft dark:bg-success-soft-dark",
  orange: "bg-warning-soft dark:bg-warning-soft-dark",
  red: "bg-danger-soft dark:bg-danger-soft-dark",
};

const VARIANT_TEXT_CLASSES: Record<Variant, string> = {
  neutral: "text-ink-muted dark:text-ink-muted-dark",
  blue: "text-accent dark:text-accent-dark",
  green: "text-success-strong dark:text-success-strong-dark",
  orange: "text-warning-strong dark:text-warning-strong-dark",
  red: "text-danger-strong dark:text-danger-strong-dark",
};

export function Badge({ variant = "neutral", children }: { variant?: Variant; children: React.ReactNode }) {
  return (
    <View className={cn("rounded-full px-2.5 py-0.5", VARIANT_CLASSES[variant])}>
      <Text className={cn("text-xs font-semibold", VARIANT_TEXT_CLASSES[variant])}>{children}</Text>
    </View>
  );
}
