import React, { useState, useEffect, useCallback, type ReactNode } from 'react';
import { cartService } from '../services/cartService';
import { useAuth } from '../hooks/useAuth';
import type { CartResponse } from '../types';
import { CartContext, type CartContextType } from './cart-context-base';

export const CartProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;
    if (!isAuthenticated) {
      return;
    }

    cartService
      .getCart()
      .then((data) => {
        if (isMounted) {
          setCart(data);
        }
      })
      .catch(() => {
        if (isMounted) {
          setCart(null);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated]);

  const fetchCart = useCallback(async (): Promise<CartResponse | null> => {
    if (!isAuthenticated) {
      setCart(null);
      return null;
    }

    setIsLoading(true);
    try {
      const data = await cartService.getCart();
      setCart(data);
      return data;
    } catch {
      setCart(null);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  const addItem = useCallback(
    async (productId: number, quantity: number = 1): Promise<CartResponse> => {
      setIsLoading(true);
      try {
        const updated = await cartService.addItem(productId, quantity);
        setCart(updated);
        return updated;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const updateQuantity = useCallback(
    async (itemId: number, quantity: number): Promise<CartResponse> => {
      setIsLoading(true);
      try {
        const updated = await cartService.updateItem(itemId, quantity);
        setCart(updated);
        return updated;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const removeItem = useCallback(
    async (itemId: number): Promise<CartResponse> => {
      setIsLoading(true);
      try {
        const updated = await cartService.removeItem(itemId);
        setCart(updated);
        return updated;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const clearCart = useCallback(async (): Promise<CartResponse> => {
    setIsLoading(true);
    try {
      const updated = await cartService.clearCart();
      setCart(updated);
      return updated;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const itemCount = (isAuthenticated ? cart?.items : null)?.reduce((sum, item) => sum + item.quantity, 0) ?? 0;

  const value: CartContextType = {
    cart: isAuthenticated ? cart : null,
    itemCount,
    isLoading,
    fetchCart,
    addItem,
    updateQuantity,
    removeItem,
    clearCart,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

export default CartProvider;
