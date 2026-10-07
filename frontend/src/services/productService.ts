import apiClient from './api';
import type { Product, ProductQueryParams, PageResponse } from '../types';

export const productService = {
  /**
   * Retrieves a paginated, filterable, and sortable list of products.
   * Query params: page, size, sortBy, direction, search, enabled, categoryId.
   */
  async getProducts(params?: ProductQueryParams): Promise<PageResponse<Product>> {
    const response = await apiClient.get<PageResponse<Product>>('/api/products', {
      params: {
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sortBy: params?.sortBy ?? 'createdAt',
        direction: params?.direction ?? 'desc',
        ...(params?.search ? { search: params.search } : {}),
        ...(params?.enabled !== undefined ? { enabled: params.enabled } : {}),
        ...(params?.categoryId !== undefined ? { categoryId: params.categoryId } : {}),
      },
    });
    return response.data;
  },

  /**
   * Retrieves details of a single product by unique ID.
   */
  async getProductById(id: number): Promise<Product> {
    const response = await apiClient.get<Product>(`/api/products/${id}`);
    return response.data;
  },
};

export default productService;
