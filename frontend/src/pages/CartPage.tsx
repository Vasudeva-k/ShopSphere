import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ShoppingCart,
  Trash2,
  ArrowRight,
  ShieldCheck,
  Loader2,
  PackageCheck,
  RotateCcw,
} from 'lucide-react';
import { useCart } from '../hooks/useCart';
import { formatCurrency } from '../utils/currency';

export const CartPage: React.FC = () => {
  const { cart, updateQuantity, removeItem, clearCart, isLoading } = useCart();
  const [mutatingItemId, setMutatingItemId] = useState<number | null>(null);
  const [isClearing, setIsClearing] = useState<boolean>(false);

  const items = cart?.items || [];
  const subtotal = Number(cart?.total ?? 0);
  const estimatedTax = subtotal > 0 ? subtotal * 0.08 : 0;
  const shipping = subtotal > 50 || subtotal === 0 ? 0 : 9.99;
  const grandTotal = subtotal + estimatedTax + shipping;

  const handleUpdateQty = async (itemId: number, currentQty: number, delta: number) => {
    const nextQty = currentQty + delta;
    if (nextQty < 1) return;
    setMutatingItemId(itemId);
    try {
      await updateQuantity(itemId, nextQty);
    } catch {
      // Ignored gracefully
    } finally {
      setMutatingItemId(null);
    }
  };

  const handleRemove = async (itemId: number) => {
    setMutatingItemId(itemId);
    try {
      await removeItem(itemId);
    } catch {
      // Ignored gracefully
    } finally {
      setMutatingItemId(null);
    }
  };

  const handleClearCart = async () => {
    if (!window.confirm('Are you sure you want to clear your shopping cart?')) return;
    setIsClearing(true);
    try {
      await clearCart();
    } catch {
      // Ignored gracefully
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
          <ShoppingCart size={28} /> Shopping Cart
        </h1>
        {items.length > 0 && (
          <button
            type="button"
            className="btn btn-outline"
            onClick={handleClearCart}
            disabled={isClearing || isLoading}
            style={{ color: 'var(--danger)', borderColor: '#fecaca', fontSize: '0.8125rem' }}
          >
            {isClearing ? (
              <>
                <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} /> Clearing...
              </>
            ) : (
              <>
                <RotateCcw size={14} /> Clear Cart
              </>
            )}
          </button>
        )}
      </div>

      {isLoading && !cart && (
        <div style={{ textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)' }}>
          <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
          <p style={{ fontSize: '1rem', fontWeight: 500 }}>Loading your shopping cart...</p>
        </div>
      )}

      {!isLoading && items.length === 0 ? (
        <div className="placeholder-card">
          <ShoppingCart className="placeholder-icon" size={48} />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>
            Your ShopSphere Cart is Empty
          </h3>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
            Explore our catalog and discover amazing products and deals to fill it up.
          </p>
          <Link to="/products" className="btn btn-primary">
            Start Shopping
          </Link>
        </div>
      ) : (
        items.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '2rem', alignItems: 'start' }}>
            {/* Items List */}
            <div
              style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-lg)',
                padding: '1.5rem',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  paddingBottom: '1rem',
                  borderBottom: '1px solid var(--border-color)',
                  color: 'var(--text-muted)',
                  fontSize: '0.875rem',
                }}
              >
                <span>Product Details</span>
                <span>Subtotal</span>
              </div>

              {items.map((item) => {
                const isMutating = mutatingItemId === item.id;
                return (
                  <div
                    key={item.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '1.25rem 0',
                      borderBottom: '1px solid var(--border-color)',
                      gap: '1rem',
                      opacity: isMutating ? 0.6 : 1,
                    }}
                  >
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                      <div
                        style={{
                          width: '70px',
                          height: '70px',
                          background: '#f8fafc',
                          borderRadius: 'var(--radius-md)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          border: '1px solid var(--border-color)',
                        }}
                      >
                        <PackageCheck size={32} style={{ color: 'var(--secondary)' }} />
                      </div>
                      <div>
                        <Link
                          to={`/products/${item.productId}`}
                          style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--text-main)' }}
                        >
                          {item.productName}
                        </Link>
                        <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                          Unit Price: {formatCurrency(item.unitPrice)}
                        </div>

                        {/* Quantity controls */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem' }}>
                          <div
                            style={{
                              display: 'inline-flex',
                              border: '1px solid var(--border-color)',
                              borderRadius: 'var(--radius-sm)',
                            }}
                          >
                            <button
                              type="button"
                              disabled={item.quantity <= 1 || isMutating}
                              onClick={() => handleUpdateQty(item.id, item.quantity, -1)}
                              aria-label="Decrease quantity"
                              style={{
                                padding: '0.2rem 0.6rem',
                                background: '#f8fafc',
                                border: 'none',
                                cursor: item.quantity <= 1 || isMutating ? 'not-allowed' : 'pointer',
                              }}
                            >
                              -
                            </button>
                            <span
                              style={{
                                padding: '0.2rem 0.75rem',
                                fontSize: '0.875rem',
                                fontWeight: 600,
                                minWidth: '32px',
                                textAlign: 'center',
                              }}
                            >
                              {item.quantity}
                            </span>
                            <button
                              type="button"
                              disabled={isMutating}
                              onClick={() => handleUpdateQty(item.id, item.quantity, 1)}
                              aria-label="Increase quantity"
                              style={{
                                padding: '0.2rem 0.6rem',
                                background: '#f8fafc',
                                border: 'none',
                                cursor: isMutating ? 'not-allowed' : 'pointer',
                              }}
                            >
                              +
                            </button>
                          </div>

                          <button
                            type="button"
                            disabled={isMutating}
                            onClick={() => handleRemove(item.id)}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: '0.25rem',
                              color: 'var(--danger)',
                              background: 'none',
                              border: 'none',
                              fontSize: '0.8125rem',
                              cursor: isMutating ? 'not-allowed' : 'pointer',
                              marginLeft: '0.5rem',
                            }}
                          >
                            <Trash2 size={14} /> Remove
                          </button>
                        </div>
                      </div>
                    </div>

                    <div style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-main)' }}>
                      {formatCurrency(item.subtotal)}
                    </div>
                  </div>
                );
              })}

              <div style={{ textAlign: 'right', marginTop: '1.25rem', fontSize: '1.125rem', fontWeight: 700 }}>
                Subtotal ({items.reduce((sum, it) => sum + it.quantity, 0)} items): {formatCurrency(subtotal)}
              </div>
            </div>

            {/* Order Summary Box */}
            <div
              style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-lg)',
                padding: '1.5rem',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              <h3 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '1.25rem' }}>Order Summary</h3>

              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.75rem',
                  fontSize: '0.875rem',
                  marginBottom: '1.25rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Items Subtotal:</span>
                  <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>{formatCurrency(subtotal)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Shipping:</span>
                  <span style={{ color: shipping === 0 ? 'var(--success)' : 'var(--text-main)', fontWeight: 600 }}>
                    {shipping === 0 ? 'FREE' : formatCurrency(shipping)}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Estimated Sales Tax (8%):</span>
                  <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>{formatCurrency(estimatedTax)}</span>
                </div>
                <hr style={{ border: 'none', borderTop: '1px solid var(--border-color)', margin: '0.5rem 0' }} />
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: '1.125rem',
                    fontWeight: 800,
                  }}
                >
                  <span>Order Total:</span>
                  <span style={{ color: 'var(--text-main)' }}>{formatCurrency(grandTotal)}</span>
                </div>
              </div>

              <Link
                to="/checkout"
                className="btn btn-primary"
                style={{
                  width: '100%',
                  backgroundColor: 'var(--brand-accent)',
                  color: 'var(--primary)',
                  fontWeight: 700,
                  padding: '0.75rem',
                  marginBottom: '1rem',
                  textAlign: 'center',
                }}
              >
                Proceed to Checkout <ArrowRight size={16} />
              </Link>

              <div
                style={{
                  fontSize: '0.8125rem',
                  color: 'var(--text-muted)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  justifyContent: 'center',
                }}
              >
                <ShieldCheck size={16} color="var(--success)" /> 256-Bit SSL Encrypted Checkout
              </div>
            </div>
          </div>
        )
      )}
    </div>
  );
};

export default CartPage;
