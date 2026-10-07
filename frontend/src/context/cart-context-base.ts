import { createContext } from 'react';
import type { CartResponse } from '../types';

export interface CartContextType {
  cart: CartResponse | null;
  itemCount: number;
  isLoading: boolean;
  fetchCart: () => Promise<CartResponse | null>;
  addItem: (productId: number, quantity?: number) => Promise<CartResponse>;
  updateQuantity: (itemId: number, quantity: number) => Promise<CartResponse>;
  removeItem: (itemId: number) => Promise<CartResponse>;
  clearCart: () => Promise<CartResponse>;
}

export const CartContext = createContext<CartContextType | undefined>(undefined);
