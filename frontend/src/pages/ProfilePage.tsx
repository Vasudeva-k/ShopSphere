import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User as UserIcon, Package, Heart, Shield, LogOut, CheckCircle, Mail, Phone, Calendar } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const ProfilePage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  if (!user) {
    return null;
  }

  return (
    <div className="container" style={{ paddingTop: '2rem', maxWidth: '1000px' }}>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Your Account & Settings</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Manage your personal profile, active orders, saved wishlist items, and security preferences
          </p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="btn btn-outline"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--danger)', borderColor: '#fecaca' }}
        >
          <LogOut size={16} /> Sign Out
        </button>
      </div>

      {/* Account Quick Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.5rem', marginBottom: '2.5rem' }}>
        <Link
          to="/orders"
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
            display: 'flex',
            gap: '1rem',
            alignItems: 'center',
            transition: 'transform 0.2s',
            boxShadow: 'var(--shadow-xs)',
          }}
        >
          <div style={{ background: 'var(--brand-accent-light)', color: 'var(--brand-accent-hover)', padding: '0.75rem', borderRadius: 'var(--radius-md)' }}>
            <Package size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>Your Orders</h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Track packages, edit or view invoices</p>
          </div>
        </Link>

        <Link
          to="/wishlist"
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
            display: 'flex',
            gap: '1rem',
            alignItems: 'center',
            transition: 'transform 0.2s',
            boxShadow: 'var(--shadow-xs)',
          }}
        >
          <div style={{ background: 'var(--danger-bg)', color: 'var(--danger)', padding: '0.75rem', borderRadius: 'var(--radius-md)' }}>
            <Heart size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>Your Wishlist</h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>View saved items and deal notifications</p>
          </div>
        </Link>

        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
            display: 'flex',
            gap: '1rem',
            alignItems: 'center',
            boxShadow: 'var(--shadow-xs)',
          }}
        >
          <div style={{ background: 'var(--info-bg)', color: 'var(--info)', padding: '0.75rem', borderRadius: 'var(--radius-md)' }}>
            <Shield size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>Login & Security</h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Active session with JWT protection</p>
          </div>
        </div>
      </div>

      {/* User Details Form Section */}
      <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-lg)', padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <UserIcon size={20} /> Personal Profile Information
          </h2>
          <span className="badge badge-success" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle size={14} /> Active Account
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.75rem' }}>
          <div>
            <label style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>Full Name</label>
            <div style={{ fontWeight: 600, fontSize: '1rem' }}>
              {user.firstName} {user.lastName}
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>Primary Email</label>
            <div style={{ fontWeight: 600, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Mail size={16} style={{ color: 'var(--text-light)' }} /> {user.email}
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>Phone Number</label>
            <div style={{ fontWeight: 600, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Phone size={16} style={{ color: 'var(--text-light)' }} /> {user.phone || 'Not provided'}
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>Member Since</label>
            <div style={{ fontWeight: 600, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Calendar size={16} style={{ color: 'var(--text-light)' }} />{' '}
              {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active'}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
