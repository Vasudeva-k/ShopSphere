import apiClient from './api';
import type { CartResponse, AddCartItemRequest, UpdateCartItemRequest } from '../types';

export const cartService = {
  /**
   * Retrieves the current authenticated user's shopping cart.
   */
  async getCart(): Promise<CartResponse> {
    const response = await apiClient.get<CartResponse>('/api/cart');
    return response.data;
  },

  /**
   * Adds an item to the shopping cart.
   */
  async addItem(productId: number, quantity: number): Promise<CartResponse> {
    const payload: AddCartItemRequest = { productId, quantity };
    const response = await apiClient.post<CartResponse>('/api/cart/items', payload);
    return response.data;
  },

  /**
   * Updates quantity of an existing item in the cart.
   */
  async updateItem(itemId: number, quantity: number): Promise<CartResponse> {
    const payload: UpdateCartItemRequest = { quantity };
    const response = await apiClient.put<CartResponse>(`/api/cart/items/${itemId}`, payload);
    return response.data;
  },

  /**
   * Removes a specific item from the cart.
   */
  async removeItem(itemId: number): Promise<CartResponse> {
    const response = await apiClient.delete<CartResponse>(`/api/cart/items/${itemId}`);
    return response.data;
  },

  /**
   * Clears all items from the shopping cart.
   */
  async clearCart(): Promise<CartResponse> {
    const response = await apiClient.delete<CartResponse>('/api/cart');
    return response.data;
  },
};

export default cartService;
