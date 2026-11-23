import { useEffect, useMemo, useState } from "react";
import { OrderForm } from "@/features/orders/components/OrderForm";
import { Sidebar } from "@/features/dashboard/components/Sidebar";
import { Topbar } from "@/features/dashboard/components/Topbar";
import { apiClient } from "@/services/apiClient";

type CreateOrderPageProps = {
  initialOrderId?: number;
  initialCreateFlow?: boolean;
  onBackToDashboard: () => void;
  onCreateOrder?: () => void;
  onCustomers?: () => void;
  onOrderDetails?: (orderId: number) => void;
  onOrderList?: () => void;
  onTracking?: () => void;
  onVehicles?: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

type VehicleType = "LOWBED" | "FLATBED" | "CURTAIN_TRAILER" | "REFER_TRUCK";
type OrderStatus =
  | "CREATED"
  | "VERIFIED"
  | "VEHICLE_ASSIGNED"
  | "TRACKING_STARTED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";
type SortKey = "newest" | "oldest" | "company_asc";

type OrderResponse = {
  id: number;
  customerAccountId: number;
  companyName: string;
  fromLocation: string;
  toLocation: string;
  vehicleType: VehicleType;
  loadDescription: string;
  cargoSpecification?: "STANDARD" | "OVERSIZED";
  cargoHeight?: string | null;
  cargoWeight?: string | null;
  cargoDimensions?: string | null;
  quantityUnits: number;
  assignedVehicles: number;
  orderStatus: OrderStatus;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
};

type OrderVehicleStatus =
  | "ASSIGNED"
  | "EN_ROUTE_TO_PICKUP"
  | "ARRIVED_AT_PICKUP"
  | "LEFT_PICKUP"
  | "IN_TRANSIT"
  | "ARRIVED_AT_DROP"
  | "UNLOADING"
  | "DELIVERED"
  | "CANCELLED";

type OrderVehicleResponse = {
  id: number;
  orderId: number;
  vehicleId: number;
  fleetNumber: string;
  plateNumber: string;
  vehicleType: VehicleType;
  driverId: number | null;
  driverName: string | null;
  status: OrderVehicleStatus;
  assignedAt: string;
  deliveredAt: string | null;
  createdAt: string;
  updatedAt: string;
};

const vehicleTypeLabels: Record<VehicleType, string> = {
  LOWBED: "Lowbed",
  FLATBED: "Flatbed",
  CURTAIN_TRAILER: "Curtain Trailer",
  REFER_TRUCK: "Reefer Truck",
};

function orderReference(id: number) {
  return `PT-ORD-${String(id).padStart(4, "0")}`;
}

function statusLabel(status: OrderStatus) {
  const labels: Record<OrderStatus, string> = {
    CREATED: "Created",
    VERIFIED: "Verified",
    VEHICLE_ASSIGNED: "Vehicle Assigned",
    TRACKING_STARTED: "Tracking Started",
    IN_PROGRESS: "In Progress",
    COMPLETED: "Completed",
    CANCELLED: "Cancelled",
  };
  return labels[status];
}

function orderVehicleStatusLabel(status: OrderVehicleStatus) {
  const labels: Record<OrderVehicleStatus, string> = {
    ASSIGNED: "Assigned",
    EN_ROUTE_TO_PICKUP: "En Route To Pickup",
    ARRIVED_AT_PICKUP: "Arrived At Pickup",
    LEFT_PICKUP: "Left Pickup",
    IN_TRANSIT: "In Transit",
    ARRIVED_AT_DROP: "Arrived At Drop",
    UNLOADING: "Unloading",
    DELIVERED: "Delivered",
    CANCELLED: "Cancelled",
  };
  return labels[status];
}

function statusTone(status: OrderStatus) {
  return status.toLowerCase().replace("_", "-");
}

function vehicleTone(vehicleType: VehicleType) {
  return vehicleType.toLowerCase().replace("_", "-");
}

function orderVehicleStatusTone(status: OrderVehicleStatus) {
  if (status === "DELIVERED") {
    return "active";
  }
  if (status === "CANCELLED") {
    return "inactive";
  }
  return "pending";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

function optionalValue(value?: string | null) {
  return value && value.trim() ? value : "-";
}

export function CreateOrderPage({
  initialOrderId,
  initialCreateFlow = false,
  onBackToDashboard,
  onCreateOrder,
  onCustomers,
  onOrderDetails,
  onOrderList,
  onTracking,
  onVehicles,
  displayName,
  role,
}: CreateOrderPageProps) {
  const [showCreateForm, setShowCreateForm] = useState(initialCreateFlow);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<OrderResponse | null>(null);
  const [assignedVehicles, setAssignedVehicles] = useState<OrderVehicleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingOrderDetails, setLoadingOrderDetails] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [vehicleFilter, setVehicleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [sortBy, setSortBy] = useState<SortKey>("newest");

  useEffect(() => {
    setShowCreateForm(initialCreateFlow);
  }, [initialCreateFlow]);

  useEffect(() => {
    if (initialCreateFlow) {
      setSelectedOrder(null);
      setAssignedVehicles([]);
    }
  }, [initialCreateFlow]);

  useEffect(() => {
    let ignore = false;

    async function loadOrders() {
      try {
        const response = await apiClient.get<OrderResponse[]>("/orders");
        if (!ignore) {
          setOrders(response);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error ? exception.message : "Could not load orders."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadOrders();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!initialOrderId) {
      if (!initialCreateFlow) {
        setSelectedOrder(null);
        setAssignedVehicles([]);
      }
      return;
    }

    let ignore = false;
    setShowCreateForm(false);
    setLoadingOrderDetails(true);
    setError("");

    async function loadOrderDetails() {
      try {
        const [orderResponse, vehicleResponse] = await Promise.all([
          apiClient.get<OrderResponse>(`/orders/${initialOrderId}`),
          apiClient.get<OrderVehicleResponse[]>(`/orders/${initialOrderId}/vehicles`),
        ]);
        if (!ignore) {
          setSelectedOrder(orderResponse);
          setAssignedVehicles(vehicleResponse);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Could not load order details."
          );
          setSelectedOrder(null);
          setAssignedVehicles([]);
        }
      } finally {
        if (!ignore) {
          setLoadingOrderDetails(false);
        }
      }
    }

    loadOrderDetails();

    return () => {
      ignore = true;
    };
  }, [initialCreateFlow, initialOrderId]);

  const filteredOrders = useMemo(() => {
    const query = search.trim().toLowerCase();
    const nextOrders = orders.filter((order) => {
      const matchesSearch =
        !query ||
        [
          orderReference(order.id),
          order.companyName,
          order.fromLocation,
          order.toLocation,
          order.loadDescription,
        ]
          .join(" ")
          .toLowerCase()
          .includes(query);
      const matchesVehicle = !vehicleFilter || order.vehicleType === vehicleFilter;
      const matchesStatus = !statusFilter || order.orderStatus === statusFilter;

      return matchesSearch && matchesVehicle && matchesStatus;
    });

    return nextOrders.sort((first, second) => {
      if (sortBy === "company_asc") {
        return first.companyName.localeCompare(second.companyName);
      }
      const firstTime = new Date(first.createdAt).getTime();
      const secondTime = new Date(second.createdAt).getTime();
      return sortBy === "oldest" ? firstTime - secondTime : secondTime - firstTime;
    });
  }, [orders, search, sortBy, statusFilter, vehicleFilter]);

  function openOrderList() {
    setShowCreateForm(false);
    setSelectedOrder(null);
    setAssignedVehicles([]);
    onOrderList?.();
  }

  function openCreateOrder() {
    setShowCreateForm(true);
    setSelectedOrder(null);
    setAssignedVehicles([]);
    onCreateOrder?.();
  }

  function openOrderDetails(order: OrderResponse) {
    setSelectedOrder(order);
    setAssignedVehicles([]);
    setShowCreateForm(false);
    onOrderDetails?.(order.id);
  }

  if (showCreateForm) {
    return (
      <main className="dashboard-page">
        <Sidebar
          activeItem="Orders"
          onCustomers={onCustomers}
          onDashboard={onBackToDashboard}
          onOrders={openOrderList}
          onVehicles={onVehicles}
          role={role}
        />
        <div className="dashboard-shell">
          <section className="order-content">
            <div className="order-content__topline">
              <nav className="breadcrumb" aria-label="Breadcrumb">
                <span onClick={openOrderList}>Orders</span>
                <span className="material-symbols-outlined">chevron_right</span>
                <span className="active">Create Order</span>
              </nav>
              <Topbar displayName={displayName} role={role} showSearch={false} />
            </div>
            <header className="order-page-header">
              <div>
                <h1>Create New Order</h1>
                <p>Register a new transport request into the Porter network.</p>
              </div>
              <button
                className="order-management-button"
                type="button"
                onClick={openOrderList}
              >
                <span className="material-symbols-outlined">arrow_back</span>
                Back
              </button>
            </header>
            <OrderForm
              onCancel={openOrderList}
              onCreated={(orderId) => onOrderDetails?.(orderId)}
            />
          </section>
        </div>
      </main>
    );
  }

  if (initialOrderId || selectedOrder) {
    const order = selectedOrder;

    return (
      <main className="dashboard-page">
        <Sidebar
          activeItem="Orders"
          onCustomers={onCustomers}
          onDashboard={onBackToDashboard}
          onOrders={openOrderList}
          onVehicles={onVehicles}
          role={role}
        />
        <div className="dashboard-shell">
          <section className="order-detail-page">
            <div className="order-detail-topline">
              <Topbar
                breadcrumb={["Orders", order ? orderReference(order.id) : "Order Details"]}
                displayName={displayName}
                role={role}
                showSearch={false}
              />
            </div>

            {error ? (
              <p className="customer-form-message customer-form-message--error" role="alert">
                {error}
              </p>
            ) : null}

            {loadingOrderDetails && !order ? (
              <section className="order-detail-panel">
                <p className="order-detail-empty">Loading order details...</p>
              </section>
            ) : null}

            {order ? (
              <>
                <header className="order-detail-header">
                  <div>
                    <button
                      className="order-detail-back"
                      type="button"
                      onClick={openOrderList}
                    >
                      <span className="material-symbols-outlined">arrow_back</span>
                      Orders
                    </button>
                    <div className="order-detail-title">
                      <h1>{orderReference(order.id)}</h1>
                      <span
                        className={`order-management-status order-management-status--${statusTone(
                          order.orderStatus
                        )}`}
                      >
                        <span />
                        {statusLabel(order.orderStatus)}
                      </span>
                    </div>
                    <p>
                      {order.companyName}
                      <span aria-hidden="true">·</span>
                      Created {formatDate(order.createdAt)}
                    </p>
                  </div>
                </header>

                <section className="order-detail-layout">
                  <div className="order-detail-main">
                    <section className="order-detail-panel order-detail-panel--route">
                      <header className="order-detail-section-header">
                        <span className="material-symbols-outlined">route</span>
                        <div>
                          <h2>Route</h2>
                          <p>Pickup and delivery locations</p>
                        </div>
                      </header>
                      <div className="order-route-detail">
                        <div className="order-route-detail__item">
                          <span className="material-symbols-outlined order-route-detail__icon order-route-detail__icon--pickup">
                            trip_origin
                          </span>
                          <div>
                            <small>Pickup</small>
                            <strong>{order.fromLocation}</strong>
                          </div>
                        </div>
                        <div className="order-route-detail__line" aria-hidden="true">
                          <span className="material-symbols-outlined">arrow_forward</span>
                        </div>
                        <div className="order-route-detail__item">
                          <span className="material-symbols-outlined order-route-detail__icon order-route-detail__icon--drop">
                            location_on
                          </span>
                          <div>
                            <small>Destination</small>
                            <strong>{order.toLocation}</strong>
                          </div>
                        </div>
                      </div>
                    </section>

                    <section className="order-detail-panel order-detail-panel--load">
                      <header className="order-detail-section-header">
                        <span className="material-symbols-outlined">inventory_2</span>
                        <div>
                          <h2>Load Details</h2>
                          <p>Cargo and vehicle requirements</p>
                        </div>
                      </header>
                      <dl className="order-detail-fields">
                        <div className="order-detail-fields__wide">
                          <dt>Commodity</dt>
                          <dd>{order.loadDescription}</dd>
                        </div>
                        <div>
                          <dt>Quantity</dt>
                          <dd>{order.quantityUnits} {order.quantityUnits === 1 ? "unit" : "units"}</dd>
                        </div>
                        <div>
                          <dt>Cargo Type</dt>
                          <dd>{order.cargoSpecification?.replaceAll("_", " ") ?? "-"}</dd>
                        </div>
                        <div>
                          <dt>Vehicle Type</dt>
                          <dd>{vehicleTypeLabels[order.vehicleType]}</dd>
                        </div>
                        <div>
                          <dt>Weight</dt>
                          <dd>{optionalValue(order.cargoWeight)}</dd>
                        </div>
                        <div>
                          <dt>Dimensions</dt>
                          <dd>{optionalValue(order.cargoDimensions)}</dd>
                        </div>
                        <div>
                          <dt>Height</dt>
                          <dd>{optionalValue(order.cargoHeight)}</dd>
                        </div>
                      </dl>
                    </section>

                    <section className="order-detail-panel order-detail-panel--vehicles">
                      <header className="order-detail-section-header order-detail-section-header--split">
                        <div className="order-detail-section-title">
                          <span className="material-symbols-outlined">local_shipping</span>
                          <div>
                            <h2>Assigned Vehicles</h2>
                            <p>Fleet and driver allocation</p>
                          </div>
                        </div>
                        <span className="order-detail-count">{assignedVehicles.length}</span>
                      </header>
                      <div className="order-assignment-card-list">
                        {!loadingOrderDetails && assignedVehicles.length === 0 ? (
                          <p className="order-detail-empty">No vehicles assigned to this order.</p>
                        ) : null}
                        {assignedVehicles.map((assignment) => (
                          <article className="order-assignment-card" key={assignment.id}>
                            <div className="order-assignment-card__vehicle">
                              <span className="material-symbols-outlined">
                                local_shipping
                              </span>
                              <div>
                                <strong>{assignment.plateNumber}</strong>
                                <small>{assignment.fleetNumber}</small>
                              </div>
                            </div>
                            <dl>
                              <div>
                                <dt>Type</dt>
                                <dd>{vehicleTypeLabels[assignment.vehicleType]}</dd>
                              </div>
                              <div>
                                <dt>Driver</dt>
                                <dd>{assignment.driverName ?? "Not assigned"}</dd>
                              </div>
                              <div>
                                <dt>Assigned</dt>
                                <dd>{formatDate(assignment.assignedAt)}</dd>
                              </div>
                            </dl>
                            <span
                              className={`order-management-status order-management-status--${orderVehicleStatusTone(
                                assignment.status
                              )}`}
                            >
                              <span />
                              {orderVehicleStatusLabel(assignment.status)}
                            </span>
                            <button
                              className="order-vehicle-tracking-link"
                              type="button"
                              onClick={onTracking}
                            >
                              <span className="material-symbols-outlined">near_me</span>
                              Open live map
                            </button>
                          </article>
                        ))}
                      </div>
                    </section>
                  </div>

                  <aside className="order-detail-panel order-detail-panel--overview">
                    <header className="order-detail-section-header">
                      <span className="material-symbols-outlined">description</span>
                      <div>
                        <h2>Order Overview</h2>
                        <p>Operational summary</p>
                      </div>
                    </header>
                    <dl className="order-detail-overview">
                      <div>
                        <dt>Customer</dt>
                        <dd>{order.companyName}</dd>
                      </div>
                      <div>
                        <dt>Order Status</dt>
                        <dd>{statusLabel(order.orderStatus)}</dd>
                      </div>
                      <div>
                        <dt>Assigned Vehicles</dt>
                        <dd>{assignedVehicles.length}</dd>
                      </div>
                      <div>
                        <dt>Required Type</dt>
                        <dd>{vehicleTypeLabels[order.vehicleType]}</dd>
                      </div>
                      <div>
                        <dt>Created By</dt>
                        <dd>{order.createdBy}</dd>
                      </div>
                      <div>
                        <dt>Last Updated</dt>
                        <dd>{formatDate(order.updatedAt)}</dd>
                      </div>
                    </dl>
                  </aside>
                </section>
              </>
            ) : null}
          </section>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-page">
      <Sidebar
        activeItem="Orders"
        onCustomers={onCustomers}
        onDashboard={onBackToDashboard}
        onOrders={openOrderList}
        onVehicles={onVehicles}
        role={role}
      />
      <div className="dashboard-shell">
        <section className="order-management">
          <div className="order-management__topline">
            <nav className="breadcrumb" aria-label="Breadcrumb">
              <span onClick={onBackToDashboard}>Dashboard</span>
              <span className="material-symbols-outlined">chevron_right</span>
              <span className="active">Orders</span>
            </nav>
            <Topbar displayName={displayName} role={role} showSearch />
          </div>

          <header className="order-management__header">
            <div>
              <h1>Orders</h1>
              <p>Track transport requests, vehicle matching, and route assignments.</p>
            </div>
            <div className="order-management__actions">
              <button className="order-management-button" type="button">
                <span className="material-symbols-outlined">download</span>
                Export Orders
              </button>
              <button
                className="order-management-button order-management-button--primary"
                type="button"
                onClick={openCreateOrder}
              >
                <span className="material-symbols-outlined">add</span>
                Create Order
              </button>
            </div>
          </header>

          <section className="order-table-panel">
            <div className="order-table-toolbar">
              <label className="order-management-search">
                <span className="material-symbols-outlined">search</span>
                <input
                  placeholder="Search by order ID, company, route, or load"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </label>

              <div className="order-filter-row">
                <label>
                  <select
                    value={vehicleFilter}
                    onChange={(event) => setVehicleFilter(event.target.value)}
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
                    <option value="CREATED">Created</option>
                    <option value="VERIFIED">Verified</option>
                    <option value="VEHICLE_ASSIGNED">Vehicle Assigned</option>
                    <option value="TRACKING_STARTED">Tracking Started</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                  <span className="material-symbols-outlined">expand_more</span>
                </label>
                <label>
                  <select
                    value={sortBy}
                    onChange={(event) => setSortBy(event.target.value as SortKey)}
                  >
                    <option value="newest">Sort By: Newest</option>
                    <option value="oldest">Oldest</option>
                    <option value="company_asc">Company A-Z</option>
                  </select>
                  <span className="material-symbols-outlined">sort</span>
                </label>
              </div>
            </div>

            {error ? (
              <p className="customer-form-message customer-form-message--error" role="alert">
                {error}
              </p>
            ) : null}

            <div className="order-management-table-header" aria-hidden="true">
              <table className="order-management-table">
                <colgroup>
                  <col className="order-table-col order-table-col--reference" />
                  <col className="order-table-col order-table-col--customer" />
                  <col className="order-table-col order-table-col--route" />
                  <col className="order-table-col order-table-col--vehicle" />
                  <col className="order-table-col order-table-col--load" />
                  <col className="order-table-col order-table-col--status" />
                </colgroup>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Customer</th>
                    <th>Route</th>
                    <th>Vehicle</th>
                    <th>Load</th>
                    <th>Status</th>
                  </tr>
                </thead>
              </table>
            </div>

            <div className="order-management-table-wrap">
              <table className="order-management-table">
                <colgroup>
                  <col className="order-table-col order-table-col--reference" />
                  <col className="order-table-col order-table-col--customer" />
                  <col className="order-table-col order-table-col--route" />
                  <col className="order-table-col order-table-col--vehicle" />
                  <col className="order-table-col order-table-col--load" />
                  <col className="order-table-col order-table-col--status" />
                </colgroup>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={6}>Loading orders...</td>
                    </tr>
                  ) : null}
                  {!loading && filteredOrders.length === 0 ? (
                    <tr>
                      <td colSpan={6}>No orders found.</td>
                    </tr>
                  ) : null}
                  {!loading
                    ? filteredOrders.map((order) => (
                        <tr key={order.id} onClick={() => openOrderDetails(order)}>
                          <td>
                            <span className="order-reference">
                              {orderReference(order.id)}
                            </span>
                          </td>
                          <td>
                            <strong>{order.companyName}</strong>
                          </td>
                          <td>
                            <div className="order-route-cell">
                              <span>{order.fromLocation}</span>
                              <span className="material-symbols-outlined">arrow_forward</span>
                              <span>{order.toLocation}</span>
                            </div>
                          </td>
                          <td>
                            <span
                              className={`order-type-pill order-type-pill--${vehicleTone(
                                order.vehicleType
                              )}`}
                            >
                              {vehicleTypeLabels[order.vehicleType]}
                            </span>
                          </td>
                          <td>
                            <div className="order-load-cell">
                              <span>{order.loadDescription}</span>
                              <small>{order.quantityUnits} unit(s)</small>
                            </div>
                          </td>
                          <td>
                            <span
                              className={`order-management-status order-management-status--${statusTone(
                                order.orderStatus
                              )}`}
                            >
                              <span />
                              {statusLabel(order.orderStatus)}
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
