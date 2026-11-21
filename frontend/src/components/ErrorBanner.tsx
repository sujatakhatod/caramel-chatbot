interface Props {
  message: string;
  onRetry: () => void;
  retryDisabled: boolean;
}

/** Friendly error with a retry action. Shows only the user-safe text it is given. */
export function ErrorBanner({ message, onRetry, retryDisabled }: Props) {
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      <button type="button" onClick={onRetry} disabled={retryDisabled}>
        Retry
      </button>
    </div>
  );
}
