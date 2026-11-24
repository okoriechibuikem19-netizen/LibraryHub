import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router";
import { BookOpen, ChevronDown, LayoutDashboard, LibraryBig, LogOut, Search, Settings, UserRound } from "lucide-react";
import { STAFF_ROLES, useAuth } from "../../auth/AuthContext";

/** The public website frame: top bar with search and account menu, page content, footer. */
export function SiteLayout() {
  const { user, signOut, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [query, setQuery] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  // Close the account menu when clicking elsewhere or changing page.
  useEffect(() => setMenuOpen(false), [location.pathname]);
  useEffect(() => {
    const close = (e) => menuRef.current && !menuRef.current.contains(e.target) && setMenuOpen(false);
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  function handleSearch(event) {
    event.preventDefault();
    if (query.trim()) navigate(`/browse?q=${encodeURIComponent(query.trim())}`);
  }

  async function handleSignOut() {
    await signOut();
    navigate("/", { replace: true });
  }

  const navClass = ({ isActive }) => `site-nav-link${isActive ? " active" : ""}`;

  return (
    <div className="site">
      <header className="site-header">
        <div className="site-header-inner">
          <Link to="/" className="brand site-brand">
            <LibraryBig size={24} /> LibraryHub
          </Link>
          <nav className="site-nav">
            <NavLink to="/" end className={navClass}>Home</NavLink>
            <NavLink to="/browse" className={navClass}>Novels</NavLink>
            <NavLink to="/textbooks" className={navClass}>Textbooks</NavLink>
            {user && <NavLink to="/library" className={navClass}>My Library</NavLink>}
          </nav>
          <form className="site-search" onSubmit={handleSearch} role="search">
            <Search size={17} aria-hidden="true" />
            <input
              type="search"
              placeholder="Search titles or authors"
              aria-label="Search books"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </form>
          {user ? (
            <div className="account-menu" ref={menuRef}>
              <button className="account-button" onClick={() => setMenuOpen((o) => !o)} aria-expanded={menuOpen}>
                <span className="avatar" aria-hidden="true">{user.name.charAt(0).toUpperCase()}</span>
                <span className="account-name">{user.name.split(" ")[0]}</span>
                <ChevronDown size={16} />
              </button>
              {menuOpen && (
                <div className="account-dropdown" role="menu">
                  <Link to="/library" role="menuitem"><BookOpen size={16} /> My Library</Link>
                  <Link to="/account" role="menuitem"><Settings size={16} /> Account & password</Link>
                  {hasRole(...STAFF_ROLES) && (
                    <Link to="/admin" role="menuitem"><LayoutDashboard size={16} /> Library admin</Link>
                  )}
                  <button onClick={handleSignOut} role="menuitem"><LogOut size={16} /> Sign out</button>
                </div>
              )}
            </div>
          ) : (
            <div className="site-auth">
              <Link to="/login" state={{ from: location.pathname + location.search }} className="btn btn-ghost btn-sm">
                <UserRound size={16} /> Sign in
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm">Join free</Link>
            </div>
          )}
        </div>
      </header>

      <main className="site-main">
        <Outlet />
      </main>

      <footer className="site-footer">
        <p>
          <strong>LibraryHub</strong>: free novels and textbooks. Online books come from{" "}
          <a href="https://www.gutenberg.org" target="_blank" rel="noreferrer">Project Gutenberg</a> and are in the public domain.
        </p>
      </footer>
    </div>
  );
}
