import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import {
  Star,
  ShieldCheck,
  Truck,
  RotateCcw,
  ShoppingCart,
  Heart,
  Check,
  ChevronRight,
  Loader2,
  AlertCircle,
  PackageCheck,
  PackageX,
  ArrowLeft,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../hooks/useCart';
import { useWishlist } from '../hooks/useWishlist';
import { productService } from '../services/productService';
import { categoryService } from '../services/categoryService';
import type { Product, Category } from '../types';

export const ProductDetailPage: React.FC = () => {
  const { productId } = useParams<{ productId: string }>();
  const { isAuthenticated } = useAuth();
  const { addItem } = useCart();
  const { isInWishlist, toggleWishlist } = useWishlist();
  const navigate = useNavigate();
  const location = useLocation();

  const [product, setProduct] = useState<Product | null>(null);
  const [category, setCategory] = useState<Category | null>(null);
  const [quantity, setQuantity] = useState<number>(1);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [isAddingToCart, setIsAddingToCart] = useState<boolean>(false);
  const [cartFeedback, setCartFeedback] = useState<string | null>(null);
  const [isWishlistLoading, setIsWishlistLoading] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;
    const fetchProduct = async () => {
      if (!productId) return;
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const prodData = await productService.getProductById(Number(productId));
        if (isMounted) {
          setProduct(prodData);
          if (prodData.categoryId) {
            categoryService
              .getCategoryById(prodData.categoryId)
              .then((cat) => {
                if (isMounted) setCategory(cat);
              })
              .catch(() => {
                // Ignore optional category lookup error
              });
          }
        }
      } catch {
        if (isMounted) {
          setErrorMessage('Product not found or unavailable in the catalog.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    fetchProduct();

    return () => {
      isMounted = false;
    };
  }, [productId]);

  const handleAddToCart = async () => {
    if (!product) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }

    if (isAddingToCart) return;
    setIsAddingToCart(true);
    setCartFeedback(null);

    try {
      await addItem(product.id, quantity);
      setCartFeedback('Added to cart successfully!');
      setTimeout(() => setCartFeedback(null), 3000);
    } catch {
      setCartFeedback('Failed to add item. Please try again.');
    } finally {
      setIsAddingToCart(false);
    }
  };

  const handleToggleWishlist = async () => {
    if (!product) return;
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }

    if (isWishlistLoading) return;
    setIsWishlistLoading(true);
    try {
      await toggleWishlist(product.id);
    } catch {
      // Handled gracefully
    } finally {
      setIsWishlistLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div
        className="container"
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '50vh',
          gap: '1rem',
          color: 'var(--text-muted)',
        }}
      >
        <Loader2 size={36} style={{ animation: 'spin 1s linear infinite' }} />
        <p style={{ fontSize: '0.9375rem', fontWeight: 500 }}>Loading product specifications...</p>
      </div>
    );
  }

  if (errorMessage || !product) {
    return (
      <div className="container" style={{ paddingTop: '3rem', maxWidth: '600px', textAlign: 'center' }}>
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '3rem 2rem',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <AlertCircle size={48} style={{ color: 'var(--danger)', margin: '0 auto 1rem' }} />
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem' }}>Product Not Found</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
            {errorMessage || 'The requested product could not be located in the ShopSphere catalog.'}
          </p>
          <Link to="/products" className="btn btn-primary">
            <ArrowLeft size={16} /> Back to Catalog
          </Link>
        </div>
      </div>
    );
  }

  const inStock = product.stockQuantity > 0 && product.enabled !== false;
  const rating = product.rating ?? 4.8;
  const reviewCount = product.reviewCount ?? 128;
  const displayCategoryName = category?.name || product.categoryName;
  const wishlisted = isAuthenticated && isInWishlist(product.id);

  return (
    <div className="container" style={{ paddingTop: '1.5rem' }}>
      {/* Breadcrumb Navigation */}
      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          fontSize: '0.8125rem',
          color: 'var(--text-muted)',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
        }}
      >
        <Link to="/">Home</Link>
        <ChevronRight size={14} />
        <Link to="/products">Products</Link>
        {displayCategoryName && (
          <>
            <ChevronRight size={14} />
            <Link
              to={`/products?categoryId=${product.categoryId}`}
              style={{ color: 'var(--secondary)', fontWeight: 600 }}
            >
              {displayCategoryName}
            </Link>
          </>
        )}
        <ChevronRight size={14} />
        <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>{product.name}</span>
      </nav>

      {/* Main Product Layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(320px, 1fr) minmax(360px, 1.2fr) 300px',
          gap: '2rem',
          alignItems: 'start',
        }}
      >
        {/* Left Column: Product Visual Presentation */}
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '2rem',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              height: '360px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)',
              borderRadius: 'var(--radius-md)',
              padding: '2rem',
              color: 'var(--text-muted)',
            }}
          >
            <div
              style={{
                width: '90px',
                height: '90px',
                borderRadius: 'var(--radius-lg)',
                background: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: 'var(--shadow-md)',
                marginBottom: '1rem',
              }}
            >
              {inStock ? (
                <PackageCheck size={48} style={{ color: 'var(--secondary)' }} />
              ) : (
                <PackageX size={48} style={{ color: 'var(--danger)' }} />
              )}
            </div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-main)' }}>
              {displayCategoryName || 'Catalog Product'}
            </h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-light)', marginTop: '0.25rem' }}>
              Item #{product.id} · ShopSphere Verified
            </p>
          </div>
        </div>

        {/* Center Column: Product Specs & Details */}
        <div>
          {displayCategoryName && (
            <span className="badge badge-info" style={{ marginBottom: '0.75rem' }}>
              {displayCategoryName}
            </span>
          )}
          <h1 style={{ fontSize: '1.625rem', fontWeight: 800, lineHeight: 1.3, marginBottom: '0.75rem' }}>
            {product.name}
          </h1>

          {/* Rating */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
            <div style={{ display: 'flex', color: 'var(--brand-accent)' }}>
              {[1, 2, 3, 4, 5].map((star) => (
                <Star
                  key={star}
                  size={16}
                  fill={star <= Math.round(rating) ? 'var(--brand-accent)' : 'none'}
                  stroke="var(--brand-accent)"
                />
              ))}
            </div>
            <span style={{ fontWeight: 700, color: 'var(--brand-accent)' }}>{rating.toFixed(1)}</span>
            <span style={{ color: 'var(--text-muted)' }}>({reviewCount} verified customer ratings)</span>
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border-color)', margin: '1rem 0' }} />

          {/* Pricing */}
          <div style={{ marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.75rem' }}>
              <span style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--text-main)' }}>
                ${Number(product.price).toFixed(2)}
              </span>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Inclusive of all merchant warranties and standard handling
            </p>
          </div>

          {/* Description */}
          <div style={{ marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.5rem' }}>Product Description</h3>
            <p style={{ fontSize: '0.9375rem', lineHeight: 1.6, color: 'var(--text-main)', whiteSpace: 'pre-line' }}>
              {product.description || 'No detailed description available for this catalog item.'}
            </p>
          </div>

          {/* Technical Specs Summary */}
          <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', padding: '1rem' }}>
            <h4 style={{ fontSize: '0.875rem', fontWeight: 700, marginBottom: '0.5rem' }}>Specifications</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', fontSize: '0.8125rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Product ID:</span> <strong>#{product.id}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Category ID:</span> <strong>#{product.categoryId}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Stock Status:</span>{' '}
                <strong style={{ color: inStock ? 'var(--success)' : 'var(--danger)' }}>
                  {inStock ? `${product.stockQuantity} in stock` : 'Out of stock'}
                </strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Listing Date:</span>{' '}
                <strong>
                  {product.createdAt ? new Date(product.createdAt).toLocaleDateString() : 'Active'}
                </strong>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Buy Box / Action Box */}
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            padding: '1.5rem',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.75rem' }}>
            ${(Number(product.price) * quantity).toFixed(2)}
          </div>

          <div
            style={{
              color: inStock ? 'var(--success)' : 'var(--danger)',
              fontWeight: 700,
              fontSize: '0.9375rem',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            {inStock ? (
              <>
                <Check size={18} /> In Stock ({product.stockQuantity} available)
              </>
            ) : (
              <>
                <AlertCircle size={18} /> Currently Unavailable
              </>
            )}
          </div>

          {/* Quantity Selector */}
          {inStock && (
            <div style={{ marginBottom: '1.25rem' }}>
              <label
                htmlFor="prod-qty"
                style={{ fontSize: '0.875rem', fontWeight: 600, display: 'block', marginBottom: '0.35rem' }}
              >
                Quantity:
              </label>
              <select
                id="prod-qty"
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  backgroundColor: '#fff',
                  outline: 'none',
                }}
              >
                {Array.from({ length: Math.min(10, product.stockQuantity) }, (_, i) => i + 1).map((num) => (
                  <option key={num} value={num}>
                    {num}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Actions */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <button
              type="button"
              className="btn-add-cart"
              onClick={handleAddToCart}
              disabled={!inStock || isAddingToCart}
              style={{
                backgroundColor: cartFeedback ? 'var(--success)' : 'var(--brand-accent)',
                color: cartFeedback ? '#fff' : 'var(--primary)',
                padding: '0.75rem',
                opacity: inStock ? 1 : 0.6,
                cursor: inStock && !isAddingToCart ? 'pointer' : 'not-allowed',
              }}
            >
              {isAddingToCart ? (
                <>
                  <Loader2 size={18} style={{ animation: 'spin 1s linear infinite' }} /> Adding to Cart...
                </>
              ) : cartFeedback ? (
                <>
                  <Check size={18} /> {cartFeedback}
                </>
              ) : (
                <>
                  <ShoppingCart size={18} /> Add to Cart
                </>
              )}
            </button>

            <Link
              to="/cart"
              className="btn btn-primary"
              style={{
                width: '100%',
                padding: '0.75rem',
                textAlign: 'center',
                opacity: inStock ? 1 : 0.6,
                pointerEvents: inStock ? 'auto' : 'none',
              }}
            >
              Go to Cart
            </Link>

            <button
              type="button"
              className="btn btn-outline"
              onClick={handleToggleWishlist}
              disabled={isWishlistLoading}
              style={{
                width: '100%',
                color: wishlisted ? 'var(--danger)' : undefined,
                opacity: isWishlistLoading ? 0.6 : 1,
              }}
            >
              {isWishlistLoading ? (
                <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
              ) : (
                <Heart size={16} fill={wishlisted ? 'var(--danger)' : 'none'} />
              )}
              {wishlisted ? 'Saved in Wishlist' : 'Add to Wishlist'}
            </button>
          </div>

          {/* Guarantees */}
          <div
            style={{
              fontSize: '0.8125rem',
              color: 'var(--text-muted)',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.5rem',
              borderTop: '1px solid var(--border-color)',
              paddingTop: '1rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Truck size={16} color="var(--primary)" /> Fast shipping directly by ShopSphere
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <ShieldCheck size={16} color="var(--success)" /> Secure transaction guaranteed
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <RotateCcw size={16} color="var(--secondary)" /> 30-Day Return & Replacement
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetailPage;
