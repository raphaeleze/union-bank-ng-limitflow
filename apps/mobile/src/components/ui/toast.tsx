import { AccessibilityInfo, Pressable, Text, View } from "react-native";
import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { X } from "lucide-react-native";

import { cn } from "@/lib/utils";

interface Toast {
  id: number;
  title: string;
  variant: "success" | "error";
}

interface ToastContextValue {
  toast: (title: string, variant?: Toast["variant"]) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback(
    (title: string, variant: Toast["variant"] = "success") => {
      const id = nextId++;
      setToasts((current) => [...current, { id, title, variant }]);
      if (variant === "error") {
        AccessibilityInfo.announceForAccessibility(title);
      }
      setTimeout(() => dismiss(id), 4000);
    },
    [dismiss],
  );

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <View pointerEvents="box-none" className="absolute inset-x-4 bottom-24 gap-2">
        {toasts.map((t) => (
          <View
            key={t.id}
            className={cn(
              "flex-row items-center gap-2 rounded-lg border px-4 py-3",
              t.variant === "success"
                ? "border-success/30 bg-success-soft dark:bg-success-soft-dark"
                : "border-danger/30 bg-danger-soft dark:bg-danger-soft-dark",
            )}
          >
            <Text
              className={cn(
                "flex-1 text-sm",
                t.variant === "success"
                  ? "text-success-strong dark:text-success-strong-dark"
                  : "text-danger-strong dark:text-danger-strong-dark",
              )}
            >
              {t.title}
            </Text>
            <Pressable onPress={() => dismiss(t.id)} accessibilityLabel="Dismiss">
              <X size={14} color="#6b6580" />
            </Pressable>
          </View>
        ))}
      </View>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
