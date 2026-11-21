import { useState, type KeyboardEvent } from "react";
import { MAX_MESSAGE_LENGTH } from "../types/chat";

interface Props {
  disabled: boolean;
  onSend: (text: string) => void;
}

/**
 * Message box. Blocks empty or whitespace-only text (spec edge case) and text over the 2,000-character
 * limit (FR-012). Enter sends; Shift+Enter inserts a newline.
 */
export function MessageInput({ disabled, onSend }: Props) {
  const [text, setText] = useState("");
  const trimmed = text.trim();
  const overLimit = text.length > MAX_MESSAGE_LENGTH;
  const canSend = !disabled && trimmed.length > 0 && !overLimit;

  function send() {
    if (!canSend) {
      return;
    }
    onSend(trimmed);
    setText("");
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  return (
    <div className="composer">
      <div className="composer__row">
        <textarea
          rows={2}
          value={text}
          placeholder="Ask about the Caramel AI inference API…"
          aria-label="Message"
          disabled={disabled}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <button type="button" className="primary" onClick={send} disabled={!canSend}>
          Send
        </button>
      </div>
      {overLimit ? (
        <div className="composer__hint composer__hint--over">
          Messages must be {MAX_MESSAGE_LENGTH.toLocaleString("en-US")} characters or fewer.
        </div>
      ) : (
        <div className="composer__hint">
          {text.length.toLocaleString("en-US")} / {MAX_MESSAGE_LENGTH.toLocaleString("en-US")}
        </div>
      )}
    </div>
  );
}
