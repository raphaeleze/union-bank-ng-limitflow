import { Text, View, type TextProps, type ViewProps } from "react-native";

import { cn } from "@/lib/utils";

export function Card({ className, ...props }: ViewProps & { className?: string }) {
  return (
    <View
      className={cn("rounded-2xl border border-border bg-card dark:border-border-dark dark:bg-card-dark", className)}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("gap-1 p-6 pb-0", className)} {...props} />;
}

export function CardTitle({ className, ...props }: TextProps & { className?: string }) {
  return <Text className={cn("text-lg font-semibold text-ink dark:text-ink-dark", className)} {...props} />;
}

export function CardContent({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("p-6", className)} {...props} />;
}
