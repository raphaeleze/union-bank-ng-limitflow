import { Text, type TextProps } from "react-native";

import { cn } from "@/lib/utils";

export function Label({ className, ...props }: TextProps & { className?: string }) {
  return <Text className={cn("text-sm font-medium text-ink dark:text-ink-dark", className)} {...props} />;
}
