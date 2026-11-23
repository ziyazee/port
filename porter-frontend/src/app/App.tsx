import { useEffect, useMemo, useState } from "react";
import { LoginPage } from "@/pages/auth/LoginPage";
import { DashboardPage } from "@/pages/dashboard/DashboardPage";
import { CreateOpsPage } from "@/pages/ops/CreateOpsPage";
import { CreateCustomerPage } from "@/pages/customers/CreateCustomerPage";
import { CreateOrderPage } from "@/pages/orders/CreateOrderPage";
import { VehiclesPage } from "@/pages/vehicles/VehiclesPage";
import { GoogleStorageTestPage } from "@/pages/google-storage/GoogleStorageTestPage";
import { TestMapPage } from "@/pages/test-map/TestMapPage";

type AppView =
  | "login"
  | "dashboard"
  | "customers"
  | "createCustomer"
  | "customerDetails"
  | "customerEdit"
  | "orders"
  | "createOrder"
  | "orderDetails"
  | "vehicles"
  | "createOps"
  | "googleTest"
  | "testMap";

type AppRoute = {
  view: AppView;
  customerId?: number;
  orderId?: number;
};
type LoginSession = {
  username: string;
  role: "ADMIN" | "OPERATIONS";
  displayName: string;
};

const AUTH_SESSION_KEY = "porter.auth.session";
const API_BASE_URL =
    import.meta.env.VITE_API_URL ??
    "https://springboot-api-783009266227.us-central1.run.app";
function readStoredSession(): LoginSession | null {
  const storedSession =
    window.localStorage.getItem(AUTH_SESSION_KEY) ??
    window.sessionStorage.getItem(AUTH_SESSION_KEY);

  if (!storedSession) {
    return null;
  }

  try {
    return JSON.parse(storedSession) as LoginSession;
  } catch {
    window.localStorage.removeItem(AUTH_SESSION_KEY);
    window.sessionStorage.removeItem(AUTH_SESSION_KEY);
    return null;
  }
}

function storeSession(session: LoginSession, remember: boolean) {
  const storage = remember ? window.localStorage : window.sessionStorage;
  const otherStorage = remember ? window.sessionStorage : window.localStorage;

  otherStorage.removeItem(AUTH_SESSION_KEY);
  storage.setItem(AUTH_SESSION_KEY, JSON.stringify(session));
}

function routeFromPath(pathname: string): AppRoute {
  const path = pathname.replace(/\/+$/, "") || "/";

  if (path === "/" || path === "/dashboard") {
    return { view: "dashboard" };
  }
  if (path === "/customers") {
    return { view: "customers" };
  }
  if (path === "/customers/new") {
    return { view: "createCustomer" };
  }
  const customerEditMatch = path.match(/^\/customers\/(\d+)\/edit$/);
  if (customerEditMatch) {
    return { view: "customerEdit", customerId: Number(customerEditMatch[1]) };
  }
  const customerMatch = path.match(/^\/customers\/(\d+)$/);
  if (customerMatch) {
    return { view: "customerDetails", customerId: Number(customerMatch[1]) };
  }
  if (path === "/orders") {
    return { view: "orders" };
  }
  if (path === "/orders/new") {
    return { view: "createOrder" };
  }
  const orderMatch = path.match(/^\/orders\/(\d+)$/);
  if (orderMatch) {
    return { view: "orderDetails", orderId: Number(orderMatch[1]) };
  }
  if (path === "/vehicles") {
    return { view: "vehicles" };
  }
  if (path === "/ops/new") {
    return { view: "createOps" };
  }
  if (path === "/google-storage-test") {
    return { view: "googleTest" };
  }
  if (path === "/test-map") {
    return { view: "testMap" };
  }
  return { view: "dashboard" };
}

