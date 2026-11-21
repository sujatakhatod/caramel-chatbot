/** Conversational role. SYSTEM messages exist only on the server and are never shown to the user. */
export type Role = "USER" | "ASSISTANT";

export interface Message {
  role: Role;
  text: string;
  createdAt: string;
}

/** State of one assistant reply (data-model.md, ReplyStatus). */
export type ReplyStatus = "PENDING" | "STREAMING" | "COMPLETE" | "FAILED";

/** Maximum message length in characters (spec FR-012). Matches chat.max-message-length. */
export const MAX_MESSAGE_LENGTH = 2000;
