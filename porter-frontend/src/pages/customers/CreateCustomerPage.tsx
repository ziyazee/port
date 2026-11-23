import { useEffect, useMemo, useState } from "react";
import { CustomerForm } from "@/features/customers/components/CustomerForm";
import { Sidebar } from "@/features/dashboard/components/Sidebar";
import { Topbar } from "@/features/dashboard/components/Topbar";
import { apiClient } from "@/services/apiClient";

type CreateCustomerPageProps = {
  createdBy: string;
  initialCustomerId?: number;
  initialCreateFlow?: boolean;
  initialEditMode?: boolean;
  onCreateCustomer?: () => void;
  onBackToDashboard: () => void;
  onCustomerDetails?: (customerId: number) => void;
  onCustomerEdit?: (customerId: number) => void;
  onCustomerList?: () => void;
  onOrders?: () => void;
  onVehicles?: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

type AccountStatus =
  | "CREATED"
  | "PENDING_FOR_VERIFICATION"
  | "VERIFIED"
  | "DEACTIVATED";

type CustomerAccount = {
  id: number;
  companyName: string;
  companyRegistrationNumber: string;
  vatNumber: string;
  nationalAddress: string;
  pocContactNumber: string;
  pocEmailAddress: string;
  commsEmailList: string[];
  registrationDocument: DocumentSummary | null;
  vatDocument: DocumentSummary | null;
  nationalAddressDocument: DocumentSummary | null;
  accountStatus: AccountStatus;
  createdAt: string;
  updatedAt: string;
};

type DocumentSummary = {
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  storageKey: string;
};

type CustomerEditForm = {
  companyName: string;
  companyRegistrationNumber: string;
  vatNumber: string;
  nationalAddress: string;
  pocContactNumber: string;
  pocEmailAddress: string;
  commsEmailList: string;
  accountStatus: AccountStatus;
};

type CustomerEditDocuments = {
  registrationDocument: File | null;
  vatDocument: File | null;
  nationalAddressDocument: File | null;
};

type OrderStatus =
  | "CREATED"
  | "VERIFIED"
  | "VEHICLE_ASSIGNED"
  | "TRACKING_STARTED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

type OrderResponse = {
  id: number;
  customerAccountId: number;
  companyName: string;
  fromLocation: string;
  toLocation: string;
  vehicleType: string;
  loadDescription: string;
  assignedVehicles: number;
  orderStatus: OrderStatus;
  createdAt: string;
};

type CustomerType = "Construction" | "Retail" | "Manufacturing";
type SortKey = "newest" | "oldest" | "name_asc";

const customerTypes: CustomerType[] = ["Construction", "Retail", "Manufacturing"];
const CUSTOMER_EDIT_DRAFT_PREFIX = "porter.customer.editDraft.";

function customerEditDraftKey(customerId: number) {
  return `${CUSTOMER_EDIT_DRAFT_PREFIX}${customerId}`;
}

function readCustomerEditDraft(customerId: number): CustomerEditForm | null {
  try {
    const draft = window.localStorage.getItem(customerEditDraftKey(customerId));
    return draft ? (JSON.parse(draft) as CustomerEditForm) : null;
  } catch {
    window.localStorage.removeItem(customerEditDraftKey(customerId));
    return null;
  }
}

function writeCustomerEditDraft(customerId: number, draft: CustomerEditForm) {
  window.localStorage.setItem(customerEditDraftKey(customerId), JSON.stringify(draft));
}

function removeCustomerEditDraft(customerId: number) {
  window.localStorage.removeItem(customerEditDraftKey(customerId));
}

function initials(companyName: string) {
  return companyName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

function customerType(customer: CustomerAccount): CustomerType {
  return customerTypes[customer.id % customerTypes.length];
}

function customerTypeTone(type: CustomerType) {
  return type.toLowerCase();
}

function city(customer: CustomerAccount) {
  return customer.nationalAddress?.split(",")[0]?.trim() || "Riyadh";
}

function orderCount(customer: CustomerAccount) {
  return (customer.id * 17) % 150;
}

function primaryContact(customer: CustomerAccount) {
  const emailName = customer.pocEmailAddress?.split("@")[0] ?? "";
  return emailName
    .split(/[._-]/)
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase() + part.slice(1))
    .join(" ") || "Operations Contact";
}

function statusCopy(status: AccountStatus) {
  const labels: Record<AccountStatus, string> = {
    CREATED: "Pending",
    PENDING_FOR_VERIFICATION: "Pending",
    VERIFIED: "Active",
    DEACTIVATED: "Inactive",
  };
  return labels[status];
}

function statusTone(status: AccountStatus) {
  if (status === "VERIFIED") {
    return "active";
  }
  if (status === "DEACTIVATED") {
    return "inactive";
  }
  return "pending";
}

function documentName(document: DocumentSummary | null) {
  return document?.originalFilename ?? "No document attached";
}

function orderReference(orderId: number) {
  return `PT-ORD-${String(orderId).padStart(4, "0")}`;
}

function orderStatusCopy(status: OrderStatus) {
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

function orderStatusTone(status: OrderStatus) {
  if (status === "COMPLETED") {
    return "active";
  }
  if (status === "CANCELLED") {
    return "inactive";
  }
  return "pending";
}

function formatOrderDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

export function CreateCustomerPage({
  createdBy,
  initialCustomerId,
  initialCreateFlow = false,
  initialEditMode = false,
  onCreateCustomer,
  onBackToDashboard,
  onCustomerDetails,
  onCustomerEdit,
  onCustomerList,
  onOrders,
  onVehicles,
  displayName,
  role,
}: CreateCustomerPageProps) {
  const [showCreateForm, setShowCreateForm] = useState(initialCreateFlow);
  const [customers, setCustomers] = useState<CustomerAccount[]>([]);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [cityFilter, setCityFilter] = useState("");
  const [sortBy, setSortBy] = useState<SortKey>("newest");
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerAccount | null>(null);
  const [editingCustomer, setEditingCustomer] = useState(false);
  const [savingCustomer, setSavingCustomer] = useState(false);
  const [editForm, setEditForm] = useState<CustomerEditForm | null>(null);
  const [editDocuments, setEditDocuments] = useState<CustomerEditDocuments>({
    registrationDocument: null,
    vatDocument: null,
    nationalAddressDocument: null,
  });

  useEffect(() => {
    setShowCreateForm(initialCreateFlow);
  }, [initialCreateFlow]);

  useEffect(() => {
    if (initialCreateFlow) {
      setSelectedCustomer(null);
    }
  }, [initialCreateFlow]);

  useEffect(() => {
    let ignore = false;

    async function loadCustomers() {
      try {
        const response = await apiClient.get<CustomerAccount[]>("/customer-accounts");
        if (!ignore) {
          setCustomers(response);
        }
      } catch (exception) {
        if (!ignore) {
          setError(
            exception instanceof Error
              ? exception.message
              : "Could not load customers."
          );
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadCustomers();

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!initialCustomerId) {
      if (!initialCreateFlow) {
        setSelectedCustomer(null);
      }
      return;
    }

    const matchingCustomer = customers.find((customer) => customer.id === initialCustomerId);
    if (matchingCustomer) {
      const draft = initialEditMode
        ? readCustomerEditDraft(matchingCustomer.id)
        : null;
      setSelectedCustomer(matchingCustomer);
      setShowCreateForm(false);
      setEditingCustomer(initialEditMode);
      setEditForm(
        initialEditMode
          ? draft ?? customerToEditForm(matchingCustomer)
          : null
      );
      setEditDocuments({
        registrationDocument: null,
        vatDocument: null,
        nationalAddressDocument: null,
      });
    }
  }, [customers, initialCreateFlow, initialCustomerId, initialEditMode]);

  useEffect(() => {
    let ignore = false;

    async function loadOrders() {
      try {
        const response = await apiClient.get<OrderResponse[]>("/orders");
        if (!ignore) {
          setOrders(response);
        }
      } catch {
        if (!ignore) {
          setOrders([]);
        }
      } finally {
        if (!ignore) {
          setOrdersLoading(false);
        }
      }
    }

    loadOrders();

    return () => {
      ignore = true;
    };
  }, []);

  const cities = useMemo(
    () => Array.from(new Set(customers.map(city))).sort(),
    [customers]
  );

  const filteredCustomers = useMemo(() => {
    const query = search.trim().toLowerCase();
    const nextCustomers = customers.filter((customer) => {
      const matchesSearch =
        !query ||
        [
          customer.companyName,
          customer.pocContactNumber,
          customer.pocEmailAddress,
          city(customer),
        ]
          .join(" ")
          .toLowerCase()
          .includes(query);
      const matchesType = !typeFilter || customerType(customer) === typeFilter;
      const matchesStatus =
        !statusFilter || statusTone(customer.accountStatus) === statusFilter;
      const matchesCity = !cityFilter || city(customer) === cityFilter;

      return matchesSearch && matchesType && matchesStatus && matchesCity;
    });

    return nextCustomers.sort((first, second) => {
      if (sortBy === "name_asc") {
        return first.companyName.localeCompare(second.companyName);
      }
      const firstTime = new Date(first.createdAt).getTime();
      const secondTime = new Date(second.createdAt).getTime();
      return sortBy === "oldest" ? firstTime - secondTime : secondTime - firstTime;
    });
  }, [cityFilter, customers, search, sortBy, statusFilter, typeFilter]);

  function customerToEditForm(customer: CustomerAccount): CustomerEditForm {
    return {
      companyName: customer.companyName,
      companyRegistrationNumber: customer.companyRegistrationNumber,
      vatNumber: customer.vatNumber,
      nationalAddress: customer.nationalAddress,
      pocContactNumber: customer.pocContactNumber,
      pocEmailAddress: customer.pocEmailAddress,
      commsEmailList: customer.commsEmailList.join(", "),
      accountStatus: customer.accountStatus,
    };
  }

  function openCustomerDetails(customer: CustomerAccount, startEdit = false) {
    const draft = startEdit ? readCustomerEditDraft(customer.id) : null;
    setSelectedCustomer(customer);
    setEditingCustomer(startEdit);
    setEditForm(startEdit ? draft ?? customerToEditForm(customer) : null);
    setEditDocuments({
      registrationDocument: null,
      vatDocument: null,
      nationalAddressDocument: null,
    });
    setError("");
    if (startEdit) {
      onCustomerEdit?.(customer.id);
    } else {
      onCustomerDetails?.(customer.id);
    }
  }

  function openCustomerDetailsView(customer: CustomerAccount) {
    removeCustomerEditDraft(customer.id);
    setEditingCustomer(false);
    setEditForm(null);
    setEditDocuments({
      registrationDocument: null,
      vatDocument: null,
      nationalAddressDocument: null,
    });
    onCustomerDetails?.(customer.id);
  }

  function openCustomerList() {
    setShowCreateForm(false);
    setSelectedCustomer(null);
    setEditingCustomer(false);
    setEditForm(null);
    setError("");
    if (selectedCustomer && editingCustomer) {
      removeCustomerEditDraft(selectedCustomer.id);
    }
    onCustomerList?.();
  }

  function openCreateCustomer() {
    setShowCreateForm(true);
    setSelectedCustomer(null);
    setError("");
    onCreateCustomer?.();
  }

  function startEditingCustomer() {
    if (!selectedCustomer) {
      return;
    }

    setEditingCustomer(true);
    setEditForm(
      readCustomerEditDraft(selectedCustomer.id) ?? customerToEditForm(selectedCustomer)
    );
    setEditDocuments({
      registrationDocument: null,
      vatDocument: null,
      nationalAddressDocument: null,
    });
    onCustomerEdit?.(selectedCustomer.id);
  }

  function updateEditField(field: keyof CustomerEditForm, value: string) {
    setEditForm((current) => {
      if (!current) {
        return current;
      }

      const next = { ...current, [field]: value };
      if (selectedCustomer) {
        writeCustomerEditDraft(selectedCustomer.id, next);
      }
      return next;
    });
  }

  function updateEditDocument(field: keyof CustomerEditDocuments, file: File | null) {
    setEditDocuments((current) => ({ ...current, [field]: file }));
  }

  async function saveCustomerDetails() {
    if (!selectedCustomer || !editForm) {
      return;
    }

    setSavingCustomer(true);
    setError("");

    const formData = new FormData();
    formData.append("companyName", editForm.companyName);
    formData.append("companyRegistrationNumber", editForm.companyRegistrationNumber);
    formData.append("vatNumber", editForm.vatNumber);
    formData.append("nationalAddress", editForm.nationalAddress);
    formData.append("pocContactNumber", editForm.pocContactNumber);
    formData.append("pocEmailAddress", editForm.pocEmailAddress);
    editForm.commsEmailList
      .split(",")
      .map((email) => email.trim())
      .filter(Boolean)
      .forEach((email) => formData.append("commsEmailList", email));
    formData.append("accountStatus", editForm.accountStatus);
    formData.append("updatedBy", createdBy);
    if (editDocuments.registrationDocument) {
      formData.append("registrationDocument", editDocuments.registrationDocument);
    }
    if (editDocuments.vatDocument) {
      formData.append("vatDocument", editDocuments.vatDocument);
    }
    if (editDocuments.nationalAddressDocument) {
      formData.append("nationalAddressDocument", editDocuments.nationalAddressDocument);
    }

    try {
      const updatedCustomer = await apiClient.putForm<CustomerAccount>(
        `/customer-accounts/${selectedCustomer.id}`,
        formData
      );
      setCustomers((current) =>
        current.map((customer) =>
          customer.id === updatedCustomer.id ? updatedCustomer : customer
        )
      );
      setSelectedCustomer(updatedCustomer);
      setEditingCustomer(false);
      setEditForm(null);
      setEditDocuments({
        registrationDocument: null,
        vatDocument: null,
        nationalAddressDocument: null,
      });
      removeCustomerEditDraft(updatedCustomer.id);
      onCustomerDetails?.(updatedCustomer.id);
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "Could not update customer details."
      );
    } finally {
      setSavingCustomer(false);
    }
  }

  if (showCreateForm) {
    return (
      <main className="dashboard-page">
        <Sidebar
          activeItem="Customers"
          onDashboard={onBackToDashboard}
          onCustomers={openCustomerList}
          onOrders={onOrders}
          onVehicles={onVehicles}
          role={role}
        />
        <div className="dashboard-shell">
          <section className="customer-workspace">
            <div className="customer-workspace__topline">
              <nav className="breadcrumb" aria-label="Breadcrumb">
                <span onClick={openCustomerList}>Customers</span>
                <span className="material-symbols-outlined">chevron_right</span>
                <span className="active">Create Customer</span>
              </nav>
              <Topbar displayName={displayName} role={role} showSearch={false} />
            </div>
            <header className="customer-workspace__header">
              <div>
                <h1>Create Customer Account</h1>
                <p>
                  Capture customer profile, KSA compliance identifiers, contact routing,
                  and optional documents.
                </p>
              </div>
              <button
                className="customer-button customer-button--secondary"
                type="button"
                onClick={openCustomerList}
              >
                <span className="material-symbols-outlined">arrow_back</span>
                Back
              </button>
            </header>
            <CustomerForm
              createdBy={createdBy}
              onCancel={openCustomerList}
            />
          </section>
        </div>
      </main>
    );
  }

  if (selectedCustomer) {
    const details = editForm ?? {
      companyName: selectedCustomer.companyName,
      companyRegistrationNumber: selectedCustomer.companyRegistrationNumber,
      vatNumber: selectedCustomer.vatNumber,
      nationalAddress: selectedCustomer.nationalAddress,
      pocContactNumber: selectedCustomer.pocContactNumber,
      pocEmailAddress: selectedCustomer.pocEmailAddress,
      commsEmailList: selectedCustomer.commsEmailList.join(", "),
      accountStatus: selectedCustomer.accountStatus,
    };
    const selectedCustomerOrders = orders
      .filter((order) => order.customerAccountId === selectedCustomer.id)
      .sort(
        (first, second) =>
          new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime()
      );

    return (
      <main className="dashboard-page">
        <Sidebar
          activeItem="Customers"
          onDashboard={onBackToDashboard}
          onCustomers={openCustomerList}
          onOrders={onOrders}
          onVehicles={onVehicles}
          role={role}
        />
        <div className="dashboard-shell">
          <Topbar
            breadcrumb={["Customers", selectedCustomer.companyName]}
            displayName={displayName}
            role={role}
            showSearch={false}
          />
          <section className="customer-detail-page">
            <header className="customer-detail-header">
              <div>
                <button
                  className="customer-detail-back"
                  type="button"
                  onClick={openCustomerList}
                >
                  <span className="material-symbols-outlined">arrow_back</span>
                  Customers
                </button>
                <h1>{selectedCustomer.companyName}</h1>
                <p>{city(selectedCustomer)} · {statusCopy(selectedCustomer.accountStatus)}</p>
              </div>
              <div className="customer-management__actions">
                {editingCustomer ? (
                  <>
                    <button
                      className="customer-management-button"
                      type="button"
	                      onClick={() => {
	                        openCustomerDetailsView(selectedCustomer);
	                      }}
                    >
                      Cancel
                    </button>
                    <button
                      className="customer-management-button customer-management-button--primary"
                      disabled={savingCustomer}
                      type="button"
                      onClick={saveCustomerDetails}
                    >
                      <span className="material-symbols-outlined">save</span>
                      {savingCustomer ? "Saving..." : "Save Changes"}
                    </button>
                  </>
                ) : (
                  <button
                    className="customer-management-button customer-management-button--primary"
                    type="button"
                    onClick={startEditingCustomer}
                  >
                    <span className="material-symbols-outlined">edit</span>
                    Edit
                  </button>
                )}
              </div>
            </header>

            {error ? (
              <p className="customer-form-message customer-form-message--error" role="alert">
                {error}
              </p>
            ) : null}

            <section className="customer-detail-panel">
              <div className="customer-detail-summary">
                <span>{initials(selectedCustomer.companyName)}</span>
                <div>
                  <strong>{selectedCustomer.companyName}</strong>
                  <small>{selectedCustomer.pocEmailAddress}</small>
                </div>
                <span
                  className={`customer-status-pill customer-status-pill--${statusTone(
                    selectedCustomer.accountStatus
                  )}`}
                >
                  <span />
                  {statusCopy(selectedCustomer.accountStatus)}
                </span>
              </div>

              <div className="customer-detail-body">
                <div className="customer-detail-main">
                  <div className="customer-detail-grid">
                  <label>
                    <span>Company Name</span>
                    <input
                      disabled={!editingCustomer}
                      value={details.companyName}
                      onChange={(event) => updateEditField("companyName", event.target.value)}
                    />
                  </label>
                  <label>
                    <span>Registration Number</span>
                    <input
                      disabled={!editingCustomer}
                      value={details.companyRegistrationNumber}
                      onChange={(event) =>
                        updateEditField("companyRegistrationNumber", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    <span>VAT Number</span>
                    <input
                      disabled={!editingCustomer}
                      value={details.vatNumber}
                      onChange={(event) => updateEditField("vatNumber", event.target.value)}
                    />
                  </label>
                  <label>
                    <span>Status</span>
                    <select
                      disabled={!editingCustomer}
                      value={details.accountStatus}
                      onChange={(event) =>
                        updateEditField("accountStatus", event.target.value as AccountStatus)
                      }
                    >
                      <option value="CREATED">Created</option>
                      <option value="PENDING_FOR_VERIFICATION">Pending Verification</option>
                      <option value="VERIFIED">Verified</option>
                      <option value="DEACTIVATED">Deactivated</option>
                    </select>
                  </label>
                  <label>
                    <span>POC Contact Number</span>
                    <input
                      disabled={!editingCustomer}
                      value={details.pocContactNumber}
                      onChange={(event) =>
                        updateEditField("pocContactNumber", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    <span>POC Email Address</span>
                    <input
                      disabled={!editingCustomer}
                      type="email"
                      value={details.pocEmailAddress}
                      onChange={(event) =>
                        updateEditField("pocEmailAddress", event.target.value)
                      }
                    />
                  </label>
                  <label className="customer-detail-grid__wide">
                    <span>Comms Email List</span>
                    <input
                      disabled={!editingCustomer}
                      value={details.commsEmailList}
                      onChange={(event) =>
                        updateEditField("commsEmailList", event.target.value)
                      }
                    />
                  </label>
                  <label className="customer-detail-grid__wide">
                    <span>National Address</span>
                    <textarea
                      disabled={!editingCustomer}
                      rows={4}
                      value={details.nationalAddress}
                      onChange={(event) =>
                        updateEditField("nationalAddress", event.target.value)
                      }
                    />
                  </label>
                  </div>

                  <div className="customer-detail-documents">
                  <h2>Documents</h2>
                  <label className="customer-detail-document-card">
                    <span className="material-symbols-outlined">description</span>
                    <span>
                      <strong>Registration Document</strong>
                      <small>
                        {editDocuments.registrationDocument?.name ??
                          documentName(selectedCustomer.registrationDocument)}
                      </small>
                    </span>
                    {editingCustomer ? (
                      <span className="customer-detail-document-card__action">
                        Replace
                        <input
                          accept="application/pdf,image/jpeg"
                          type="file"
                          onChange={(event) =>
                            updateEditDocument(
                              "registrationDocument",
                              event.target.files?.[0] ?? null
                            )
                          }
                        />
                      </span>
                    ) : null}
                  </label>
                  <label className="customer-detail-document-card">
                    <span className="material-symbols-outlined">receipt_long</span>
                    <span>
                      <strong>VAT Document</strong>
                      <small>
                        {editDocuments.vatDocument?.name ??
                          documentName(selectedCustomer.vatDocument)}
                      </small>
                    </span>
                    {editingCustomer ? (
                      <span className="customer-detail-document-card__action">
                        Replace
                        <input
                          accept="application/pdf,image/jpeg"
                          type="file"
                          onChange={(event) =>
                            updateEditDocument(
                              "vatDocument",
                              event.target.files?.[0] ?? null
                            )
                          }
                        />
                      </span>
                    ) : null}
                  </label>
                  <label className="customer-detail-document-card">
                    <span className="material-symbols-outlined">location_on</span>
                    <span>
                      <strong>National Address Document</strong>
                      <small>
                        {editDocuments.nationalAddressDocument?.name ??
                          documentName(selectedCustomer.nationalAddressDocument)}
                      </small>
                    </span>
                    {editingCustomer ? (
                      <span className="customer-detail-document-card__action">
                        Replace
                        <input
                          accept="application/pdf,image/jpeg"
                          type="file"
                          onChange={(event) =>
                            updateEditDocument(
                              "nationalAddressDocument",
                              event.target.files?.[0] ?? null
                            )
                          }
                        />
                      </span>
                    ) : null}
                  </label>
                  </div>
                </div>

                <aside className="customer-detail-orders">
                  <header>
                    <h2>Order History</h2>
                    <span>{selectedCustomerOrders.length}</span>
                  </header>
                  <div className="customer-detail-order-list">
                    {ordersLoading ? (
                      <div className="customer-detail-order-empty">Loading orders...</div>
                    ) : null}
                    {!ordersLoading && selectedCustomerOrders.length === 0 ? (
                      <div className="customer-detail-order-empty">No orders yet.</div>
                    ) : null}
                    {!ordersLoading
                      ? selectedCustomerOrders.map((order) => (
                          <article className="customer-detail-order-card" key={order.id}>
                            <div className="customer-detail-order-card__top">
                              <strong>{orderReference(order.id)}</strong>
                              <span
                                className={`customer-status-pill customer-status-pill--${orderStatusTone(
                                  order.orderStatus
                                )}`}
                              >
                                <span />
                                {orderStatusCopy(order.orderStatus)}
                              </span>
                            </div>
                            <p>{order.fromLocation} to {order.toLocation}</p>
                            <dl>
                              <div>
                                <dt>Vehicle</dt>
                                <dd>{order.vehicleType}</dd>
                              </div>
                              <div>
                                <dt>Assigned</dt>
                                <dd>{order.assignedVehicles}</dd>
                              </div>
                              <div>
                                <dt>Created</dt>
                                <dd>{formatOrderDate(order.createdAt)}</dd>
                              </div>
                            </dl>
                          </article>
                        ))
                      : null}
                  </div>
                </aside>
              </div>
            </section>
          </section>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-page">
      <Sidebar
        activeItem="Customers"
        onDashboard={onBackToDashboard}
        onCustomers={openCustomerList}
        onOrders={onOrders}
        onVehicles={onVehicles}
        role={role}
      />
      <div className="dashboard-shell">
        <section className="customer-management">
          <div className="customer-management__topline">
            <nav className="breadcrumb" aria-label="Breadcrumb">
              <span onClick={onBackToDashboard}>Dashboard</span>
              <span className="material-symbols-outlined">chevron_right</span>
              <span className="active">Customers</span>
            </nav>
            <Topbar displayName={displayName} role={role} showSearch />
          </div>

          <header className="customer-management__header">
            <div>
              <h1>Customers</h1>
              <p>Manage company accounts and customer information.</p>
            </div>
            <div className="customer-management__actions">
              <button className="customer-management-button" type="button">
                <span className="material-symbols-outlined">download</span>
                Export Customers
              </button>
              <button
                className="customer-management-button customer-management-button--primary"
                type="button"
                onClick={openCreateCustomer}
              >
                <span className="material-symbols-outlined">add</span>
                Create Customer
              </button>
            </div>
          </header>

          <section className="customer-table-panel data-panel">
            <div className="customer-table-toolbar">
              <label className="customer-management-search">
                <span className="material-symbols-outlined">search</span>
                <input
                  placeholder="Search by company name, contact person, phone number, or email"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </label>

              <div className="customer-filter-row">
                <label>
                  <select
                    value={typeFilter}
                    onChange={(event) => setTypeFilter(event.target.value)}
                  >
                    <option value="">Customer Type: All</option>
                    {customerTypes.map((type) => (
                      <option key={type} value={type}>
                        {type}
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
                    <option value="active">Active</option>
                    <option value="pending">Pending</option>
                    <option value="inactive">Inactive</option>
                  </select>
                  <span className="material-symbols-outlined">expand_more</span>
                </label>
                <label>
                  <select
                    value={cityFilter}
                    onChange={(event) => setCityFilter(event.target.value)}
                  >
                    <option value="">City: All</option>
                    {cities.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    ))}
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
                    <option value="name_asc">Name A-Z</option>
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

            <div className="customer-management-table-header" aria-hidden="true">
              <table className="customer-management-table">
                <colgroup>
                  <col className="customer-table-col customer-table-col--company" />
                  <col className="customer-table-col customer-table-col--contact" />
                  <col className="customer-table-col customer-table-col--info" />
                  <col className="customer-table-col customer-table-col--type" />
                  <col className="customer-table-col customer-table-col--orders" />
                  <col className="customer-table-col customer-table-col--status" />
                </colgroup>
                <thead>
                  <tr>
                    <th>Company Name</th>
                    <th>Primary Contact</th>
                    <th>Contact Info</th>
                    <th>Type</th>
                    <th className="align-right">Orders</th>
                    <th>Status</th>
                  </tr>
                </thead>
              </table>
            </div>

            <div className="customer-management-table-wrap table-scroll">
              <table className="customer-management-table">
                <colgroup>
                  <col className="customer-table-col customer-table-col--company" />
                  <col className="customer-table-col customer-table-col--contact" />
                  <col className="customer-table-col customer-table-col--info" />
                  <col className="customer-table-col customer-table-col--type" />
                  <col className="customer-table-col customer-table-col--orders" />
                  <col className="customer-table-col customer-table-col--status" />
                </colgroup>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={6}>Loading customers...</td>
                    </tr>
                  ) : null}
                  {!loading && filteredCustomers.length === 0 ? (
                    <tr>
                      <td colSpan={6}>No customers found.</td>
                    </tr>
                  ) : null}
                  {!loading
                    ? filteredCustomers.map((customer) => (
                        <tr key={customer.id} onClick={() => openCustomerDetails(customer)}>
                          <td>
                            <div className="customer-company-cell">
                              <span>{initials(customer.companyName)}</span>
                              <div>
                                <button type="button">{customer.companyName}</button>
                                <small>{city(customer)}</small>
                              </div>
                            </div>
                          </td>
                          <td>{primaryContact(customer)}</td>
                          <td>
                            <div className="customer-contact-cell">
                              <span>{customer.pocContactNumber}</span>
                              <small>{customer.pocEmailAddress}</small>
                            </div>
                          </td>
                          <td>
                            <span
                              className={`customer-type-pill customer-type-pill--${customerTypeTone(
                                customerType(customer)
                              )}`}
                            >
                              {customerType(customer)}
                            </span>
                          </td>
                          <td className="align-right">
                            <div className="customer-orders-cell">
                              <span>{orderCount(customer)}</span>
                            </div>
                          </td>
                          <td>
                            <span
                              className={`customer-status-pill customer-status-pill--${statusTone(
                                customer.accountStatus
                              )}`}
                            >
                              <span />
                              {statusCopy(customer.accountStatus)}
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
