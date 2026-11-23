import { DashboardOverview } from "@/features/dashboard/components/DashboardOverview";
import { RecentTables } from "@/features/dashboard/components/RecentTables";
import { Sidebar } from "@/features/dashboard/components/Sidebar";

type DashboardPageProps = {
  onCustomers: () => void;
  onCreateCustomer: () => void;
  onCreateOrder: () => void;
  onOrders: () => void;
  onVehicles: () => void;
  onGoogleTest: () => void;
  onTestMap: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

export function DashboardPage({
  onCustomers,
  onCreateCustomer,
  onCreateOrder,
  onOrders,
  onVehicles,
  onGoogleTest,
  onTestMap,
  displayName,
  role,
}: DashboardPageProps) {
  return (
    <main className="dashboard-page">
      <Sidebar
        onCustomers={onCustomers}
        onGoogleTest={onGoogleTest}
        onOrders={onOrders}
        onTestMap={onTestMap}
        onVehicles={onVehicles}
        role={role}
      />
      <div className="dashboard-shell">
        <section className="dashboard-content">
          <DashboardOverview
            displayName={displayName}
            onCreateCustomer={onCreateCustomer}
            onCreateOrder={onCreateOrder}
            role={role}
          />
          <RecentTables />
        </section>
      </div>
    </main>
  );
}
