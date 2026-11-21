import { useRef, useState } from "react";
import { ErrorBanner } from "../components/ErrorBanner";
import { MessageInput } from "../components/MessageInput";
import { MessageList } from "../components/MessageList";
import { NewChatButton } from "../components/NewChatButton";
import { StreamingIndicator } from "../components/StreamingIndicator";
import { streamChat } from "../services/chatClient";
import { newSessionId } from "../services/sessionId";
import type { Message, ReplyStatus } from "../types/chat";

export function ChatPage() {
  const [sessionId, setSessionId] = useState<string>(() => newSessionId());
  const [messages, setMessages] = useState<Message[]>([]);
  const [replyStatus, setReplyStatus] = useState<ReplyStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const lastUserMessage = useRef<string | null>(null);

  const busy = replyStatus === "PENDING" || replyStatus === "STREAMING";

  async function send(text: string, isRetry: boolean) {
    lastUserMessage.current = text;
    setError(null);
    setReplyStatus("PENDING");

    if (!isRetry) {
      setMessages((prev) => [
        ...prev,
        { role: "USER", text, createdAt: new Date().toISOString() },
      ]);
    }

    // Each call owns its own flag, so tokens from one reply never leak into another.
    let assistantStarted = false;

    await streamChat(sessionId, text, {
      onToken: (fragment) => {
        setReplyStatus("STREAMING");
        // Decided outside the updater: StrictMode may run updaters twice, and the flag must not change between runs.
        const isFirstToken = !assistantStarted;
        assistantStarted = true;
        setMessages((prev) => {
          if (isFirstToken) {
            return [
              ...prev,
              { role: "ASSISTANT", text: fragment, createdAt: new Date().toISOString() },
            ];
          }
          const last = prev[prev.length - 1];
          return [...prev.slice(0, -1), { ...last, text: last.text + fragment }];
        });
      },
      onDone: () => {
        setReplyStatus("COMPLETE");
      },
      onError: (message) => {
        // Partial reply text stays visible. The user can retry the same message.
        setReplyStatus("FAILED");
        setError(message);
      },
    });
  }

  function handleNewChat() {
    setSessionId(newSessionId());
    setMessages([]);
    setReplyStatus(null);
    setError(null);
    lastUserMessage.current = null;
  }

  function handleRetry() {
    const text = lastUserMessage.current;
    if (text) {
      void send(text, true);
    }
  }

  return (
    <div className="chat">
      <header className="chat__header">
        <h1 className="chat__title">Caramel AI Tech Support</h1>
        <NewChatButton onNewChat={handleNewChat} disabled={busy} />
      </header>

      <MessageList messages={messages} />

      <StreamingIndicator visible={busy} />

      {error && (
        <ErrorBanner message={error} onRetry={handleRetry} retryDisabled={busy} />
      )}

      <MessageInput disabled={busy} onSend={(text) => void send(text, false)} />
    </div>
  );
}
