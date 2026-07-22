import { TextInput, type TextInputProps } from "react-native";

import { cn } from "@/lib/utils";

export interface InputProps extends TextInputProps {
  className?: string;
  invalid?: boolean;
}

export function Input({ className, invalid, ...props }: InputProps) {
  return (
    <TextInput
      className={cn(
        "h-10 rounded-lg border border-border bg-card px-3 text-sm text-ink dark:border-border-dark dark:bg-card-dark dark:text-ink-dark",
        invalid && "border-danger dark:border-danger-dark",
        className,
      )}
      placeholderTextColor="#6b6580"
      {...props}
    />
  );
}
