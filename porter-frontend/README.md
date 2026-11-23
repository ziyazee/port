# Scalable React Frontend

React + TypeScript frontend scaffolded with Vite. The app implements the Porter login, operations dashboard, create customer, and create order screens from the supplied references, with feature code, shared brand UI, styling, and API access kept in predictable places as the backend grows.

## Commands

```bash
npm install
npm run dev
npm run build
npm run lint
```

## Run Frontend And Backend With Docker

From the repository root:

```bash
docker compose up --build
```

Open the app at:

```text
http://localhost:5174
```

If your default Node is newer than the Vite toolchain expects, use Homebrew's Node 22 for this project:

```bash
PATH="/opt/homebrew/opt/node@22/bin:$PATH" npm install
PATH="/opt/homebrew/opt/node@22/bin:$PATH" npm run dev
PATH="/opt/homebrew/opt/node@22/bin:$PATH" npm run build
```

## Structure

```text
src/
  app/                 App composition and providers
  components/brand/    Reusable brand elements
  features/auth/       Auth-specific UI components and logic
  features/customers/  Customer-specific UI components and logic
  features/dashboard/  Dashboard-specific UI components and logic
  features/orders/     Order-specific UI components and logic
  pages/auth/          Auth route-level pages
  pages/customers/     Customer route-level pages
  pages/dashboard/     Dashboard route-level pages
  pages/orders/        Order route-level pages
  services/            Backend API clients and integration code
  styles/              Tokens, reset, utilities, and component CSS
```

Set `VITE_API_BASE_URL` in `.env.local` when your backend service URL is ready.
