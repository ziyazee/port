import { OpsAccountForm } from "@/features/ops/components/OpsAccountForm";
import { Sidebar } from "@/features/dashboard/components/Sidebar";
import { Topbar } from "@/features/dashboard/components/Topbar";

type CreateOpsPageProps = {
  onBackToDashboard: () => void;
  displayName: string;
};

export function CreateOpsPage({ onBackToDashboard, displayName }: CreateOpsPageProps) {
  return (
    <main className="dashboard-page">
      <Sidebar
        onDashboard={onBackToDashboard}
        role="ADMIN"
      />
      <div className="dashboard-shell">
        <Topbar
          breadcrumb={["Operations", "Create Ops User"]}
          displayName={displayName}
          role="ADMIN"
          showSearch={false}
        />
        <section className="ops-content">
          <header className="customer-page-header">
            <h1>Create Ops User</h1>
            <p>Add an operations account with dashboard access.</p>
          </header>
          <OpsAccountForm onCancel={onBackToDashboard} />
        </section>
      </div>
    </main>
  );
}
