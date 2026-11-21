interface Props {
  visible: boolean;
}

/** Shown from send until the reply completes or fails (spec FR-004). */
export function StreamingIndicator({ visible }: Props) {
  if (!visible) {
    return null;
  }
  return (
    <div className="streaming" role="status">
      Caramel AI is typing…
    </div>
  );
}
