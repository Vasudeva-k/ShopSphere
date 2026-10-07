import React from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Home, ShoppingBag } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="container" style={{ paddingTop: '4rem', paddingBottom: '4rem', textAlign: 'center', maxWidth: '600px' }}>
      <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-lg)', padding: '3.5rem 2rem', boxShadow: 'var(--shadow-sm)' }}>
        <AlertCircle size={64} style={{ color: 'var(--brand-accent)', margin: '0 auto 1.25rem' }} />
        <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.75rem', color: 'var(--text-main)' }}>Page Not Found (404)</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem', marginBottom: '2rem', lineHeight: 1.6 }}>
          We couldn't find the page you are looking for. It might have been moved, renamed, or is temporarily unavailable.
        </p>

        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/" className="btn btn-primary" style={{ backgroundColor: 'var(--brand-accent)', color: 'var(--primary)', fontWeight: 700 }}>
            <Home size={16} /> Return to Homepage
          </Link>
          <Link to="/products" className="btn btn-outline">
            <ShoppingBag size={16} /> Explore All Products
          </Link>
        </div>
      </div>
    </div>
  );
};
