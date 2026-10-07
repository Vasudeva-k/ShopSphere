import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import {
  ArrowLeft,
  Package,
  CheckCircle,
  Clock,
  XCircle,
  Truck,
  AlertCircle,
  Loader2,
  FileText,
  RotateCcw,
  ShieldCheck,
  CheckCircle2,
  CreditCard,
  RefreshCcw,
} from 'lucide-react';
import { orderService } from '../services/orderService';
import { paymentService } from '../services/paymentService';
import type { OrderResponse, OrderStatus, PaymentResponse, PaymentStatus } from '../types';
import type { AxiosError } from 'axios';

interface LocationState {
  orderCreated?: boolean;
  payment?: PaymentResponse;
}

export const OrderDetailPage: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const location = useLocation();
  const locationState = location.state as LocationState | null;

  const numericOrderId = Number(orderId);
  const isValidId = Boolean(numericOrderId && !isNaN(numericOrderId));

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(
    locationState?.payment && locationState.payment.orderId === numericOrderId
      ? locationState.payment
      : null
  );
  const [isLoading, setIsLoading] = useState<boolean>(isValidId);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);
  const [isRefunding, setIsRefunding] = useState<boolean>(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(
    isValidId ? null : 'Invalid order ID provided.'
  );
  const [successMessage, setSuccessMessage] = useState<string | null>(
    locationState?.orderCreated
      ? 'Your order has been placed and payment simulation processed successfully!'
      : null
  );

  useEffect(() => {
    if (!isValidId) {
      return;
    }

    let isMounted = true;

    orderService
      .getOrderById(numericOrderId)
      .then((data) => {
        if (isMounted) {
          setOrder(data);
        }
      })
      .catch((err: AxiosError<{ message?: string; error?: string }>) => {
        if (isMounted) {
          setErrorMessage(
            err.response?.data?.message ||
              err.response?.data?.error ||
              'Unable to load order details. The order may not exist or you may not have permission to view it.'
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
  }, [numericOrderId, isValidId]);

  const handleCancelOrder = async () => {
    if (!order || isCancelling) return;
    if (!window.confirm(`Are you sure you want to cancel Order #${order.id}?`)) {
      return;
    }

    setIsCancelling(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const updatedOrder = await orderService.cancelOrder(order.id);
      setOrder(updatedOrder);
      setSuccessMessage(`Order #${order.id} has been cancelled successfully.`);
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      setErrorMessage(
        axiosError.response?.data?.message ||
          axiosError.response?.data?.error ||
          'Unable to cancel this order.'
      );
    } finally {
      setIsCancelling(false);
    }
  };

  /**
   * Handle payment refund when reliable payment ID is available from current flow.
   * Only SUCCESS payments can be refunded.
   */
  const handleRefundPayment = async () => {
    if (!payment || payment.status !== 'SUCCESS' || isRefunding || !order) return;

    const confirmed = window.confirm(
      `Are you sure you want to refund Payment #${payment.id} for Order #${order.id}? This will cancel the order.`
    );
    if (!confirmed) return;

    setIsRefunding(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const refundedPayment = await paymentService.refundPayment(payment.id);
      setPayment(refundedPayment);

      // Re-fetch order to synchronize authoritative cancellation state from backend
      const updatedOrder = await orderService.getOrderById(order.id);
      setOrder(updatedOrder);

      setSuccessMessage(`Payment #${payment.id} has been refunded successfully. Order #${order.id} status is now CANCELLED.`);
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      setErrorMessage(
        axiosError.response?.data?.message ||
          axiosError.response?.data?.error ||
          'Unable to process refund for this payment.'
      );
    } finally {
      setIsRefunding(false);
    }
  };

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'DELIVERED':
        return (
          <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle size={14} /> Delivered
          </span>
        );
      case 'SHIPPED':
        return (
          <span className="badge" style={{ backgroundColor: '#e0f2fe', color: '#0369a1', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Truck size={14} /> Shipped
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="badge badge-warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={14} /> Processing
          </span>
        );
      case 'CONFIRMED':
        return (
          <span className="badge" style={{ backgroundColor: '#e0e7ff', color: '#4338ca', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle size={14} /> Confirmed
          </span>
        );
      case 'PENDING':
        return (
          <span className="badge" style={{ backgroundColor: '#fef3c7', color: '#92400e', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={14} /> Pending
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="badge" style={{ backgroundColor: '#fee2e2', color: '#991b1b', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <XCircle size={14} /> Cancelled
          </span>
        );
      default:
        return <span className="badge">{status}</span>;
    }
  };

  const getPaymentStatusBadge = (status: PaymentStatus) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <span className="badge" style={{ backgroundColor: '#ecfdf5', color: '#065f46', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle2 size={13} /> SUCCESS
          </span>
        );
      case 'PENDING':
        return (
          <span className="badge" style={{ backgroundColor: '#fef3c7', color: '#92400e', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> PENDING
          </span>
        );
      case 'FAILED':
        return (
          <span className="badge" style={{ backgroundColor: '#fee2e2', color: '#991b1b', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <XCircle size={13} /> FAILED
          </span>
        );
      case 'REFUNDED':
        return (
          <span className="badge" style={{ backgroundColor: '#f3e8ff', color: '#6b21a8', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <RefreshCcw size={13} /> REFUNDED
          </span>
        );
      default:
        return <span className="badge">{status}</span>;
    }
  };

  if (isLoading) {
    return (
      <div className="container" style={{ paddingTop: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
        <p style={{ fontSize: '1rem', fontWeight: 500 }}>Loading order #{orderId}...</p>
      </div>
    );
  }

  if (errorMessage && !order) {
    return (
      <div className="container" style={{ paddingTop: '3rem', maxWidth: '600px', textAlign: 'center' }}>
        <div className="placeholder-card">
          <AlertCircle className="placeholder-icon" size={48} style={{ color: 'var(--danger)' }} />
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Order Not Found</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
            {errorMessage}
          </p>
          <Link to="/orders" className="btn btn-primary">
            Back to Orders
          </Link>
        </div>
      </div>
    );
  }

  if (!order) return null;

  const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED';
  const canRefund = payment && payment.status === 'SUCCESS' && order.status !== 'CANCELLED';
  const subtotal = Number(order.totalAmount);
  const items = order.items || [];

  return (
    <div className="container" style={{ paddingTop: '2rem', maxWidth: '1080px' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link
          to="/orders"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            color: 'var(--secondary)',
            fontSize: '0.875rem',
            fontWeight: 600,
          }}
        >
          <ArrowLeft size={16} /> Back to all orders
        </Link>
      </div>

      {successMessage && (
        <div
          style={{
            background: '#ecfdf5',
            border: '1px solid #a7f3d0',
            color: '#065f46',
            borderRadius: 'var(--radius-md)',
            padding: '1rem 1.25rem',
            marginBottom: '1.5rem',
            fontSize: '0.9375rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
          }}
        >
          <CheckCircle2 size={22} style={{ flexShrink: 0 }} />
          <div>
            <strong>Success!</strong> {successMessage}
          </div>
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
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <FileText size={28} /> Order #{order.id}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Placed on{' '}
            {new Date(order.createdAt).toLocaleDateString(undefined, {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })}
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div>{getStatusBadge(order.status)}</div>

          {/* Refund Action (Available when reliable payment ID with SUCCESS status exists in current session) */}
          {canRefund && (
            <button
              type="button"
              className="btn btn-outline"
              disabled={isRefunding}
              onClick={handleRefundPayment}
              style={{
                color: '#6b21a8',
                borderColor: '#e9d5ff',
                backgroundColor: '#faf5ff',
                fontSize: '0.8125rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
              }}
            >
              {isRefunding ? (
                <>
                  <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} /> Processing Refund...
                </>
              ) : (
                <>
                  <RefreshCcw size={14} /> Simulate Refund
                </>
              )}
            </button>
          )}

          {/* Cancel Order Action */}
          {canCancel && !canRefund && (
            <button
              type="button"
              className="btn btn-outline"
              disabled={isCancelling}
              onClick={handleCancelOrder}
              style={{
                color: 'var(--danger)',
                borderColor: '#fecaca',
                fontSize: '0.8125rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
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
          )}
        </div>
      </div>

      {/* Order Meta & Summary Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '1.5rem',
          marginBottom: '2rem',
        }}
      >
        {/* Fulfillment Status */}
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
          }}
        >
          <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', color: 'var(--text-main)' }}>
            Fulfillment Status
          </h3>
          <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
            <div>
              Status: <strong style={{ color: 'var(--text-main)' }}>{order.status}</strong>
            </div>
            <div>
              Last updated:{' '}
              {new Date(order.updatedAt || order.createdAt).toLocaleDateString(undefined, {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </div>
            <div style={{ marginTop: '0.5rem', fontSize: '0.8125rem', color: 'var(--text-light)' }}>
              Standard Express Delivery
            </div>
          </div>
        </div>

        {/* Payment / Gateway Details Card */}
        {payment ? (
          <div
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-lg)',
              padding: '1.5rem',
            }}
          >
            <h3
              style={{
                fontSize: '1rem',
                fontWeight: 700,
                marginBottom: '0.75rem',
                color: 'var(--text-main)',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <CreditCard size={18} color="var(--brand-accent)" /> Payment Information
            </h3>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>Status:</span>
                {getPaymentStatusBadge(payment.status)}
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Payment ID:</span>
                <strong style={{ color: 'var(--text-main)' }}>#{payment.id}</strong>
              </div>
              {payment.transactionId && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Transaction ID:</span>
                  <code style={{ fontSize: '0.75rem', background: '#f1f5f9', padding: '1px 5px', borderRadius: '3px' }}>
                    {payment.transactionId}
                  </code>
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Mode:</span>
                <span style={{ fontSize: '0.8125rem', color: 'var(--secondary)', fontWeight: 600 }}>Simulated Gateway</span>
              </div>
            </div>
          </div>
        ) : (
          <div
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-lg)',
              padding: '1.5rem',
            }}
          >
            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', color: 'var(--text-main)' }}>
              Order Verification
            </h3>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--success)', fontWeight: 600 }}>
                <ShieldCheck size={16} /> Verified Authenticated Order
              </div>
              <div>User ID Reference: #{order.userId}</div>
              <div style={{ fontSize: '0.8125rem', marginTop: '0.25rem' }}>
                Recorded securely in the ShopSphere database.
              </div>
            </div>
          </div>
        )}

        {/* Grand Total Breakdown */}
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
          }}
        >
          <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', color: 'var(--text-main)' }}>
            Order Financial Total
          </h3>
          <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>Items Total:</span>
              <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>${subtotal.toFixed(2)}</span>
            </div>
            <hr style={{ border: 'none', borderTop: '1px solid var(--border-color)', margin: '0.35rem 0' }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.125rem', fontWeight: 800, color: 'var(--text-main)' }}>
              <span>Total Amount:</span>
              <span>${subtotal.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Items Section */}
      <div
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '1.5rem',
          boxShadow: 'var(--shadow-xs)',
        }}
      >
        <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '1.25rem' }}>
          Items in this Order ({items.reduce((sum, item) => sum + item.quantity, 0)})
        </h2>

        <div style={{ display: 'flex', flexDirection: 'column' }}>
          {items.map((item) => (
            <div
              key={item.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '1.25rem 0',
                borderBottom: '1px solid #f1f5f9',
                gap: '1rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div
                  style={{
                    width: '56px',
                    height: '56px',
                    background: '#f8fafc',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    border: '1px solid var(--border-color)',
                    flexShrink: 0,
                  }}
                >
                  <Package size={28} style={{ color: 'var(--secondary)' }} />
                </div>
                <div>
                  {item.productId ? (
                    <Link
                      to={`/products/${item.productId}`}
                      style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-main)' }}
                    >
                      {item.productName}
                    </Link>
                  ) : (
                    <span style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-main)' }}>
                      {item.productName}
                    </span>
                  )}
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    Unit Price: ${Number(item.unitPrice).toFixed(2)} × Quantity: {item.quantity}
                  </div>
                </div>
              </div>

              <div style={{ textAlign: 'right' }}>
                <div style={{ fontWeight: 800, fontSize: '1.125rem', color: 'var(--text-main)' }}>
                  ${Number(item.subtotal).toFixed(2)}
                </div>
                {item.productId && (
                  <Link
                    to={`/products/${item.productId}`}
                    className="btn btn-outline"
                    style={{
                      fontSize: '0.75rem',
                      padding: '0.25rem 0.6rem',
                      marginTop: '0.35rem',
                      display: 'inline-block',
                    }}
                  >
                    Buy Again
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default OrderDetailPage;
