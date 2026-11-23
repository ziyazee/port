import { useState, type FormEvent } from "react";
import { apiClient } from "@/services/apiClient";

type OpsAccountFormProps = {
  onCancel: () => void;
};

type LoginResponse = {
  username: string;
  role: "ADMIN" | "OPERATIONS";
  displayName: string;
};

export function OpsAccountForm({ onCancel }: OpsAccountFormProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [verifyPassword, setVerifyPassword] = useState("");
  const [createdAccount, setCreatedAccount] = useState<LoginResponse | null>(null);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setCreatedAccount(null);

    const trimmedUsername = username.trim();

    if (!trimmedUsername) {
      setError("Username is required.");
      return;
    }

    if (!password.trim()) {
      setError("Password is required.");
      return;
    }

    if (password !== verifyPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);

    try {
      const account = await apiClient.post<LoginResponse>("/auth/signup/operations", {
        username: trimmedUsername,
        password,
        displayName: trimmedUsername,
      });

      setCreatedAccount(account);
      setUsername("");
      setPassword("");
      setVerifyPassword("");
    } catch {
      setError("Could not create the operations account. Check the username and try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="ops-form" onSubmit={handleSubmit}>
      <section className="customer-form-section ops-form-section">
        <header>
          <span className="material-symbols-outlined">manage_accounts</span>
          <h2>Account Credentials</h2>
        </header>

        <div className="ops-form-grid">
          <label className="customer-field customer-field--full">
            <span>
              User Name <span className="required-mark">*</span>
            </span>
            <input
              autoComplete="username"
              onChange={(event) => setUsername(event.target.value)}
              placeholder="dispatcher"
              required
              type="text"
              value={username}
            />
          </label>

          <label className="customer-field">
            <span>
              Password <span className="required-mark">*</span>
            </span>
            <input
              autoComplete="new-password"
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter password"
              required
              type="password"
              value={password}
            />
          </label>

          <label className="customer-field">
            <span>
              Verify Password <span className="required-mark">*</span>
            </span>
            <input
              autoComplete="new-password"
              onChange={(event) => setVerifyPassword(event.target.value)}
              placeholder="Re-enter password"
              required
              type="password"
              value={verifyPassword}
            />
          </label>
        </div>

        {error ? (
          <p className="ops-form__message ops-form__message--error" role="alert">
            {error}
          </p>
        ) : null}

        {createdAccount ? (
          <p className="ops-form__message ops-form__message--success" role="status">
            Operations account created for {createdAccount.username}.
          </p>
        ) : null}
      </section>

      <footer className="customer-action-bar ops-action-bar">
        <button
          className="customer-button customer-button--secondary"
          onClick={onCancel}
          type="button"
        >
          Cancel
        </button>
        <button
          className="customer-button customer-button--primary"
          disabled={isSubmitting}
          type="submit"
        >
          <span className="material-symbols-outlined">person_add</span>
          {isSubmitting ? "Creating..." : "Create Ops Account"}
        </button>
      </footer>
    </form>
  );
}
