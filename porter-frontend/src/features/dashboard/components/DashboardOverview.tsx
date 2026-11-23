import { Topbar } from "@/features/dashboard/components/Topbar";

const actions = [
  {
    id: "customer",
    title: "Create Customer Account",
    description: "Onboard a new enterprise or individual client.",
    icon: "person_add",
  },
  {
    id: "order",
    title: "Create Order",
    description: "Initiate a new delivery or dispatch request.",
    icon: "add_box",
  },
];

const dailyOrderStatuses = [
  {
    label: "Today's Orders",
    value: "1,248",
    icon: "inventory_2",
    tone: "orders",
  },
  {
    label: "Active Deliveries",
    value: "156",
    icon: "local_shipping",
    tone: "active",
  },
  {
    label: "Completed",
    value: "892",
    icon: "check_circle",
    tone: "completed",
  },
];

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good Morning";
  if (hour < 17) return "Good Afternoon";
  return "Good Evening";
}

function getFormattedDate() {
  return new Date().toLocaleDateString("en-US", {
    weekday: "long",
    month: "short",
    day: "numeric",
  });
}

type DashboardOverviewProps = {
  onCreateCustomer: () => void;
  onCreateOrder: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

export function DashboardOverview({
  onCreateCustomer,
  onCreateOrder,
  displayName,
  role,
}: DashboardOverviewProps) {
  return (
    <>
      <header className="dashboard-header">
        <section className="dashboard-welcome">
          <div>
            <div className="dashboard-welcome__title">
              <h1 className="dashboard-welcome__greeting">
                {getGreeting()},
              </h1>
              <span className="dashboard-welcome__team">{displayName}</span>
            </div>
            <span className="dashboard-welcome__date">{getFormattedDate()}</span>
            <div className="quick-shortcuts">
              {dailyOrderStatuses.map((status) => (
                <div
                  className={`quick-shortcut quick-shortcut--status quick-shortcut--${status.tone}`}
                  key={status.label}
                >
                  <span className="material-symbols-outlined">{status.icon}</span>
                  <span>{status.label}</span>
                  <strong>{status.value}</strong>
                </div>
              ))}
            </div>
          </div>
        </section>
        <Topbar displayName={displayName} role={role} />
      </header>

      <section className="quick-actions" aria-label="Quick actions">
        {actions.map((action) => (
          <button
            className="action-card"
            type="button"
            key={action.title}
            onClick={() => {
              if (action.id === "customer") {
                onCreateCustomer();
                return;
              }
              onCreateOrder();
            }}
          >
            <div className="action-card__header">
              <span className="action-card__icon material-symbols-outlined">
                {action.icon}
              </span>
              <span className="material-symbols-outlined action-card__arrow">
                arrow_forward
              </span>
            </div>
            <span className="action-card__copy">
              <strong>{action.title}</strong>
              <span>{action.description}</span>
            </span>
          </button>
        ))}
      </section>
    </>
  );
}
