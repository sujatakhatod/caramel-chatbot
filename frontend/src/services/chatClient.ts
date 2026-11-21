/**
 * Client for POST /api/chat. Reads the Server-Sent Events stream with fetch, because EventSource
 * cannot send a POST body. The client holds no API key.
 */

export interface StreamHandlers {
  onToken: (text: string) => void;
  onDone: () => void;
  onError: (message: string) => void;
}

export const FRIENDLY_ERROR = "Sorry, something went wrong. Please try again.";

// Must match server.port in backend/src/main/resources/application.yml.
const API_BASE: string = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

interface SseEvent {
  event: string;
  data: Record<string, unknown>;
}

function parseBlock(block: string): SseEvent | null {
  let event = "message";
  const dataLines: string[] = [];
  for (const line of block.split("\n")) {
    if (line.startsWith("event:")) {
      event = line.slice("event:".length).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice("data:".length).trim());
    }
  }
  if (dataLines.length === 0) {
    return null;
  }
  try {
    return { event, data: JSON.parse(dataLines.join("\n")) as Record<string, unknown> };
  } catch {
    return null;
  }
}

/**
 * Sends one user message and streams the reply. Exactly one of onDone or onError is called,
 * except when the network drops mid-stream, in which case onError is called.
 */
export async function streamChat(
  sessionId: string,
  message: string,
  handlers: StreamHandlers,
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
      body: JSON.stringify({ sessionId, message }),
    });
  } catch {
    handlers.onError(FRIENDLY_ERROR);
    return;
  }

  if (!response.ok) {
    // Pre-stream rejections (400, 403, 413, 429) return a JSON body with a user-safe "error" text.
    let message = FRIENDLY_ERROR;
    try {
      const body = (await response.json()) as { error?: unknown };
      if (typeof body.error === "string") {
        message = body.error;
      }
    } catch {
      // Non-JSON error body: keep the friendly default.
    }
    handlers.onError(message);
    return;
  }

  if (!response.body) {
    handlers.onError(FRIENDLY_ERROR);
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let finished = false;

  try {
    while (!finished) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });

      let boundary = buffer.indexOf("\n\n");
      while (boundary !== -1 && !finished) {
        const block = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        boundary = buffer.indexOf("\n\n");

        const parsed = parseBlock(block);
        if (!parsed) {
          continue;
        }
        if (parsed.event === "token") {
          handlers.onToken(String(parsed.data.text ?? ""));
        } else if (parsed.event === "done") {
          finished = true;
          handlers.onDone();
        } else if (parsed.event === "error") {
          finished = true;
          handlers.onError(String(parsed.data.message ?? FRIENDLY_ERROR));
        }
      }
    }
  } catch {
    if (!finished) {
      finished = true;
      handlers.onError(FRIENDLY_ERROR);
    }
  } finally {
    await reader.cancel().catch(() => undefined);
  }

  if (!finished) {
    // The stream ended without a terminal event.
    handlers.onError(FRIENDLY_ERROR);
  }
}
