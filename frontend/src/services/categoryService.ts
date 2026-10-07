import apiClient from './api';
import type { Category } from '../types';

export const categoryService = {
  /**
   * Retrieves all product categories.
   */
  async getCategories(): Promise<Category[]> {
    const response = await apiClient.get<Category[]>('/api/categories');
    return response.data;
  },

  /**
   * Retrieves active product categories.
   */
  async getActiveCategories(): Promise<Category[]> {
    const response = await apiClient.get<Category[]>('/api/categories/active');
    return response.data;
  },

  /**
   * Retrieves a category by unique ID.
   */
  async getCategoryById(id: number): Promise<Category> {
    const response = await apiClient.get<Category>(`/api/categories/${id}`);
    return response.data;
  },

  /**
   * Retrieves a category by unique name.
   */
  async getCategoryByName(name: string): Promise<Category> {
    const response = await apiClient.get<Category>(`/api/categories/name/${encodeURIComponent(name)}`);
    return response.data;
  },
};

export default categoryService;
