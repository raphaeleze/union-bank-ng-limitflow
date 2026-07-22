import { ActivityIndicator, Pressable, Text, type PressableProps } from "react-native";

import { cn } from "@/lib/utils";

type Variant = "default" | "outline" | "ghost" | "destructive";

const VARIANT_CLASSES: Record<Variant, string> = {
  default: "bg-accent dark:bg-accent-dark",
  outline: "border border-border bg-card dark:border-border-dark dark:bg-card-dark",
  ghost: "bg-transparent",
  destructive: "bg-danger dark:bg-danger-dark",
};

const VARIANT_TEXT_CLASSES: Record<Variant, string> = {
  default: "text-white",
  outline: "text-ink dark:text-ink-dark",
  ghost: "text-ink-muted dark:text-ink-muted-dark",
  destructive: "text-white",
};

export interface ButtonProps extends Omit<PressableProps, "children"> {
  variant?: Variant;
  loading?: boolean;
  children: React.ReactNode;
  className?: string;
}

export function Button({
  variant = "default",
  loading = false,
  disabled,
  children,
  className,
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      className={cn(
        "h-10 flex-row items-center justify-center gap-2 rounded-lg px-4",
        VARIANT_CLASSES[variant],
        isDisabled && "opacity-50",
        className,
      )}
      {...props}
    >
      {loading && <ActivityIndicator size="small" color={variant === "outline" || variant === "ghost" ? "#5b3df5" : "#ffffff"} />}
      {typeof children === "string" ? (
        <Text className={cn("text-sm font-medium", VARIANT_TEXT_CLASSES[variant])}>{children}</Text>
      ) : (
        children
      )}
    </Pressable>
  );
}
