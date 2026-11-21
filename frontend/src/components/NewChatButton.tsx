interface Props {
  onNewChat: () => void;
  disabled: boolean;
}

/** Clears the window and starts a fresh session (spec US4). */
export function NewChatButton({ onNewChat, disabled }: Props) {
  return (
    <button type="button" onClick={onNewChat} disabled={disabled}>
      New chat
    </button>
  );
}
