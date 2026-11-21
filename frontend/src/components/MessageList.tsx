import { useEffect, useRef } from "react";
import type { Message } from "../types/chat";

interface Props {
  messages: Message[];
}

/** Shows the whole session history and keeps the newest message in view (US2). */
export function MessageList({ messages }: Props) {
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: "end" });
  }, [messages]);

  return (
    <div className="messages" aria-live="polite">
      {messages.map((m, i) => (
        <div
          key={`${m.createdAt}-${i}`}
          className={`message ${m.role === "USER" ? "message--user" : "message--assistant"}`}
        >
          {m.text}
        </div>
      ))}
      <div ref={endRef} />
    </div>
  );
}
