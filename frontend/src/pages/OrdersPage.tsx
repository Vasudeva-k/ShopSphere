import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Package,
  CheckCircle,
  Clock,
  AlertCircle,
  Loader2,
  XCircle,
  Truck,
  ArrowRight,
  RotateCcw,
} from 'lucide-react';
import { orderService } from '../services/orderService';
import { formatCurrency } from '../utils/currency';
import type { OrderResponse, OrderStatus, PageResponse } from '../types';
import type { AxiosError } from 'axios';

export const OrdersPage: React.FC = () => {
  const [ordersPage, setOrdersPage] = useState<PageResponse<OrderResponse> | null>(null);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [cancellingOrderId, setCancellingOrderId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const fetchOrders = useCallback(async (page: number) => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await orderService.getOrders({ page, size: 10 });
      setOrdersPage(data);
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      setErrorMessage(
        axiosError.response?.data?.message ||
          axiosError.response?.data?.error ||
          'Failed to load orders. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    orderService
      .getOrders({ page: currentPage, size: 10 })
      .then((data) => {
        if (isMounted) {
          setOrdersPage(data);
        }
      })
      .catch((err: AxiosError<{ message?: string; error?: string }>) => {
        if (isMounted) {
          setErrorMessage(
            err.response?.data?.message ||
              err.response?.data?.error ||
              'Failed to load orders. Please try again.'
          );
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [currentPage]);

  const handleCancelOrder = async (orderId: number) => {
    if (!window.confirm(`Are you sure you want to cancel Order #${orderId}?`)) {
      return;
    }

    setCancellingOrderId(orderId);
    setErrorMessage(null);
    setActionSuccess(null);

    try {
      await orderService.cancelOrder(orderId);
      setActionSuccess(`Order #${orderId} has been successfully cancelled.`);
      await fetchOrders(currentPage);
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      setErrorMessage(
        axiosError.response?.data?.message ||
          axiosError.response?.data?.error ||
          `Unable to cancel Order #${orderId}. It may have already been processed.`
      );
    } finally {
      setCancellingOrderId(null);
    }
  };

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'DELIVERED':
        return (
          <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle size={13} /> Delivered
          </span>
        );
      case 'SHIPPED':
        return (
          <span className="badge" style={{ backgroundColor: '#e0f2fe', color: '#0369a1', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Truck size={13} /> Shipped
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="badge badge-warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> Processing
          </span>
        );
      case 'CONFIRMED':
        return (
          <span className="badge" style={{ backgroundColor: '#e0e7ff', color: '#4338ca', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle size={13} /> Confirmed
          </span>
        );
      case 'PENDING':
        return (
          <span className="badge" style={{ backgroundColor: '#fef3c7', color: '#92400e', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> Pending
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="badge" style={{ backgroundColor: '#fee2e2', color: '#991b1b', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <XCircle size={13} /> Cancelled
          </span>
        );
      default:
        return <span className="badge">{status}</span>;
    }
  };

  const orders = ordersPage?.content || [];

  return (
    <div className="container" style={{ paddingTop: '2rem', maxWidth: '1080px' }}>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Package size={28} /> Your Orders & Purchase History
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Track active shipments, view invoices, or re-order your favorite products
          </p>
        </div>
      </div>

      {actionSuccess && (
        <div
          style={{
            background: '#ecfdf5',
            border: '1px solid #a7f3d0',
            color: '#065f46',
            borderRadius: 'var(--radius-md)',
            padding: '0.875rem 1.25rem',
            marginBottom: '1.5rem',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <CheckCircle size={18} /> {actionSuccess}
        </div>
      )}

      {errorMessage && (
        <div
          style={{
            background: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#991b1b',
            borderRadius: 'var(--radius-md)',
            padding: '0.875rem 1.25rem',
            marginBottom: '1.5rem',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <AlertCircle size={18} /> {errorMessage}
        </div>
      )}

      {isLoading && (
        <div style={{ textAlign: 'center', padding: '4rem 0', color: 'var(--text-muted)' }}>
          <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
          <p style={{ fontSize: '1rem', fontWeight: 500 }}>Loading your order history...</p>
        </div>
      )}

      {!isLoading && orders.length === 0 && (
        <div className="placeholder-card">
          <Package className="placeholder-icon" size={48} />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>
            No Orders Placed Yet
          </h3>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
            Looks like you haven't placed any orders yet. Discover great items from our catalog.
          </p>
          <Link to="/products" className="btn btn-primary">
            Start Shopping
          </Link>
        </div>
      )}

      {!isLoading && orders.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {orders.map((order) => {
            const isCancelling = cancellingOrderId === order.id;
            const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED';

            return (
              <div
                key={order.id}
                style={{
                  background: 'var(--bg-surface)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-lg)',
                  overflow: 'hidden',
                  boxShadow: 'var(--shadow-xs)',
                  opacity: isCancelling ? 0.6 : 1,
                }}
              >
                {/* Order Header */}
                <div
                  style={{
                    background: '#f8fafc',
                    padding: '1rem 1.5rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    gap: '1rem',
                    borderBottom: '1px solid var(--border-color)',
                    fontSize: '0.875rem',
                  }}
                >
                  <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
                    <div>
                      <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>
                        ORDER PLACED
                      </span>
                      <span style={{ fontWeight: 600 }}>
                        {new Date(order.createdAt).toLocaleDateString(undefined, {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                        })}
                      </span>
                    </div>
                    <div>
                      <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>
                        TOTAL
                      </span>
                      <span style={{ fontWeight: 700, color: 'var(--text-main)' }}>
                        {formatCurrency(order.totalAmount)}
                      </span>
                    </div>
                    <div>
                      <span style={{ color: 'var(--text-muted)', display: 'block', fontSize: '0.75rem', fontWeight: 600 }}>
                        STATUS
                      </span>
                      <div>{getStatusBadge(order.status)}</div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                      ORDER # <strong>{order.id}</strong>
                    </span>
                    <Link
                      to={`/orders/${order.id}`}
                      className="btn btn-outline"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.8125rem' }}
                    >
                      View Details
                    </Link>
                  </div>
                </div>

                {/* Order Body Items */}
                <div style={{ padding: '1.25rem 1.5rem' }}>
                  {order.items?.map((item) => (
                    <div
                      key={item.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '0.875rem 0',
                        borderBottom: '1px solid #f1f5f9',
                        gap: '1rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        <div
                          style={{
                            width: '48px',
                            height: '48px',
                            background: '#f8fafc',
                            borderRadius: 'var(--radius-md)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            border: '1px solid var(--border-color)',
                            flexShrink: 0,
                          }}
                        >
                          <Package size={22} style={{ color: 'var(--secondary)' }} />
                        </div>
                        <div>
                          {item.productId ? (
                            <Link
                              to={`/products/${item.productId}`}
                              style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--text-main)' }}
                            >
                              {item.productName}
                            </Link>
                          ) : (
                            <span style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--text-main)' }}>
                              {item.productName}
                            </span>
                          )}
                          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                            Qty: {item.quantity} · {formatCurrency(item.unitPrice)} each
                          </div>
                        </div>
                      </div>

                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontWeight: 700, fontSize: '0.9375rem' }}>
                          {formatCurrency(item.subtotal)}
                        </div>
                        {item.productId && (
                          <Link
                            to={`/products/${item.productId}`}
                            style={{
                              fontSize: '0.75rem',
                              color: 'var(--secondary)',
                              fontWeight: 600,
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '2px',
                              marginTop: '0.25rem',
                            }}
                          >
                            Buy Again <ArrowRight size={12} />
                          </Link>
                        )}
                      </div>
                    </div>
                  ))}

                  {/* Order Footer Actions */}
                  {canCancel && (
                    <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        onClick={() => handleCancelOrder(order.id)}
                        disabled={isCancelling}
                        className="btn btn-outline"
                        style={{
                          color: 'var(--danger)',
                          borderColor: '#fecaca',
                          fontSize: '0.8125rem',
                          padding: '0.4rem 0.85rem',
                        }}
                      >
                        {isCancelling ? (
                          <>
                            <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} /> Cancelling...
                          </>
                        ) : (
                          <>
                            <RotateCcw size={14} /> Cancel Order
                          </>
                        )}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {/* Pagination Controls */}
          {ordersPage && ordersPage.totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '1rem', marginTop: '1.5rem' }}>
              <button
                type="button"
                className="btn btn-outline"
                disabled={ordersPage.first || isLoading}
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
              >
                Previous
              </button>
              <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                Page {ordersPage.number + 1} of {ordersPage.totalPages}
              </span>
              <button
                type="button"
                className="btn btn-outline"
                disabled={ordersPage.last || isLoading}
                onClick={() => setCurrentPage((prev) => prev + 1)}
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default OrdersPage;
