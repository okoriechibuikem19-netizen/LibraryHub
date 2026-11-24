import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router";
import { ArrowLeft, BookOpen, LayoutDashboard, LibraryBig, LogOut, Menu, Users, X } from "lucide-react";
import { roleLabel, useAuth } from "../../auth/AuthContext";

// The staff area's sidebar menu. Add new admin pages here.
// roles: leave it out = everyone who is signed in; roles: ["ADMIN", "LIBRARIAN"] = staff only.
const NAV_ITEMS = [
  { to: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { to: "/admin/books", label: "Library books", icon: BookOpen },
  { to: "/admin/users", label: "Users", icon: Users, roles: ["ADMIN"] },
];

/** Sidebar + top bar around the staff (admin) pages. The page itself appears at <Outlet />. */
export function AppLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  if (!user) return null;

  const items = NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(user.role));

  async function handleSignOut() {
    await signOut();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar${menuOpen ? " open" : ""}`}>
        <div className="brand">
          <LibraryBig size={22} /> LibraryHub
        </div>
        <nav>
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/admin"}
              className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
              onClick={() => setMenuOpen(false)}
            >
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        <Link to="/" className="nav-link back-to-site"><ArrowLeft size={18} /> Back to the website</Link>
        <div className="sidebar-user">
          <div className="avatar" aria-hidden="true">
            {user.name.charAt(0).toUpperCase()}
          </div>
          <div className="sidebar-user-text">
            <strong>{user.name}</strong>
            <span>{roleLabel(user.role)}</span>
          </div>
          <button className="icon-btn" onClick={handleSignOut} title="Sign out" aria-label="Sign out">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      {menuOpen && <div className="sidebar-backdrop" onClick={() => setMenuOpen(false)} />}

      <div className="main">
        <header className="topbar">
          <button className="icon-btn menu-btn" onClick={() => setMenuOpen((open) => !open)} aria-label="Menu">
            {menuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
          <span className="topbar-title">LibraryHub</span>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
