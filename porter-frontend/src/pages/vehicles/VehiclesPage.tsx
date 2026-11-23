import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Sidebar } from "@/features/dashboard/components/Sidebar";
import { Topbar } from "@/features/dashboard/components/Topbar";
import { apiClient } from "@/services/apiClient";

type VehiclesPageProps = {
  onBackToDashboard: () => void;
  onCustomers?: () => void;
  onOrders?: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

type VehicleType = "LOWBED" | "FLATBED" | "CURTAIN_TRAILER" | "REFER_TRUCK";
type VehicleStatus = "AVAILABLE" | "ASSIGNED" | "MAINTENANCE" | "INACTIVE";

type Vehicle = {
  id: number;
  fleetNumber: string;
  plateNumber: string;
  vehicleType: VehicleType;
  driverName: string | null;
  status: VehicleStatus;
  createdAt: string;
  updatedAt: string;
  driverHistory: Array<{
    id: number;
    driverName: string;
    assignedAt: string;
    unassignedAt: string | null;
  }>;
};

type VehicleForm = {
  fleetNumber: string;
  plateNumber: string;
  vehicleType: VehicleType;
  driverName: string;
  status: VehicleStatus;
};

const vehicleTypeLabels: Record<VehicleType, string> = {
  LOWBED: "Lowbed",
  FLATBED: "Flatbed",
  CURTAIN_TRAILER: "Curtain Trailer",
  REFER_TRUCK: "Reefer Truck",
};

const statusLabels: Record<VehicleStatus, string> = {
  AVAILABLE: "Available",
  ASSIGNED: "Assigned",
  MAINTENANCE: "Maintenance",
  INACTIVE: "Inactive",
};

const initialForm: VehicleForm = {
  fleetNumber: "",
  plateNumber: "",
  vehicleType: "LOWBED",
  driverName: "",
  status: "AVAILABLE",
};

function statusTone(status: VehicleStatus) {
  if (status === "AVAILABLE") {
    return "available";
  }
  if (status === "ASSIGNED") {
    return "assigned";
  }
  if (status === "MAINTENANCE") {
    return "maintenance";
  }
  return "inactive";
}

function vehicleTone(vehicleType: VehicleType) {
  return vehicleType.toLowerCase().replace("_", "-");
}

export function VehiclesPage({
  onBackToDashboard,
  onCustomers,
  onOrders,
  displayName,
  role,
}: VehiclesPageProps) {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [form, setForm] = useState<VehicleForm>(initialForm);

  useEffect(() => {
    let ignore = false;

    async function loadVehicles() {
      try {
        const response = await apiClient.get<Vehicle[]>("/vehicles");
        if (!ignore) {
          setVehicles(response);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Could not load vehicles."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadVehicles();

    return () => {
      ignore = true;
    };
  }, []);

  const filteredVehicles = useMemo(() => {
    const query = search.trim().toLowerCase();
    return vehicles.filter((vehicle) => {
      const matchesSearch =
        !query ||
        [
          vehicle.fleetNumber,
          vehicle.plateNumber,
          vehicle.driverName ?? "",
          vehicleTypeLabels[vehicle.vehicleType],
          statusLabels[vehicle.status],
        ]
          .join(" ")
          .toLowerCase()
          .includes(query);
      const matchesType = !typeFilter || vehicle.vehicleType === typeFilter;
      const matchesStatus = !statusFilter || vehicle.status === statusFilter;

      return matchesSearch && matchesType && matchesStatus;
    });
  }, [search, statusFilter, typeFilter, vehicles]);

  function updateForm(field: keyof VehicleForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function createVehicle(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      const createdVehicle = await apiClient.post<Vehicle>("/vehicles", {
        ...form,
        driverName: form.driverName.trim(),
      });
      setVehicles((current) => [...current, createdVehicle]);
      setForm(initialForm);
      setShowCreateForm(false);
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "Could not create vehicle."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="dashboard-page">
      <Sidebar
        activeItem="Vehicles"
        onCustomers={onCustomers}
        onDashboard={onBackToDashboard}
        onOrders={onOrders}
        role={role}
      />
      <div className="dashboard-shell">
        <section className="vehicles-management">
          <div className="vehicles-management__topline">
            <nav className="breadcrumb" aria-label="Breadcrumb">
              <span onClick={onBackToDashboard}>Dashboard</span>
              <span className="material-symbols-outlined">chevron_right</span>
              <span className="active">Vehicles</span>
            </nav>
            <Topbar displayName={displayName} role={role} showSearch />
          </div>

          <header className="vehicles-management__header">
            <div>
              <h1>Vehicles</h1>
              <p>Manage vehicle inventory, driver assignment, and operational status.</p>
            </div>
            <button
              className="vehicles-button vehicles-button--primary"
              type="button"
              onClick={() => setShowCreateForm(true)}
            >
              <span className="material-symbols-outlined">add</span>
              Add Vehicle
            </button>
          </header>

          {showCreateForm ? (
            <form className="vehicles-create-panel" onSubmit={createVehicle}>
              <div className="vehicles-create-panel__header">
                <h2>Add New Vehicle</h2>
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateForm(false);
                    setForm(initialForm);
                  }}
                >
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>
              <div className="vehicles-form-grid">
                <label>
                  <span>Fleet Number</span>
                  <input
                    required
                    placeholder="LB-006"
                    value={form.fleetNumber}
                    onChange={(event) => updateForm("fleetNumber", event.target.value)}
                  />
                </label>
                <label>
                  <span>Plate Number</span>
                  <input
                    required
                    placeholder="KSA-LB-1006"
                    value={form.plateNumber}
                    onChange={(event) => updateForm("plateNumber", event.target.value)}
                  />
                </label>
                <label>
                  <span>Vehicle Type</span>
                  <select
                    value={form.vehicleType}
                    onChange={(event) => updateForm("vehicleType", event.target.value)}
                  >
                    {Object.entries(vehicleTypeLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>Driver</span>
                  <input
                    required
                    placeholder="Driver name"
                    value={form.driverName}
                    onChange={(event) => updateForm("driverName", event.target.value)}
                  />
                </label>
                <label>
                  <span>Status</span>
                  <select
                    value={form.status}
                    onChange={(event) => updateForm("status", event.target.value)}
                  >
                    {Object.entries(statusLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="vehicles-create-panel__actions">
                <button
                  className="vehicles-button"
                  type="button"
                  onClick={() => {
                    setShowCreateForm(false);
                    setForm(initialForm);
                  }}
                >
                  Cancel
                </button>
                <button
                  className="vehicles-button vehicles-button--primary"
                  disabled={saving}
                  type="submit"
                >
                  {saving ? "Saving..." : "Save Vehicle"}
                </button>
              </div>
            </form>
          ) : null}

          <section className="vehicles-table-panel">
            <div className="vehicles-table-toolbar">
              <label className="vehicles-search">
                <span className="material-symbols-outlined">search</span>
                <input
                  placeholder="Search by fleet, plate, driver, type, or status"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </label>
              <div className="vehicles-filter-row">
                <label>
                  <select
                    value={typeFilter}
                    onChange={(event) => setTypeFilter(event.target.value)}
                  >
                    <option value="">Vehicle Type: All</option>
                    {Object.entries(vehicleTypeLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                  <span className="material-symbols-outlined">expand_more</span>
                </label>
                <label>
                  <select
                    value={statusFilter}
                    onChange={(event) => setStatusFilter(event.target.value)}
                  >
                    <option value="">Status: All</option>
                    {Object.entries(statusLabels).map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                  <span className="material-symbols-outlined">expand_more</span>
                </label>
              </div>
            </div>

            {error ? (
              <p className="customer-form-message customer-form-message--error" role="alert">
                {error}
              </p>
            ) : null}

            <div className="vehicles-table-header" aria-hidden="true">
              <table className="vehicles-table">
                <colgroup>
                  <col className="vehicles-table-col vehicles-table-col--fleet" />
                  <col className="vehicles-table-col vehicles-table-col--plate" />
                  <col className="vehicles-table-col vehicles-table-col--type" />
                  <col className="vehicles-table-col vehicles-table-col--driver" />
                  <col className="vehicles-table-col vehicles-table-col--status" />
                </colgroup>
                <thead>
                  <tr>
                    <th>Fleet Number</th>
                    <th>Plate Number</th>
                    <th>Type</th>
                    <th>Driver</th>
                    <th>Status</th>
                  </tr>
                </thead>
              </table>
            </div>

            <div className="vehicles-table-wrap">
              <table className="vehicles-table">
                <colgroup>
                  <col className="vehicles-table-col vehicles-table-col--fleet" />
                  <col className="vehicles-table-col vehicles-table-col--plate" />
                  <col className="vehicles-table-col vehicles-table-col--type" />
                  <col className="vehicles-table-col vehicles-table-col--driver" />
                  <col className="vehicles-table-col vehicles-table-col--status" />
                </colgroup>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={5}>Loading vehicles...</td>
                    </tr>
                  ) : null}
                  {!loading && filteredVehicles.length === 0 ? (
                    <tr>
                      <td colSpan={5}>No vehicles found.</td>
                    </tr>
                  ) : null}
                  {!loading
                    ? filteredVehicles.map((vehicle) => (
                        <tr key={vehicle.id}>
                          <td>
                            <span className="vehicles-fleet-number">
                              {vehicle.fleetNumber}
                            </span>
                          </td>
                          <td>{vehicle.plateNumber}</td>
                          <td>
                            <span
                              className={`vehicles-type-pill vehicles-type-pill--${vehicleTone(
                                vehicle.vehicleType
                              )}`}
                            >
                              {vehicleTypeLabels[vehicle.vehicleType]}
                            </span>
                          </td>
                          <td>{vehicle.driverName || "Unassigned"}</td>
                          <td>
                            <span
                              className={`vehicles-status vehicles-status--${statusTone(
                                vehicle.status
                              )}`}
                            >
                              <span />
                              {statusLabels[vehicle.status]}
                            </span>
                          </td>
                        </tr>
                      ))
                    : null}
                </tbody>
              </table>
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
