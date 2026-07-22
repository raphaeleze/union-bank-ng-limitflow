import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "expo-router";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { Text, View } from "react-native";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Enter your password"),
});

type LoginValues = z.infer<typeof loginSchema>;

export default function LoginScreen() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "customer@limitflow.demo", password: "" },
  });

  const onSubmit = async (values: LoginValues) => {
    setServerError(null);
    try {
      await login(values.email, values.password);
      router.replace("/(app)");
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : (error as Error).message);
    }
  };

  return (
    <View className="flex-1 justify-center bg-surface px-4 dark:bg-surface-dark">
      <View className="mb-8 items-center gap-2">
        <View className="h-12 w-12 items-center justify-center rounded-2xl bg-accent dark:bg-accent-dark">
          <Text className="text-xl font-bold text-white">LF</Text>
        </View>
        <Text className="text-2xl font-semibold text-ink dark:text-ink-dark">LimitFlow</Text>
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">
          Sign in to manage your transfer limit.
        </Text>
      </View>

      <View className="gap-4">
        <View className="gap-1.5">
          <Label>Email</Label>
          <Controller
            control={control}
            name="email"
            render={({ field }) => (
              <Input
                value={field.value}
                onChangeText={field.onChange}
                autoCapitalize="none"
                keyboardType="email-address"
                accessibilityLabel="Email"
              />
            )}
          />
          {errors.email && <Text className="text-xs text-danger dark:text-danger-dark">{errors.email.message}</Text>}
        </View>

        <View className="gap-1.5">
          <Label>Password</Label>
          <Controller
            control={control}
            name="password"
            render={({ field }) => (
              <Input
                value={field.value}
                onChangeText={field.onChange}
                secureTextEntry
                accessibilityLabel="Password"
              />
            )}
          />
          {errors.password && (
            <Text className="text-xs text-danger dark:text-danger-dark">{errors.password.message}</Text>
          )}
        </View>

        {serverError && <Text className="text-sm text-danger dark:text-danger-dark">{serverError}</Text>}

        <Button loading={isSubmitting} onPress={handleSubmit(onSubmit)}>
          {isSubmitting ? "Signing in…" : "Sign in"}
        </Button>
      </View>

      <Text className="mt-6 text-center text-xs text-ink-muted dark:text-ink-muted-dark">
        Demo account: customer@limitflow.demo — password Password123!
      </Text>
    </View>
  );
}
