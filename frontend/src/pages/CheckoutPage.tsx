import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  CreditCard,
  MapPin,
  Loader2,
  AlertCircle,
  PackageCheck,
  ShoppingCart,
  CheckCircle2,
  XCircle,
  ArrowRight,
  RefreshCw,
} from 'lucide-react';
import { useCart } from '../hooks/useCart';
import { useAuth } from '../hooks/useAuth';
import { orderService } from '../services/orderService';
import { paymentService } from '../services/paymentService';
import type { OrderResponse, PaymentResponse } from '../types';
import type { AxiosError } from 'axios';

type CheckoutStep = 'REVIEW' | 'PAYMENT' | 'RESULT';

export const CheckoutPage: React.FC = () => {
  const { cart, fetchCart, isLoading: isCartLoading } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState<CheckoutStep>('REVIEW');
  const [createdOrder, setCreatedOrder] = useState<OrderResponse | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);

  const [isPlacingOrder, setIsPlacingOrder] = useState(false);
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const items = cart?.items || [];
  const subtotal = Number(cart?.total ?? 0);
  const estimatedTax = subtotal > 0 ? subtotal * 0.08 : 0;
  const shipping = subtotal > 50 || subtotal === 0 ? 0 : 9.99;
  const grandTotal = subtotal + estimatedTax + shipping;

  /**
   * Step 1: Create the order from the cart, then immediately create a PENDING payment.
   */
  const handlePlaceOrder = async () => {
    if (items.length === 0 || isPlacingOrder) return;

    setIsPlacingOrder(true);
    setErrorMessage(null);

    try {
      // 1. Create order on backend (authoritative stock validation & cart clearance)
      const order = await orderService.createOrder();
      setCreatedOrder(order);

      // 2. Synchronize active cart state in React context
      await fetchCart();

      // 3. Initiate payment for this order (authoritative amount determined by backend)
      const initialPayment = await paymentService.createPayment(order.id);
      setPayment(initialPayment);

      // 4. Advance to payment simulation step
      setStep('PAYMENT');
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      const message =
        axiosError.response?.data?.message ||
        axiosError.response?.data?.error ||
        'Unable to complete your request at this time. Please check your cart and try again.';
      setErrorMessage(message);
    } finally {
      setIsPlacingOrder(false);
    }
  };

  /**
   * Retry payment creation if order was created but payment creation failed initially.
   */
  const handleRetryPaymentCreation = async () => {
    if (!createdOrder || isPlacingOrder) return;

    setIsPlacingOrder(true);
    setErrorMessage(null);

    try {
      const initialPayment = await paymentService.createPayment(createdOrder.id);
      setPayment(initialPayment);
      setStep('PAYMENT');
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      const message =
        axiosError.response?.data?.message ||
        axiosError.response?.data?.error ||
        'Unable to initialize payment for this order.';
      setErrorMessage(message);
    } finally {
      setIsPlacingOrder(false);
    }
  };

  /**
   * Step 2: Process payment simulation (Success or Failure).
   */
  const handleProcessPayment = async (simulateSuccess: boolean) => {
    if (!payment || isProcessingPayment) return;

    setIsProcessingPayment(true);
    setErrorMessage(null);

    try {
      const processedPayment = await paymentService.processPayment(payment.id, simulateSuccess);
      setPayment(processedPayment);
      setStep('RESULT');
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; error?: string }>;
      const message =
        axiosError.response?.data?.message ||
        axiosError.response?.data?.error ||
        'Payment processing encountered an error. Please try again.';
      setErrorMessage(message);
    } finally {
      setIsProcessingPayment(false);
    }
  };

  // Loading state when initial cart is fetching
  if (isCartLoading && !cart && step === 'REVIEW') {
    return (
      <div className="container" style={{ paddingTop: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
        <p style={{ fontSize: '1rem', fontWeight: 500 }}>Loading checkout details...</p>
      </div>
    );
  }

  // Cart is empty and no order was placed in this session
  if (items.length === 0 && step === 'REVIEW' && !createdOrder) {
    return (
      <div className="container" style={{ paddingTop: '3rem', maxWidth: '600px', textAlign: 'center' }}>
        <div className="placeholder-card">
          <ShoppingCart className="placeholder-icon" size={48} />
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>
            Your Cart is Empty
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
            You need at least one item in your shopping cart before proceeding to checkout.
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
            <Link to="/products" className="btn btn-primary">
              Browse Products
            </Link>
            <Link to="/cart" className="btn btn-outline">
              View Cart
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // ==========================================
  // STEP 3: RESULT VIEW (SUCCESS OR FAILED)
  // ==========================================
  if (step === 'RESULT' && payment && createdOrder) {
    const isSuccess = payment.status === 'SUCCESS';

    return (
      <div className="container" style={{ paddingTop: '3rem', maxWidth: '720px' }}>
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '2.5rem',
            boxShadow: 'var(--shadow-md)',
            textAlign: 'center',
          }}
        >
          {isSuccess ? (
            <>
              <div
                style={{
                  width: '64px',
                  height: '64px',
                  borderRadius: '50%',
                  background: '#ecfdf5',
                  color: 'var(--success)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 1.25rem',
                }}
              >
                <CheckCircle2 size={38} />
              </div>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
                Payment Successful!
              </h1>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem', marginBottom: '2rem' }}>
                Your payment has been authorized and Order #{createdOrder.id} is now confirmed.
              </p>
            </>
          ) : (
            <>
              <div
                style={{
                  width: '64px',
                  height: '64px',
                  borderRadius: '50%',
                  background: '#fef2f2',
                  color: 'var(--danger)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 1.25rem',
                }}
              >
                <XCircle size={38} />
              </div>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
                Simulated Payment Declined
              </h1>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem', marginBottom: '2rem' }}>
                The payment simulation returned a FAILED response. No funds were charged. Your order remains saved in PENDING status.
              </p>
            </>
          )}

          {/* Payment / Order Details Summary */}
          <div
            style={{
              background: '#f8fafc',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-md)',
              padding: '1.5rem',
              textAlign: 'left',
              marginBottom: '2rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.75rem',
              fontSize: '0.9375rem',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Order ID:</span>
              <strong style={{ color: 'var(--text-main)' }}>#{createdOrder.id}</strong>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Payment Amount:</span>
              <strong style={{ color: 'var(--text-main)' }}>${Number(payment.amount).toFixed(2)}</strong>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)' }}>Payment Status:</span>
              <span
                className={`badge ${isSuccess ? 'badge-success' : 'badge-danger'}`}
                style={{
                  backgroundColor: isSuccess ? '#ecfdf5' : '#fee2e2',
                  color: isSuccess ? '#065f46' : '#991b1b',
                  fontWeight: 700,
                  fontSize: '0.8125rem',
                  padding: '0.2rem 0.6rem',
                  borderRadius: 'var(--radius-sm)',
                }}
              >
                {payment.status}
              </span>
            </div>

            {payment.transactionId && (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-muted)' }}>Transaction Reference:</span>
                <code style={{ fontSize: '0.8125rem', background: '#ffffff', padding: '2px 6px', borderRadius: '4px', border: '1px solid var(--border-color)' }}>
                  {payment.transactionId}
                </code>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Simulated Gateway Mode:</span>
              <span style={{ color: 'var(--secondary)', fontWeight: 600 }}>Backend Simulation API</span>
            </div>
          </div>

          {/* Action Navigation Buttons */}
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => navigate(`/orders/${createdOrder.id}`, { state: { payment, orderCreated: isSuccess } })}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600 }}
            >
              View Order Details <ArrowRight size={16} />
            </button>
            <Link to="/products" className="btn btn-outline" style={{ fontWeight: 600 }}>
              Continue Shopping
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // ==========================================
  // STEP 2: PAYMENT SIMULATION VIEW
  // ==========================================
  if (step === 'PAYMENT' && payment && createdOrder) {
    return (
      <div className="container" style={{ paddingTop: '2.5rem', maxWidth: '760px' }}>
        <div className="page-header" style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              color: 'var(--secondary)',
              fontWeight: 700,
              fontSize: '0.875rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
              marginBottom: '0.5rem',
            }}
          >
            <CreditCard size={18} /> Step 2: Payment Simulation
          </div>
          <h1 className="page-title" style={{ fontSize: '1.75rem', fontWeight: 800 }}>
            Simulated Payment Gateway
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem', marginTop: '0.25rem' }}>
            Choose a simulation action below to complete your order checkout
          </p>
        </div>

        {errorMessage && (
          <div
            style={{
              background: '#fef2f2',
              border: '1px solid #fecaca',
              color: '#991b1b',
              borderRadius: 'var(--radius-md)',
              padding: '1rem 1.25rem',
              marginBottom: '1.5rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
            }}
          >
            <AlertCircle size={20} style={{ flexShrink: 0 }} />
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>{errorMessage}</span>
          </div>
        )}

        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '2rem',
            boxShadow: 'var(--shadow-sm)',
            marginBottom: '1.5rem',
          }}
        >
          {/* Order / Payment Summary Bar */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1rem',
              padding: '1.25rem',
              background: '#f8fafc',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-md)',
              marginBottom: '1.75rem',
            }}
          >
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                Order Number
              </div>
              <div style={{ fontSize: '1.125rem', fontWeight: 800, color: 'var(--text-main)', marginTop: '0.25rem' }}>
                #{createdOrder.id}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                Payment Status
              </div>
              <div style={{ marginTop: '0.25rem' }}>
                <span
                  style={{
                    backgroundColor: '#fef3c7',
                    color: '#92400e',
                    fontSize: '0.8125rem',
                    fontWeight: 700,
                    padding: '0.2rem 0.6rem',
                    borderRadius: 'var(--radius-sm)',
                    display: 'inline-block',
                  }}
                >
                  {payment.status}
                </span>
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                Authoritative Total Amount
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)', marginTop: '0.25rem' }}>
                ${Number(payment.amount).toFixed(2)}
              </div>
            </div>
          </div>

          <div
            style={{
              background: '#f0f9ff',
              border: '1px solid #bae6fd',
              color: '#0369a1',
              borderRadius: 'var(--radius-md)',
              padding: '1rem 1.25rem',
              fontSize: '0.875rem',
              lineHeight: 1.5,
              marginBottom: '2rem',
            }}
          >
            <strong>Simulated Payment Environment:</strong> No actual monetary transaction will occur.
            Select <strong>Simulate Successful Payment</strong> to generate a gateway transaction ID and transition the order to <strong>CONFIRMED</strong>, or select <strong>Simulate Failed Payment</strong> to test gateway decline handling.
          </div>

          {/* Simulation Action Controls */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <button
              type="button"
              className="btn btn-primary"
              disabled={isProcessingPayment}
              onClick={() => handleProcessPayment(true)}
              style={{
                width: '100%',
                padding: '1rem',
                fontSize: '1rem',
                fontWeight: 700,
                backgroundColor: 'var(--brand-accent)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.6rem',
                cursor: isProcessingPayment ? 'not-allowed' : 'pointer',
                opacity: isProcessingPayment ? 0.7 : 1,
              }}
            >
              {isProcessingPayment ? (
                <>
                  <Loader2 size={20} style={{ animation: 'spin 1s linear infinite' }} /> Processing Simulation...
                </>
              ) : (
                <>
                  <CheckCircle2 size={20} /> Simulate Successful Payment (${Number(payment.amount).toFixed(2)})
                </>
              )}
            </button>

            <button
              type="button"
              className="btn btn-outline"
              disabled={isProcessingPayment}
              onClick={() => handleProcessPayment(false)}
              style={{
                width: '100%',
                padding: '0.85rem',
                fontSize: '0.9375rem',
                fontWeight: 600,
                color: 'var(--danger)',
                borderColor: '#fecaca',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                cursor: isProcessingPayment ? 'not-allowed' : 'pointer',
                opacity: isProcessingPayment ? 0.7 : 1,
              }}
            >
              <XCircle size={18} /> Simulate Failed / Declined Payment
            </button>
          </div>
        </div>

        <div style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
          <ShieldCheck size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '4px' }} />
          Authoritative amount computed by backend Spring Boot service
        </div>
      </div>
    );
  }

  // ==========================================
  // STEP 1: ORDER REVIEW & PLACEMENT VIEW
  // ==========================================
  return (
    <div className="container" style={{ paddingTop: '2rem', maxWidth: '1080px' }}>
      <div className="page-header">
        <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <ShieldCheck size={28} style={{ color: 'var(--success)' }} /> Checkout & Order Review
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
          Review your items, destination, and proceed to payment simulation
        </p>
      </div>

      {errorMessage && (
        <div
          style={{
            background: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#991b1b',
            borderRadius: 'var(--radius-md)',
            padding: '1rem 1.25rem',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '0.75rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertCircle size={20} style={{ flexShrink: 0 }} />
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>{errorMessage}</span>
          </div>
          {createdOrder && !payment && (
            <button
              type="button"
              className="btn btn-outline"
              onClick={handleRetryPaymentCreation}
              disabled={isPlacingOrder}
              style={{ fontSize: '0.8125rem', padding: '0.35rem 0.75rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
            >
              <RefreshCw size={14} /> Retry Payment Initiation
            </button>
          )}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: '2rem', alignItems: 'start' }}>
        {/* Left Column: Order Sections */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Shipping Destination */}
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
                fontSize: '1.125rem',
                fontWeight: 700,
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <MapPin size={20} color="var(--secondary)" /> 1. Shipping Destination
            </h3>
            <div
              style={{
                background: '#f8fafc',
                padding: '1rem 1.25rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                color: 'var(--text-main)',
                lineHeight: 1.6,
              }}
            >
              <div style={{ fontWeight: 700 }}>
                {user ? `${user.firstName} ${user.lastName}` : 'ShopSphere Customer'}
              </div>
              <div>Primary Account Address / Standard Express Delivery</div>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.8125rem', marginTop: '0.25rem' }}>
                Estimated delivery: 2-3 business days
              </div>
            </div>
          </div>

          {/* Payment Method Details */}
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
                fontSize: '1.125rem',
                fontWeight: 700,
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <CreditCard size={20} color="var(--brand-accent)" /> 2. Payment Method
            </h3>
            <div
              style={{
                background: '#f8fafc',
                padding: '1rem 1.25rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                color: 'var(--text-main)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <div style={{ fontWeight: 600 }}>ShopSphere Payment Gateway Simulation</div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  Authoritative amount calculated upon order creation. Supports simulated approval or decline.
                </div>
              </div>
              <CheckCircle2 size={20} color="var(--success)" />
            </div>
          </div>

          {/* Items Review */}
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
                fontSize: '1.125rem',
                fontWeight: 700,
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <PackageCheck size={20} color="var(--primary)" /> 3. Review Items ({items.reduce((s, it) => s + it.quantity, 0)})
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column' }}>
              {items.map((item) => (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '1rem 0',
                    borderBottom: '1px solid var(--border-color)',
                    gap: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <div
                      style={{
                        width: '50px',
                        height: '50px',
                        background: '#f8fafc',
                        borderRadius: 'var(--radius-md)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: '1px solid var(--border-color)',
                        flexShrink: 0,
                      }}
                    >
                      <PackageCheck size={24} style={{ color: 'var(--secondary)' }} />
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--text-main)' }}>
                        {item.productName}
                      </div>
                      <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                        Qty: {item.quantity} × ${Number(item.unitPrice).toFixed(2)}
                      </div>
                    </div>
                  </div>

                  <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-main)', textAlign: 'right' }}>
                    ${Number(item.subtotal).toFixed(2)}
                  </div>
                </div>
              ))}
            </div>

            <div style={{ marginTop: '1rem', textAlign: 'right' }}>
              <Link to="/cart" style={{ fontSize: '0.8125rem', color: 'var(--secondary)', fontWeight: 600 }}>
                Edit Cart Items
              </Link>
            </div>
          </div>
        </div>

        {/* Right Column: Order Summary & Place Order */}
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
            boxShadow: 'var(--shadow-sm)',
            position: 'sticky',
            top: '2rem',
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
              <span>Items ({items.reduce((s, it) => s + it.quantity, 0)}):</span>
              <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>${subtotal.toFixed(2)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span>Shipping:</span>
              <span style={{ color: shipping === 0 ? 'var(--success)' : 'var(--text-main)', fontWeight: 600 }}>
                {shipping === 0 ? 'FREE' : `$${shipping.toFixed(2)}`}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span>Estimated Tax (8%):</span>
              <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>${estimatedTax.toFixed(2)}</span>
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
              <span>Estimated Total:</span>
              <span style={{ color: 'var(--text-main)' }}>${grandTotal.toFixed(2)}</span>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-primary"
            onClick={handlePlaceOrder}
            disabled={isPlacingOrder || items.length === 0}
            style={{
              width: '100%',
              backgroundColor: 'var(--brand-accent)',
              color: 'var(--primary)',
              fontWeight: 700,
              padding: '0.85rem',
              marginBottom: '1rem',
              fontSize: '1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              cursor: isPlacingOrder || items.length === 0 ? 'not-allowed' : 'pointer',
              opacity: isPlacingOrder ? 0.7 : 1,
            }}
          >
            {isPlacingOrder ? (
              <>
                <Loader2 size={18} style={{ animation: 'spin 1s linear infinite' }} /> Placing Order...
              </>
            ) : (
              'Place Order & Proceed to Payment'
            )}
          </button>

          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textAlign: 'center', lineHeight: 1.4 }}>
            By placing your order, you agree to ShopSphere's Conditions of Use and simulated payment flow.
          </p>

          <div
            style={{
              marginTop: '1.25rem',
              paddingTop: '1rem',
              borderTop: '1px solid var(--border-color)',
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
    </div>
  );
};

export default CheckoutPage;
