import apiClient from './api';
import type { WishlistItemResponse, WishlistQueryParams, PageResponse } from '../types';

export const wishlistService = {
  /**
   * Retrieves paginated wishlist items for the authenticated user.
   */
  async getWishlist(params?: WishlistQueryParams): Promise<PageResponse<WishlistItemResponse>> {
    const response = await apiClient.get<PageResponse<WishlistItemResponse>>('/api/wishlist', {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sortBy: params?.sortBy ?? 'createdAt',
        direction: params?.direction ?? 'desc',
      },
    });
    return response.data;
  },

  /**
   * Retrieves a single wishlist item by ID.
   */
  async getItem(wishlistItemId: number): Promise<WishlistItemResponse> {
    const response = await apiClient.get<WishlistItemResponse>(`/api/wishlist/${wishlistItemId}`);
    return response.data;
  },

  /**
   * Adds a product to the user's wishlist.
   */
  async addItem(productId: number): Promise<WishlistItemResponse> {
    const response = await apiClient.post<WishlistItemResponse>(`/api/wishlist/products/${productId}`);
    return response.data;
  },

  /**
   * Removes an item from the wishlist by wishlist item ID.
   */
  async removeItem(wishlistItemId: number): Promise<void> {
    await apiClient.delete(`/api/wishlist/${wishlistItemId}`);
  },

  /**
   * Checks whether a product currently exists in the user's wishlist.
   */
  async isProductInWishlist(productId: number): Promise<boolean> {
    const response = await apiClient.get<{ exists: boolean }>(`/api/wishlist/products/${productId}/exists`);
    return response.data?.exists ?? false;
  },

  /**
   * Clears all items from the user's wishlist.
   */
  async clearWishlist(): Promise<void> {
    await apiClient.delete('/api/wishlist');
  },
};

export default wishlistService;
