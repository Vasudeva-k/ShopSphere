import apiClient from './api';
import type { PaymentResponse, PaymentProcessRequest } from '../types';

export const paymentService = {
  /**
   * Initiates a new payment for the specified order ID.
   * Backend verifies order ownership, prevents duplicates,
   * authoritatively computes the payment amount, and sets status to PENDING.
   */
  async createPayment(orderId: number): Promise<PaymentResponse> {
    const response = await apiClient.post<PaymentResponse>(`/api/payments/orders/${orderId}`);
    return response.data;
  },

  /**
   * Processes a pending payment simulation with a success or failure flag.
   * simulateSuccess = true: Generates transaction ID, marks payment SUCCESS, and confirms order.
   * simulateSuccess = false: Marks payment FAILED and leaves order in its current status.
   */
  async processPayment(paymentId: number, simulateSuccess: boolean): Promise<PaymentResponse> {
    const payload: PaymentProcessRequest = { simulateSuccess };
    const response = await apiClient.post<PaymentResponse>(`/api/payments/${paymentId}/process`, payload);
    return response.data;
  },

  /**
   * Refunds a completed (SUCCESS) payment.
   * Backend marks payment as REFUNDED and the associated order as CANCELLED.
   */
  async refundPayment(paymentId: number): Promise<PaymentResponse> {
    const response = await apiClient.post<PaymentResponse>(`/api/payments/${paymentId}/refund`);
    return response.data;
  },
};

export default paymentService;
