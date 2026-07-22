import * as Linking from "expo-linking";
import { Mail, MessageCircle, Phone } from "lucide-react-native";
import { Pressable, ScrollView, Text, View } from "react-native";

import { Card, CardContent } from "@/components/ui/card";

const CHANNELS = [
  { icon: Phone, label: "Call us", value: "0700-LIMITFLOW", href: "tel:0700546483569" },
  { icon: Mail, label: "Email us", value: "support@limitflow.demo", href: "mailto:support@limitflow.demo" },
];

export default function SupportScreen() {
  return (
    <ScrollView className="flex-1 bg-surface px-4 pt-4 dark:bg-surface-dark" contentContainerClassName="gap-4 pb-8">
      <View>
        <Text className="text-xl font-semibold text-ink dark:text-ink-dark">Support</Text>
        <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">Need help with a request? Reach out any time.</Text>
      </View>

      <Card>
        <CardContent className="gap-0">
          {CHANNELS.map((channel) => (
            <Pressable key={channel.label} className="flex-row items-center gap-3 py-3" onPress={() => Linking.openURL(channel.href)}>
              <View className="h-9 w-9 items-center justify-center rounded-full bg-accent-soft dark:bg-accent-soft-dark">
                <channel.icon size={16} color="#5b3df5" />
              </View>
              <View>
                <Text className="text-sm font-medium text-ink dark:text-ink-dark">{channel.label}</Text>
                <Text className="text-sm text-ink-muted dark:text-ink-muted-dark">{channel.value}</Text>
              </View>
            </Pressable>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex-row items-start gap-3">
          <MessageCircle size={20} color="#6b6580" />
          <Text className="flex-1 text-sm text-ink-muted dark:text-ink-muted-dark">
            If a request lands in review, our team follows up using the details on your profile — no need to
            call in just to check on it. Track progress any time from the Requests tab.
          </Text>
        </CardContent>
      </Card>
    </ScrollView>
  );
}
