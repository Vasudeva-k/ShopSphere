import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Search,
  ShoppingCart,
  Heart,
  User as UserIcon,
  Package,
  Layers,
  Sparkles,
  LogOut,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useCart } from '../../hooks/useCart';
import { useWishlist } from '../../hooks/useWishlist';
import { categoryService } from '../../services/categoryService';
import type { Category } from '../../types';

export const Header: React.FC = () => {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [categories, setCategories] = useState<Category[]>([]);
  const { user, isAuthenticated, logout } = useAuth();
  const { itemCount: cartCount } = useCart();
  const { wishlistCount } = useWishlist();
  const navigate = useNavigate();

  useEffect(() => {
    let isMounted = true;
    categoryService
      .getCategories()
      .then((data) => {
        if (isMounted) setCategories(data.slice(0, 7));
      })
      .catch(() => {
        // Safe fallback
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchKeyword.trim())}`);
    }
  };

  const handleSignOut = async (e: React.MouseEvent) => {
    e.preventDefault();
    await logout();
    navigate('/login');
  };

  return (
    <header className="site-header">
      <div className="container">
        {/* Main Navbar */}
        <div className="header-main">
          {/* Logo */}
          <Link to="/" className="brand-logo">
            <Sparkles className="brand-highlight" size={26} />
            <span>
              Shop<span className="brand-highlight">Sphere</span>
            </span>
          </Link>

          {/* Search Bar */}
          <form className="header-search" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="Search millions of products, electronics, fashion, and deals..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              aria-label="Search ShopSphere"
            />
            <button type="submit" className="search-btn" aria-label="Submit search">
              <Search size={18} />
            </button>
          </form>

          {/* Right Action Icons */}
          <div className="header-actions">
            {/* Account Link: Authenticated vs Unauthenticated */}
            {isAuthenticated && user ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Link to="/profile" className="header-nav-item">
                  <span className="nav-subtext">Hello, {user.firstName}</span>
                  <span className="nav-maintext" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <UserIcon size={14} /> Account
                  </span>
                </Link>
                <button
                  type="button"
                  onClick={handleSignOut}
                  className="header-nav-item"
                  title="Sign out of your account"
                  style={{ background: 'none', border: 'none', color: '#cbd5e1', padding: '0.35rem' }}
                >
                  <span className="nav-subtext" style={{ color: '#ef4444' }}>Exit</span>
                  <span className="nav-maintext" style={{ display: 'flex', alignItems: 'center', gap: '3px', fontSize: '0.8125rem' }}>
                    <LogOut size={13} />
                  </span>
                </button>
              </div>
            ) : (
              <Link to="/login" className="header-nav-item">
                <span className="nav-subtext">Hello, Sign In</span>
                <span className="nav-maintext" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <UserIcon size={14} /> Account
                </span>
              </Link>
            )}

            {/* Orders Link */}
            <Link to="/orders" className="header-nav-item">
              <span className="nav-subtext">Returns</span>
              <span className="nav-maintext" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Package size={14} /> & Orders
              </span>
            </Link>

            {/* Wishlist Link with live count */}
            <Link to="/wishlist" className="header-nav-item" style={{ position: 'relative' }}>
              <span className="nav-subtext">Your</span>
              <span className="nav-maintext" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Heart size={14} /> Wishlist
                {isAuthenticated && wishlistCount > 0 && (
                  <span
                    style={{
                      background: 'var(--danger)',
                      color: '#fff',
                      fontSize: '0.7rem',
                      padding: '0 5px',
                      borderRadius: '9999px',
                      fontWeight: 800,
                      marginLeft: '2px',
                    }}
                  >
                    {wishlistCount}
                  </span>
                )}
              </span>
            </Link>

            {/* Cart Link with live badge */}
            <Link to="/cart" className="header-nav-item cart-button">
              <ShoppingCart size={24} />
              <span className="badge-count">{isAuthenticated ? cartCount : 0}</span>
              <span className="nav-maintext" style={{ marginLeft: '4px' }}>
                Cart
              </span>
            </Link>
          </div>
        </div>
      </div>

      {/* Sub-Navigation Categories Bar */}
      <nav className="sub-nav">
        <div className="container">
          <ul className="sub-nav-links">
            <li>
              <Link to="/products" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Layers size={16} /> All Products
              </Link>
            </li>
            {categories.map((cat) => (
              <li key={cat.id}>
                <Link to={`/products?categoryId=${cat.id}`}>{cat.name}</Link>
              </li>
            ))}
            <li>
              <Link to="/products?sortBy=price&direction=asc" style={{ color: 'var(--brand-accent)', fontWeight: 'bold' }}>
                ⚡ Budget Deals
              </Link>
            </li>
          </ul>
        </div>
      </nav>
    </header>
  );
};

export default Header;
