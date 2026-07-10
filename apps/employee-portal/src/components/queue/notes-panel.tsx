"use client";

import { formatDistanceToNow } from "date-fns";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { useAddNoteMutation, useRequestNotesQuery } from "@/hooks/use-request-detail";
import { ApiError } from "@/lib/api-client";

export function NotesPanel({ requestId }: { requestId: string }) {
  const { data: notes, isLoading } = useRequestNotesQuery(requestId);
  const addNote = useAddNoteMutation();
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const submit = async () => {
    const note = draft.trim();
    if (!note) return;
    setError(null);
    try {
      await addNote.mutateAsync({ requestId, note });
      setDraft("");
      toast("Note added.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't save that note.");
    }
  };

  return (
    <div className="space-y-4">
      {isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-14" />
          <Skeleton className="h-14" />
        </div>
      ) : !notes || notes.length === 0 ? (
        <p className="text-sm text-slate-500">No notes yet.</p>
      ) : (
        <ul className="space-y-3">
          {notes.map((note) => (
            <li key={note.id} className="rounded-lg bg-slate-50 p-3">
              <p className="text-sm text-slate-800">{note.note}</p>
              <p className="mt-1 text-xs text-slate-400">
                {note.authorName} · {formatDistanceToNow(new Date(note.createdAt), { addSuffix: true })}
              </p>
            </li>
          ))}
        </ul>
      )}

      <div className="space-y-2">
        <Textarea
          placeholder="Leave a note for this case…"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button size="sm" onClick={submit} disabled={addNote.isPending || !draft.trim()}>
          {addNote.isPending ? "Saving…" : "Add note"}
        </Button>
      </div>
    </div>
  );
}
