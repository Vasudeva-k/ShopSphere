import React, { useState, useEffect, useCallback, useMemo, type ReactNode } from 'react';
import { wishlistService } from '../services/wishlistService';
import { useAuth } from '../hooks/useAuth';
import type { WishlistItemResponse } from '../types';
import { WishlistContext, type WishlistContextType } from './wishlist-context-base';

export const WishlistProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [wishlistItems, setWishlistItems] = useState<WishlistItemResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;
    if (!isAuthenticated) {
      return;
    }

    wishlistService
      .getWishlist({ size: 100 })
      .then((page) => {
        if (isMounted) {
          setWishlistItems(page.content || []);
        }
      })
      .catch(() => {
        if (isMounted) {
          setWishlistItems([]);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated]);

  const fetchWishlist = useCallback(async (): Promise<void> => {
    if (!isAuthenticated) {
      setWishlistItems([]);
      return;
    }

    setIsLoading(true);
    try {
      const page = await wishlistService.getWishlist({ size: 100 });
      setWishlistItems(page.content || []);
    } catch {
      setWishlistItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  // Map of productId -> wishlistItemId for O(1) existence checks
  const productToItemIdMap = useMemo(() => {
    const map = new Map<number, number>();
    wishlistItems.forEach((item) => {
      map.set(item.productId, item.id);
    });
    return map;
  }, [wishlistItems]);

  const isInWishlist = useCallback(
    (productId: number): boolean => {
      return productToItemIdMap.has(productId);
    },
    [productToItemIdMap]
  );

  const getWishlistItemId = useCallback(
    (productId: number): number | undefined => {
      return productToItemIdMap.get(productId);
    },
    [productToItemIdMap]
  );

  const addToWishlist = useCallback(
    async (productId: number): Promise<WishlistItemResponse> => {
      setIsLoading(true);
      try {
        const newItem = await wishlistService.addItem(productId);
        setWishlistItems((prev) => [newItem, ...prev.filter((i) => i.id !== newItem.id)]);
        return newItem;
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const removeFromWishlist = useCallback(
    async (wishlistItemId: number): Promise<void> => {
      setIsLoading(true);
      try {
        await wishlistService.removeItem(wishlistItemId);
        setWishlistItems((prev) => prev.filter((item) => item.id !== wishlistItemId));
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const toggleWishlist = useCallback(
    async (productId: number): Promise<boolean> => {
      const existingItemId = productToItemIdMap.get(productId);
      if (existingItemId) {
        await removeFromWishlist(existingItemId);
        return false;
      } else {
        await addToWishlist(productId);
        return true;
      }
    },
    [productToItemIdMap, removeFromWishlist, addToWishlist]
  );

  const clearWishlist = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    try {
      await wishlistService.clearWishlist();
      setWishlistItems([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const activeWishlistItems = isAuthenticated ? wishlistItems : [];
  const wishlistCount = activeWishlistItems.length;

  const value: WishlistContextType = {
    wishlistItems: activeWishlistItems,
    wishlistCount,
    isLoading,
    fetchWishlist,
    addToWishlist,
    removeFromWishlist,
    toggleWishlist,
    clearWishlist,
    isInWishlist,
    getWishlistItemId,
  };

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
};

export default WishlistProvider;
