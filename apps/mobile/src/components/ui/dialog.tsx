import { Modal, Pressable, Text, View, type ViewProps } from "react-native";

import { cn } from "@/lib/utils";

export function Dialog({
  open,
  onOpenChange,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
}) {
  return (
    <Modal visible={open} transparent animationType="fade" onRequestClose={() => onOpenChange(false)}>
      <Pressable
        className="flex-1 items-center justify-center bg-ink/40 px-4"
        onPress={() => onOpenChange(false)}
      >
        {/* An empty onPress (not stopPropagation, which GestureResponderEvent doesn't
            reliably expose) makes this Pressable claim the touch responder for taps inside
            the dialog card, so they don't fall through to the scrim's dismiss handler. */}
        <Pressable onPress={() => {}} className="w-full max-w-md">
          {children}
        </Pressable>
      </Pressable>
    </Modal>
  );
}

export function DialogContent({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("rounded-2xl bg-card p-6 dark:bg-card-dark", className)} {...props} />;
}

export function DialogHeader({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("mb-4 gap-1", className)} {...props} />;
}

export function DialogTitle({ children }: { children: React.ReactNode }) {
  return <Text className="text-lg font-semibold text-ink dark:text-ink-dark">{children}</Text>;
}

export function DialogDescription({ children }: { children: React.ReactNode }) {
  return <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{children}</Text>;
}

export function DialogFooter({ className, ...props }: ViewProps & { className?: string }) {
  return <View className={cn("mt-6 flex-row justify-end gap-2", className)} {...props} />;
}
