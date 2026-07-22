import { Link } from "expo-router";
import { LogOut, Mail, User as UserIcon } from "lucide-react-native";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/lib/auth";

export default function ProfileScreen() {
  const { user, logout } = useAuth();

  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Profile</Text>

      <Card>
        <CardContent className="flex-row items-center gap-4">
          <View className="h-14 w-14 items-center justify-center rounded-full bg-accent dark:bg-accent-dark">
            <Text className="text-lg font-semibold text-white">
              {user?.firstName?.[0]}
              {user?.lastName?.[0]}
            </Text>
          </View>
          <View>
            <Text className="text-base font-medium text-ink dark:text-ink-dark">
              {user?.firstName} {user?.lastName}
            </Text>
            <View className="flex-row items-center gap-1">
              <Mail size={14} color="#6b6580" />
              <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{user?.email}</Text>
            </View>
          </View>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Link href="/support" asChild>
            <Pressable className="flex-row items-center gap-3">
              <UserIcon size={16} color="#6b6580" />
              <Text className="text-sm font-medium text-ink dark:text-ink-dark">Contact support</Text>
            </Pressable>
          </Link>
        </CardContent>
      </Card>

      <Button variant="outline" onPress={logout}>
        <View className="flex-row items-center gap-2">
          <LogOut size={16} color="#151222" />
          <Text className="text-sm font-medium text-ink dark:text-ink-dark">Log out</Text>
        </View>
      </Button>
    </ScrollView>
  );
}
