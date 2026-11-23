type TopbarProps = {
  breadcrumb?: string[];
  showSearch?: boolean;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

function roleLabel(role: "ADMIN" | "OPERATIONS") {
  return role === "ADMIN" ? "Admin" : "Operations";
}

export function Topbar({
  breadcrumb,
  showSearch = true,
  displayName,
  role,
}: TopbarProps) {
  return (
    <div className={`dashboard-topbar${breadcrumb ? "" : " dashboard-topbar--right"}`}>
      {breadcrumb ? (
        <nav className="breadcrumb" aria-label="Breadcrumb">
          {breadcrumb.map((item, index) => (
            <span key={item} className={index === breadcrumb.length - 1 ? "active" : ""}>
              {index > 0 ? (
                <span className="material-symbols-outlined">chevron_right</span>
              ) : null}
              {item}
            </span>
          ))}
        </nav>
      ) : null}

      {showSearch ? (
        <label className="dashboard-search">
          <span className="material-symbols-outlined">search</span>
          <input placeholder="Search..." />
          <span className="dashboard-search__shortcut">⌘K</span>
        </label>
      ) : null}

      <div className="dashboard-topbar__actions">
        <button className="icon-button icon-button--notification" type="button">
          <span className="material-symbols-outlined">notifications</span>
        </button>
        <div className="user-profile">
          <div className="user-avatar">
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDVfksU-Qp0KpGckGNJ1JWUiRLqlHD0HUSn7gtiFOXmevEdUCKcjQxMIDH4AyEg5nQV944By_qK7lYL_-BX8FW0cMatCsRquhq281QInRFdFxC8zGq4rxShtTRVWDkA6lyCK881ju5GMZtHyla01e_lVthNgYPpvdvLlJkNE8vzAAdSbuRS26NjmNI0VNW6CXj1JWPJypsvZEkouxeOKVZ4aZLgW3eYELYyEzbiTvwr6YNm5-EGu-PfAg"
              alt="User Profile Avatar"
            />
          </div>
          <div className="user-info">
            <span className="user-info__name">{displayName}</span>
            <span className="user-info__role">{roleLabel(role)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
