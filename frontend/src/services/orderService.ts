import apiClient from './api';
import type {
  OrderResponse,
  OrderQueryParams,
  PageResponse,
} from '../types';

export const orderService = {
  /**
   * Create an order from the authenticated user's active cart.
   * User identity is resolved via the JWT token by the backend.
   */
  async createOrder(): Promise<OrderResponse> {
    const response = await apiClient.post<OrderResponse>('/api/orders');
    return response.data;
  },

  /**
   * Get a paginated list of orders placed by the authenticated user.
   */
  async getOrders(params?: OrderQueryParams): Promise<PageResponse<OrderResponse>> {
    const response = await apiClient.get<PageResponse<OrderResponse>>('/api/orders', {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
      },
    });
    return response.data;
  },

  /**
   * Get order details by order ID for the authenticated user.
   */
  async getOrderById(orderId: number): Promise<OrderResponse> {
    const response = await apiClient.get<OrderResponse>(`/api/orders/${orderId}`);
    return response.data;
  },

  /**
   * Cancel an order if it is in a cancellable status (PENDING / CONFIRMED).
   */
  async cancelOrder(orderId: number): Promise<OrderResponse> {
    const response = await apiClient.post<OrderResponse>(`/api/orders/${orderId}/cancel`);
    return response.data;
  },
};

export default orderService;
