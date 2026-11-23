const previewImage =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuCuauxoVz5BFFv85jkvMq0COBdhGoQVQeDVEiOvtwO5PKdswAbNP6NAwBfyR-XSvLo6ojGoMtFeZpjChq5HPx9rcRQLfyyZGidGQsi-NCKEojWcS2OnD6zOJvqtm0NzTx6E-A41dqO6eWY81b4SO7yrTKvvhBzywGNFMLlywaHC6TuaNXW3e_9Nw9uTKE-vgx0Bj04BiTR3c-nqHZJrhFi9tcOM13D0anxlYpM3LO--U-A5m3eZODLA06zjfThC2jrAy7XrVFtrwFAd";

export function AuthHero() {
  return (
    <section className="auth-hero" aria-label="Porter platform benefits">
      <div className="auth-hero__content">
        <div className="auth-hero__copy">
          <h1>Manage Logistics Operations From One Platform</h1>
          <p>
            Create orders, track vehicles, manage deliveries, and keep transport
            operations moving from one secure workspace.
          </p>
        </div>

        <div className="operations-preview">
          <img src={previewImage} alt="Logistics operations dashboard preview" />
        </div>
      </div>
    </section>
  );
}
