import { createContext } from 'react';
import type { WishlistItemResponse } from '../types';

export interface WishlistContextType {
  wishlistItems: WishlistItemResponse[];
  wishlistCount: number;
  isLoading: boolean;
  fetchWishlist: () => Promise<void>;
  addToWishlist: (productId: number) => Promise<WishlistItemResponse>;
  removeFromWishlist: (wishlistItemId: number) => Promise<void>;
  toggleWishlist: (productId: number) => Promise<boolean>;
  clearWishlist: () => Promise<void>;
  isInWishlist: (productId: number) => boolean;
  getWishlistItemId: (productId: number) => number | undefined;
}

export const WishlistContext = createContext<WishlistContextType | undefined>(undefined);
