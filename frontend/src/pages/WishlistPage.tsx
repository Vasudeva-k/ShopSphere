import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Heart,
  ShoppingCart,
  Trash2,
  RotateCcw,
  Loader2,
  PackageCheck,
} from 'lucide-react';
import { useWishlist } from '../hooks/useWishlist';
import { useCart } from '../hooks/useCart';
import { formatCurrency } from '../utils/currency';

export const WishlistPage: React.FC = () => {
  const { wishlistItems, removeFromWishlist, clearWishlist, isLoading } = useWishlist();
  const { addItem } = useCart();
  const [movingItemId, setMovingItemId] = useState<number | null>(null);
  const [removingItemId, setRemovingItemId] = useState<number | null>(null);
  const [isClearing, setIsClearing] = useState<boolean>(false);

  const handleMoveToCart = async (wishlistItemId: number, productId: number) => {
    setMovingItemId(wishlistItemId);
    try {
      // 1. Add product to cart
      await addItem(productId, 1);
      // 2. If successful, remove from wishlist
      await removeFromWishlist(wishlistItemId);
    } catch {
      // If adding to cart fails, keep the wishlist item
    } finally {
      setMovingItemId(null);
    }
  };

  const handleRemove = async (wishlistItemId: number) => {
    setRemovingItemId(wishlistItemId);
    try {
      await removeFromWishlist(wishlistItemId);
    } catch {
      // Handled gracefully
    } finally {
      setRemovingItemId(null);
    }
  };

  const handleClear = async () => {
    if (!window.confirm('Are you sure you want to clear your wishlist?')) return;
    setIsClearing(true);
    try {
      await clearWishlist();
    } catch {
      // Handled gracefully
    } finally {
      setIsClearing(false);
    }
  };

  return (
    <div className="container" style={{ paddingTop: '2rem' }}>
      <div
        className="page-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Heart size={28} style={{ color: 'var(--danger)' }} /> Your Wishlist & Saved Items
        </h1>
        {wishlistItems.length > 0 && (
          <button
            type="button"
            className="btn btn-outline"
            onClick={handleClear}
            disabled={isClearing || isLoading}
            style={{ color: 'var(--danger)', borderColor: '#fecaca', fontSize: '0.8125rem' }}
          >
            {isClearing ? (
              <>
                <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} /> Clearing...
              </>
            ) : (
              <>
                <RotateCcw size={14} /> Clear Wishlist
              </>
            )}
          </button>
        )}
      </div>

      {isLoading && wishlistItems.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)' }}>
          <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
          <p style={{ fontSize: '1rem', fontWeight: 500 }}>Loading saved items...</p>
        </div>
      ) : wishlistItems.length === 0 ? (
        <div className="placeholder-card">
          <Heart className="placeholder-icon" size={48} />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>
            Your Wishlist is Empty
          </h3>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
            Save products you love to keep track of prices, deals, and stock availability.
          </p>
          <Link to="/products" className="btn btn-primary">
            Browse Products
          </Link>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.5rem' }}>
          {wishlistItems.map((item) => {
            const isMoving = movingItemId === item.id;
            const isRemoving = removingItemId === item.id;
            const inStock = item.productEnabled !== false;

            return (
              <div
                key={item.id}
                style={{
                  background: 'var(--bg-surface)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '1.5rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  boxShadow: 'var(--shadow-xs)',
                  opacity: isMoving || isRemoving ? 0.6 : 1,
                }}
              >
                <div>
                  <div
                    style={{
                      height: '140px',
                      background: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      marginBottom: '1rem',
                    }}
                  >
                    <PackageCheck size={42} style={{ color: 'var(--secondary)' }} />
                  </div>
                  <Link
                    to={`/products/${item.productId}`}
                    style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-main)', display: 'block', marginBottom: '0.5rem' }}
                  >
                    {item.productName}
                  </Link>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    {formatCurrency(item.productPrice)}
                  </div>
                  <div
                    style={{
                      fontSize: '0.75rem',
                      color: inStock ? 'var(--success)' : 'var(--danger)',
                      fontWeight: 600,
                      marginTop: '0.25rem',
                    }}
                  >
                    {inStock ? 'Available in Catalog' : 'Currently Unavailable'}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.5rem' }}>
                  <button
                    type="button"
                    className="btn-add-cart"
                    disabled={isMoving || isRemoving || !inStock}
                    onClick={() => handleMoveToCart(item.id, item.productId)}
                    style={{ flex: 1, opacity: inStock ? 1 : 0.6 }}
                  >
                    {isMoving ? (
                      <>
                        <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} /> Moving...
                      </>
                    ) : (
                      <>
                        <ShoppingCart size={16} /> Move to Cart
                      </>
                    )}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={isMoving || isRemoving}
                    onClick={() => handleRemove(item.id)}
                    aria-label="Remove item from wishlist"
                    style={{ padding: '0.5rem 0.75rem' }}
                  >
                    {isRemoving ? (
                      <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
                    ) : (
                      <Trash2 size={16} style={{ color: 'var(--danger)' }} />
                    )}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default WishlistPage;
