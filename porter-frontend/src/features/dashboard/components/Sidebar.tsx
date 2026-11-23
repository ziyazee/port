type SidebarProps = {
  activeItem?:
    | "Dashboard"
    | "Customers"
    | "Orders"
    | "Vehicles"
    | "Tracking Links"
    | "Reports"
    | "Google Test"
    | "Test Map";
  onDashboard?: () => void;
  onCustomers?: () => void;
  onOrders?: () => void;
  onVehicles?: () => void;
  onGoogleTest?: () => void;
  onTestMap?: () => void;
  role?: "ADMIN" | "OPERATIONS";
};

const primaryItems = [
  { label: "Dashboard", icon: "space_dashboard", path: "/dashboard" },
  { label: "Customers", icon: "people", path: "/customers" },
  { label: "Orders", icon: "receipt_long", path: "/orders" },
  { label: "Vehicles", icon: "local_shipping", path: "/vehicles" },
  { label: "Tracking Links", icon: "link", path: "/tracking-links" },
  { label: "Test Map", icon: "map", path: "/test-map" },
  { label: "Reports", icon: "bar_chart", path: "/reports" },
  { label: "Google Test", icon: "science", path: "/google-storage-test" },
];

const secondaryItems = [
  { label: "Settings", icon: "settings" },
  { label: "Support", icon: "help" },
];

export function Sidebar({
  activeItem = "Dashboard",
  onDashboard,
  onCustomers,
  onOrders,
  onVehicles,
  onGoogleTest,
  onTestMap,
  role: _role = "OPERATIONS",
}: SidebarProps) {
  return (
    <aside className="dashboard-sidebar">
      <div className="dashboard-brand">
        <div className="dashboard-brand__mark">
          <span className="material-symbols-outlined">bolt</span>
        </div>
        <div>
          <strong>CreativeOps</strong>
          <span>Command Center</span>
        </div>
      </div>

      <nav className="dashboard-nav" aria-label="Dashboard navigation">
        {primaryItems.map((item) => (
          <a
            className={`dashboard-nav__item${
              item.label === activeItem ? " dashboard-nav__item--active" : ""
            }`}
            href={item.path}
            key={item.label}
            onClick={(event) => {
              event.preventDefault();
              if (item.label === "Dashboard") {
                onDashboard?.();
              }
              if (item.label === "Customers") {
                onCustomers?.();
              }
              if (item.label === "Orders") {
                onOrders?.();
              }
              if (item.label === "Vehicles") {
                onVehicles?.();
              }
              if (item.label === "Google Test") {
                onGoogleTest?.();
              }
              if (item.label === "Test Map") {
                onTestMap?.();
              }
            }}
          >
            <span className="material-symbols-outlined">{item.icon}</span>
            {item.label}
          </a>
        ))}
      </nav>

      <nav className="dashboard-nav dashboard-nav--footer" aria-label="Help">
        {secondaryItems.map((item) => (
          <a className="dashboard-nav__item" href="/" key={item.label}>
            <span className="material-symbols-outlined">{item.icon}</span>
            {item.label}
          </a>
        ))}
      </nav>
    </aside>
  );
}
