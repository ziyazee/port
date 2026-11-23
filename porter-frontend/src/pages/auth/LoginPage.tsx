import { AuthHero } from "@/features/auth/components/AuthHero";
import { LoginForm } from "@/features/auth/components/LoginForm";

type LoginPageProps = {
  onLogin: (session: LoginSession, remember: boolean) => void;
};

type LoginSession = {
  username: string;
  role: "ADMIN" | "OPERATIONS";
  displayName: string;
};

export function LoginPage({ onLogin }: LoginPageProps) {
  return (
    <main className="auth-page">
      <AuthHero />
      <LoginForm onLogin={onLogin} />
    </main>
  );
}
