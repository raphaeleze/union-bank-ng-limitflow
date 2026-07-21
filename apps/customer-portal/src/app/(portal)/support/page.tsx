import { Mail, MessageCircle, Phone } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";

const CHANNELS = [
  { icon: Phone, label: "Call us", value: "0700-LIMITFLOW", href: "tel:0700546483569" },
  { icon: Mail, label: "Email us", value: "support@limitflow.demo", href: "mailto:support@limitflow.demo" },
];

export default function SupportPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-ink">Support</h1>
        <p className="text-sm text-ink-muted">Need help with a request? Reach out any time.</p>
      </div>

      <Card>
        <CardContent className="divide-y divide-border p-5">
          {CHANNELS.map((channel) => (
            <a
              key={channel.label}
              href={channel.href}
              className="flex items-center gap-3 py-3 first:pt-0 last:pb-0"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-accent-soft text-accent">
                <channel.icon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-medium text-ink">{channel.label}</p>
                <p className="text-sm text-ink-muted">{channel.value}</p>
              </div>
            </a>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex items-start gap-3 p-5">
          <MessageCircle className="mt-0.5 h-5 w-5 shrink-0 text-ink-muted" />
          <p className="text-sm text-ink-muted">
            If a request lands in review, our team follows up using the details on your profile —
            no need to call in just to check on it. Track progress any time from the Requests tab.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
