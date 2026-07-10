"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import {
  useApproveMutation,
  useRejectMutation,
  useRequestVerificationMutation,
} from "@/hooks/use-request-detail";
import { ApiError } from "@/lib/api-client";

type ActionKind = "approve" | "reject" | "request-verification" | null;

const COPY: Record<
  Exclude<ActionKind, null>,
  { title: string; description: string; confirmLabel: string; successMessage: string }
> = {
  approve: {
    title: "Approve this request?",
    description: "The customer's daily transfer limit will be updated immediately.",
    confirmLabel: "Approve",
    successMessage: "Request approved — the customer has been notified.",
  },
  reject: {
    title: "Reject this request?",
    description: "The customer will be notified. Add a short reason if you can.",
    confirmLabel: "Reject",
    successMessage: "Request rejected — the customer has been notified.",
  },
  "request-verification": {
    title: "Request additional verification?",
    description: "Sends the customer back through OTP verification before we can decide.",
    confirmLabel: "Request verification",
    successMessage: "Verification requested — the customer has a new code waiting.",
  },
};

export function ReviewActions({ requestId }: { requestId: string }) {
  const [open, setOpen] = useState<ActionKind>(null);
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const approve = useApproveMutation();
  const reject = useRejectMutation();
  const requestVerification = useRequestVerificationMutation();

  const mutation = open === "approve" ? approve : open === "reject" ? reject : requestVerification;

  const close = () => {
    setOpen(null);
    setNote("");
    setError(null);
  };

  const confirm = async () => {
    if (!open) return;
    setError(null);
    try {
      await mutation.mutateAsync({ requestId, note: note.trim() || undefined });
      toast(COPY[open].successMessage);
      close();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    }
  };

  return (
    <>
      <div className="flex flex-wrap gap-2">
        <Button onClick={() => setOpen("approve")}>Approve</Button>
        <Button variant="outline" onClick={() => setOpen("request-verification")}>
          Request verification
        </Button>
        <Button variant="destructive" onClick={() => setOpen("reject")}>
          Reject
        </Button>
      </div>

      <Dialog open={open !== null} onOpenChange={(next) => !next && close()}>
        <DialogContent>
          {open && (
            <>
              <DialogHeader>
                <DialogTitle>{COPY[open].title}</DialogTitle>
                <DialogDescription>{COPY[open].description}</DialogDescription>
              </DialogHeader>

              <Textarea
                placeholder="Add a note (optional)"
                value={note}
                onChange={(event) => setNote(event.target.value)}
              />
              {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

              <DialogFooter>
                <Button variant="outline" onClick={close} disabled={mutation.isPending}>
                  Cancel
                </Button>
                <Button
                  variant={open === "reject" ? "destructive" : "default"}
                  onClick={confirm}
                  disabled={mutation.isPending}
                >
                  {mutation.isPending ? "Saving…" : COPY[open].confirmLabel}
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
