import { Check } from "lucide-react-native";
import { Pressable, View } from "react-native";

import { cn } from "@/lib/utils";

export function Checkbox({
  checked,
  onValueChange,
}: {
  checked: boolean;
  onValueChange: (value: boolean) => void;
}) {
  return (
    <Pressable
      accessibilityRole="checkbox"
      accessibilityState={{ checked }}
      onPress={() => onValueChange(!checked)}
      className={cn(
        "h-4 w-4 items-center justify-center rounded border border-border bg-card dark:border-border-dark dark:bg-card-dark",
        checked && "border-accent bg-accent dark:border-accent-dark dark:bg-accent-dark",
      )}
    >
      {checked && <Check size={12} color="#ffffff" />}
    </Pressable>
  );
}