export function App() {
  const [session, setSession] = useState<LoginSession | null>(() =>
    readStoredSession()
  );
  const [route, setRoute] = useState<AppRoute>(() => {
    const path = window.location.pathname;
    return readStoredSession() ? routeFromPath(path) : { view: "login" };
  });
  const view = route.view;

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/health`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Backend health check failed: ${response.status}`);
        }
        return response.json();
      })
      .then((health) => {
        console.log("Backend connected:", health);
      })
      .catch((error) => {
        console.error("Backend connection failed:", error);
      });
  }, []);

  useEffect(() => {
    function handlePopState() {
      setRoute(session ? routeFromPath(window.location.pathname) : { view: "login" });
    }

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [session]);

  const navigate = useMemo(
    () => (path: string, replace = false) => {
      if (window.location.pathname !== path) {
        if (replace) {
          window.history.replaceState(null, "", path);
        } else {
          window.history.pushState(null, "", path);
        }
      }
      setRoute(session ? routeFromPath(path) : { view: "login" });
    },
    [session]
  );

  useEffect(() => {
    if (!session) {
      return;
    }
    if (window.location.pathname === "/") {
      navigate("/dashboard", true);
    }
  }, [navigate, session]);

  if (view === "dashboard" && session) {
    return (
      <DashboardPage
        onCustomers={() => navigate("/customers")}
        onCreateCustomer={() => navigate("/customers/new")}
        onCreateOrder={() => navigate("/orders/new")}
        onOrders={() => navigate("/orders")}
        onVehicles={() => navigate("/vehicles")}
        onGoogleTest={() => navigate("/google-storage-test")}
        onTestMap={() => navigate("/test-map")}
        displayName={session.displayName}
        role={session.role}
      />
    );
  }

  if (
    (
      view === "customers" ||
      view === "createCustomer" ||
      view === "customerDetails" ||
      view === "customerEdit"
    ) &&
    session
  ) {
    return (
      <CreateCustomerPage
        createdBy={session.username}
        initialCustomerId={route.customerId}
        initialCreateFlow={view === "createCustomer"}
        initialEditMode={view === "customerEdit"}
        onBackToDashboard={() => navigate("/dashboard")}
        onCreateCustomer={() => navigate("/customers/new")}
        onCustomerDetails={(customerId) => navigate(`/customers/${customerId}`)}
        onCustomerEdit={(customerId) => navigate(`/customers/${customerId}/edit`)}
        onCustomerList={() => navigate("/customers")}
        onOrders={() => navigate("/orders")}
        onVehicles={() => navigate("/vehicles")}
        displayName={session.displayName}
        role={session.role}
      />
    );
  }

  if ((view === "orders" || view === "createOrder" || view === "orderDetails") && session) {
    return (
      <CreateOrderPage
        initialOrderId={route.orderId}
        initialCreateFlow={view === "createOrder"}
        onCreateOrder={() => navigate("/orders/new")}
        onCustomers={() => navigate("/customers")}
        onBackToDashboard={() => navigate("/dashboard")}
        onOrderDetails={(orderId) => navigate(`/orders/${orderId}`)}
        onOrderList={() => navigate("/orders")}
        onTracking={() => navigate("/test-map")}
        onVehicles={() => navigate("/vehicles")}
        displayName={session.displayName}
        role={session.role}
      />
    );
  }

  if (view === "vehicles" && session) {
    return (
      <VehiclesPage
        onBackToDashboard={() => navigate("/dashboard")}
        onCustomers={() => navigate("/customers")}
        onOrders={() => navigate("/orders")}
        displayName={session.displayName}
        role={session.role}
      />
    );
  }

  if (view === "createOps" && session?.role === "ADMIN") {
    return (
      <CreateOpsPage
        displayName={session.displayName}
        onBackToDashboard={() => navigate("/dashboard")}
      />
    );
  }

  if (view === "googleTest" && session) {
    return (
      <GoogleStorageTestPage
        onBackToDashboard={() => navigate("/dashboard")}
        displayName={session.displayName}
        role={session.role}
      />
    );
  }

  if (view === "testMap" && session) {
    return (
      <TestMapPage
        onBackToDashboard={() => navigate("/dashboard")}
        displayName={session.displayName}
        role={session.role}
      />
    );
  }

  return (
    <LoginPage
      onLogin={(nextSession, remember) => {
        storeSession(nextSession, remember);
        setSession(nextSession);
        window.history.replaceState(null, "", "/dashboard");
        setRoute({ view: "dashboard" });
      }}
    />
  );
}
