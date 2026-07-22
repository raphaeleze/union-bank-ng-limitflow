import { TextInput, type TextInputProps } from "react-native";

import { cn } from "@/lib/utils";

export function Textarea({ className, ...props }: TextInputProps & { className?: string }) {
  return (
    <TextInput
      multiline
      textAlignVertical="top"
      className={cn(
        "min-h-[90px] rounded-lg border border-border bg-card px-3 py-2 text-sm text-ink dark:border-border-dark dark:bg-card-dark dark:text-ink-dark",
        className,
      )}
      placeholderTextColor="#6b6580"
      {...props}
    />
  );
}
