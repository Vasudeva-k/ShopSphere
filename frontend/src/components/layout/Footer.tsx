import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, Truck, RotateCcw, Headset } from 'lucide-react';

const CURRENT_YEAR = new Date().getFullYear();

export const Footer: React.FC = () => {
  return (
    <footer className="site-footer">
      <div className="container">
        {/* Value Propositions / Trust Badges */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.5rem',
          paddingBottom: '2.5rem',
          marginBottom: '2.5rem',
          borderBottom: '1px solid #334155'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <Truck size={32} style={{ color: 'var(--brand-accent)' }} />
            <div>
              <h5 style={{ color: '#fff', fontWeight: 700 }}>Fast Delivery</h5>
              <p style={{ fontSize: '0.8125rem' }}>Tracked shipping on all orders</p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <ShieldCheck size={32} style={{ color: 'var(--brand-accent)' }} />
            <div>
              <h5 style={{ color: '#fff', fontWeight: 700 }}>Secure Checkout</h5>
              <p style={{ fontSize: '0.8125rem' }}>Encrypted payments & JWT safety</p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <RotateCcw size={32} style={{ color: 'var(--brand-accent)' }} />
            <div>
              <h5 style={{ color: '#fff', fontWeight: 700 }}>Hassle-Free Returns</h5>
              <p style={{ fontSize: '0.8125rem' }}>30-day money-back guarantee</p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <Headset size={32} style={{ color: 'var(--brand-accent)' }} />
            <div>
              <h5 style={{ color: '#fff', fontWeight: 700 }}>24/7 Support</h5>
              <p style={{ fontSize: '0.8125rem' }}>Dedicated customer care team</p>
            </div>
          </div>
        </div>

        {/* Footer Navigation Columns */}
        <div className="footer-grid">
          <div className="footer-col">
            <h4>Get to Know Us</h4>
            <ul className="footer-links">
              <li><Link to="/">About ShopSphere</Link></li>
              <li><Link to="/">Careers</Link></li>
              <li><Link to="/">Press Releases</Link></li>
              <li><Link to="/">ShopSphere Science</Link></li>
            </ul>
          </div>

          <div className="footer-col">
            <h4>Shop With Us</h4>
            <ul className="footer-links">
              <li><Link to="/products">All Products</Link></li>
              <li><Link to="/profile">Your Account</Link></li>
              <li><Link to="/orders">Your Orders</Link></li>
              <li><Link to="/wishlist">Your Wishlist</Link></li>
            </ul>
          </div>

          <div className="footer-col">
            <h4>Payment & Invoicing</h4>
            <ul className="footer-links">
              <li><Link to="/cart">ShopSphere Wallet</Link></li>
              <li><Link to="/orders">Download Invoices</Link></li>
              <li><Link to="/orders">Payment Security</Link></li>
            </ul>
          </div>

          <div className="footer-col">
            <h4>Let Us Help You</h4>
            <ul className="footer-links">
              <li><Link to="/">Help & FAQs</Link></li>
              <li><Link to="/orders">Shipping Rates & Policies</Link></li>
              <li><Link to="/orders">Returns & Replacements</Link></li>
              <li><Link to="/">Contact Us</Link></li>
            </ul>
          </div>
        </div>

        {/* Footer Bottom */}
        <div className="footer-bottom">
          <p>© {CURRENT_YEAR} ShopSphere Inc. All rights reserved.</p>
          <div style={{ display: 'flex', gap: '1.5rem' }}>
            <Link to="/">Privacy Notice</Link>
            <Link to="/">Conditions of Use</Link>
            <Link to="/">Security Policies</Link>
          </div>
        </div>
      </div>
    </footer>
  );
};
