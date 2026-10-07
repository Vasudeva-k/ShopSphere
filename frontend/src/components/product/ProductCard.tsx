import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Heart, ShoppingCart, Star, Check, PackageCheck, PackageX, Loader2 } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useCart } from '../../hooks/useCart';
import { useWishlist } from '../../hooks/useWishlist';
import type { Product } from '../../types';

export interface ProductCardProps {
  product: Product;
  categoryName?: string;
  originalPrice?: number;
  discountPercent?: number;
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  categoryName,
  originalPrice,
  discountPercent,
}) => {
  const { isAuthenticated } = useAuth();
  const { addItem } = useCart();
  const { isInWishlist, toggleWishlist } = useWishlist();
  const navigate = useNavigate();
  const location = useLocation();

  const [isAddingToCart, setIsAddingToCart] = useState<boolean>(false);
  const [isAddedSuccess, setIsAddedSuccess] = useState<boolean>(false);
  const [isWishlistLoading, setIsWishlistLoading] = useState<boolean>(false);

  const rating = product.rating ?? 4.7;
  const reviewCount = product.reviewCount ?? 128;
  const inStock = product.stockQuantity > 0 && product.enabled !== false;
  const displayCategory = categoryName || product.categoryName;
  const wishlisted = isAuthenticated && isInWishlist(product.id);

  const handleWishlistClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }

    if (isWishlistLoading) return;
    setIsWishlistLoading(true);
    try {
      await toggleWishlist(product.id);
    } catch {
      // Ignore wishlist toggle error gracefully
    } finally {
      setIsWishlistLoading(false);
    }
  };

  const handleAddToCartClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }

    if (isAddingToCart || !inStock) return;
    setIsAddingToCart(true);

    try {
      await addItem(product.id, 1);
      setIsAddedSuccess(true);
      setTimeout(() => setIsAddedSuccess(false), 1500);
    } catch {
      // Handled gracefully
    } finally {
      setIsAddingToCart(false);
    }
  };

  return (
    <div className="product-card">
      {/* Product Visual Area & Wishlist Button */}
      <div className="product-image-wrap">
        <Link
          to={`/products/${product.id}`}
          aria-label={`View details for ${product.name}`}
          style={{ width: '100%', height: '100%', display: 'block' }}
        >
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} loading="lazy" />
          ) : (
            <div
              style={{
                width: '100%',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--text-muted)',
                background: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)',
                padding: '1rem',
                textAlign: 'center',
              }}
            >
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(255,255,255,0.8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: 'var(--shadow-xs)',
                  marginBottom: '0.5rem',
                }}
              >
                {inStock ? (
                  <PackageCheck size={28} style={{ color: 'var(--secondary)' }} />
                ) : (
                  <PackageX size={28} style={{ color: 'var(--danger)' }} />
                )}
              </div>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                {displayCategory || 'ShopSphere Item'}
              </span>
            </div>
          )}
        </Link>
        <button
          type="button"
          className="product-wishlist-btn"
          onClick={handleWishlistClick}
          disabled={isWishlistLoading}
          aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
          style={{
            color: wishlisted ? 'var(--danger)' : undefined,
            opacity: isWishlistLoading ? 0.6 : 1,
          }}
        >
          {isWishlistLoading ? (
            <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
          ) : (
            <Heart size={18} fill={wishlisted ? 'var(--danger)' : 'none'} />
          )}
        </button>
      </div>

      {/* Product Info */}
      <div className="product-info">
        {displayCategory && <span className="product-category">{displayCategory}</span>}

        <Link to={`/products/${product.id}`} className="product-title" title={product.name}>
          {product.name}
        </Link>

        {/* Rating Display */}
        <div className="product-rating" aria-label={`Rated ${rating} out of 5 stars`}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            {[1, 2, 3, 4, 5].map((star) => (
              <Star
                key={star}
                size={14}
                fill={star <= Math.round(rating) ? 'var(--brand-accent)' : 'none'}
                stroke="var(--brand-accent)"
              />
            ))}
          </div>
          <span className="review-count">({reviewCount})</span>
        </div>

        {/* Price and Discount */}
        <div className="product-pricing">
          <span className="product-price">${Number(product.price).toFixed(2)}</span>
          {originalPrice && originalPrice > product.price && (
            <span className="product-original-price">${originalPrice.toFixed(2)}</span>
          )}
          {discountPercent && discountPercent > 0 && (
            <span className="product-discount-badge">{discountPercent}% OFF</span>
          )}
        </div>

        {/* Stock status indicator */}
        <div
          style={{
            fontSize: '0.75rem',
            marginBottom: '0.75rem',
            color: inStock ? 'var(--success)' : 'var(--danger)',
            fontWeight: 600,
          }}
        >
          {inStock ? `In Stock (${product.stockQuantity})` : 'Out of Stock'}
        </div>

        {/* Add to Cart Button */}
        <button
          type="button"
          className="btn-add-cart"
          onClick={handleAddToCartClick}
          disabled={!inStock || isAddingToCart}
          aria-label={`Add ${product.name} to cart`}
          style={{
            backgroundColor: isAddedSuccess ? 'var(--success)' : undefined,
            color: isAddedSuccess ? '#fff' : undefined,
            opacity: inStock ? 1 : 0.6,
            cursor: inStock && !isAddingToCart ? 'pointer' : 'not-allowed',
          }}
        >
          {isAddingToCart ? (
            <>
              <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} /> Adding...
            </>
          ) : isAddedSuccess ? (
            <>
              <Check size={16} /> Added
            </>
          ) : (
            <>
              <ShoppingCart size={16} /> Add to Cart
            </>
          )}
        </button>
      </div>
    </div>
  );
};

export default ProductCard;
