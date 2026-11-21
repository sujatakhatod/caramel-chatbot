/**
 * Generates a client-side session ID. The caller keeps it in memory only: chat history must not
 * persist across sessions (spec FR-017), so this is never written to localStorage.
 */
export function newSessionId(): string {
  return crypto.randomUUID();
}
