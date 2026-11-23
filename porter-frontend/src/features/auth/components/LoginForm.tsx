import { useState, type FormEvent } from "react";
import { PorterBrand } from "@/components/brand/PorterBrand";
import { apiClient } from "@/services/apiClient";

type LoginFormProps = {
  onLogin: (session: LoginResponse, remember: boolean) => void;
};

type LoginResponse = {
  username: string;
  role: "ADMIN" | "OPERATIONS";
  displayName: string;
};

export function LoginForm({ onLogin }: LoginFormProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(true);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      const session = await apiClient.post<LoginResponse>("/auth/login", {
        username,
        password,
      });
      onLogin(session, remember);
    } catch (error) {
      console.error("Login failed", {
        username,
        error,
      });
      setError("Invalid username or password.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="login-panel" aria-label="Sign in">
      <div className="login-card">
        <header className="login-card__header">
          <PorterBrand />
          <div>
            <h2>Welcome Back</h2>
            <p>Sign in to continue to your dashboard.</p>
          </div>
        </header>

        <form
          className="login-form"
          onSubmit={handleSubmit}
        >
          <div className="form-field">
            <label htmlFor="username">Username</label>
            <input
              autoComplete="username"
              id="username"
              onChange={(event) => setUsername(event.target.value)}
              placeholder="admin or operations"
              required
              type="text"
              value={username}
            />
          </div>

          <div className="form-field">
            <div className="form-field__label-row">
              <label htmlFor="password">Password</label>
              <a href="/">Forgot Password?</a>
            </div>
            <input
              autoComplete="current-password"
              id="password"
              onChange={(event) => setPassword(event.target.value)}
              placeholder="••••••••"
              required
              type="password"
              value={password}
            />
          </div>

          {error ? (
            <p className="login-form__error" role="alert">
              {error}
            </p>
          ) : null}

          <label className="remember-control" htmlFor="remember">
            <input
              checked={remember}
              id="remember"
              onChange={(event) => setRemember(event.target.checked)}
              type="checkbox"
            />
            <span>Remember Me</span>
          </label>

          <button className="sign-in-button" disabled={isSubmitting} type="submit">
            {isSubmitting ? "Signing In..." : "Sign In"}
            <span className="material-symbols-outlined">arrow_forward</span>
          </button>
        </form>
      </div>
    </section>
  );
}
