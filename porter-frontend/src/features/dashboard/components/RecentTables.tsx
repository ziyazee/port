const orders = [
  { id: "#ORD-9012", customer: "Acme Corp", status: "Dispatched", date: "Oct 24, 2023" },
  { id: "#ORD-9011", customer: "Globex Inc", status: "Processing", date: "Oct 23, 2023" },
  { id: "#ORD-9010", customer: "Stark Ind.", status: "Pending", date: "Oct 22, 2023" },
];

const customers = [
  { name: "Acme Corp", email: "contact@acme.com", lastActivity: "2 hours ago", active: true },
  { name: "Globex Inc", email: "info@globex.com", lastActivity: "1 day ago", active: true },
  { name: "Stark Ind.", email: "billing@stark.com", lastActivity: "3 days ago", active: false },
];

function getStatusClass(status: string) {
  if (status === "Processing") {
    return "status-pill status-pill--processing";
  }

  if (status === "Dispatched") {
    return "status-pill status-pill--dispatched";
  }

  return "status-pill status-pill--pending";
}

function getAvatarClass(index: number) {
  const classes = ["customer-avatar--tertiary", "customer-avatar--primary", "customer-avatar--neutral"];
  return `customer-avatar ${classes[index % classes.length]}`;
}

export function RecentTables() {
  return (
    <section className="recent-grid">
      <article className="data-panel">
        <header className="data-panel__header">
          <h2>Recently Created Orders</h2>
          <button type="button">
            View All
            <span className="material-symbols-outlined">arrow_forward</span>
          </button>
        </header>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td className="mono-cell">{order.id}</td>
                  <td>{order.customer}</td>
                  <td>
                    <span className={getStatusClass(order.status)}>
                      <span className="status-pill__dot" />
                      {order.status}
                    </span>
                  </td>
                  <td className="muted-cell">{order.date}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>

      <article className="data-panel">
        <header className="data-panel__header">
          <h2>Recently Created Customers</h2>
          <button type="button">
            View All
            <span className="material-symbols-outlined">arrow_forward</span>
          </button>
        </header>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Customer</th>
                <th>Last Activity</th>
                <th className="align-right">Status</th>
              </tr>
            </thead>
            <tbody>
              {customers.map((customer, index) => (
                <tr key={customer.name}>
                  <td>
                    <div className="customer-cell">
                      <div className={getAvatarClass(index)}>
                        {customer.name.charAt(0)}
                      </div>
                      <div className="customer-info">
                        <span>{customer.name}</span>
                        <span>{customer.email}</span>
                      </div>
                    </div>
                  </td>
                  <td className="muted-cell">{customer.lastActivity}</td>
                  <td className="align-right">
                    <span 
                      className={`material-symbols-outlined status-icon ${
                        customer.active ? "status-icon--active" : "status-icon--pending"
                      }`}
                    >
                      {customer.active ? "check_circle" : "pending"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>
    </section>
  );
}
