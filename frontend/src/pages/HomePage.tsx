import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Tag, Zap, TrendingUp, Award, Loader2, Layers } from 'lucide-react';
import { ProductCard } from '../components/product/ProductCard';
import { productService } from '../services/productService';
import { categoryService } from '../services/categoryService';
import type { Product, Category } from '../types';

export const HomePage: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [dealsProducts, setDealsProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    let isMounted = true;

    const loadData = async () => {
      setIsLoading(true);
      try {
        const [cats, prodsPage] = await Promise.all([
          categoryService.getCategories().catch(() => [] as Category[]),
          productService.getProducts({ size: 8, sortBy: 'createdAt', direction: 'desc', enabled: true }).catch(() => ({ content: [] as Product[] })),
        ]);

        if (isMounted) {
          setCategories(cats);
          const prods = prodsPage.content || [];
          setFeaturedProducts(prods);
          setDealsProducts(prods.slice(0, 4));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    loadData();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="home-page">
      {/* 1. Hero Banner */}
      <section className="hero-section">
        <div className="container hero-content">
          <div className="hero-text">
            <span className="badge badge-warning" style={{ marginBottom: '1rem' }}>
              <Zap size={14} style={{ marginRight: '4px' }} /> Discover ShopSphere
            </span>
            <h1>Shop Verified Marketplace Products</h1>
            <p>
              Explore top-tier electronics, fashion, home essentials, and more. Backed by secure Spring Boot JWT authentication and fast direct fulfillment.
            </p>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <Link
                to="/products"
                className="btn btn-primary"
                style={{ backgroundColor: 'var(--brand-accent)', color: 'var(--primary)' }}
              >
                Explore All Products <ArrowRight size={16} />
              </Link>
              <Link to="/products?sortBy=price&direction=asc" className="btn btn-outline" style={{ color: '#fff', borderColor: '#475569' }}>
                Budget Deals
              </Link>
            </div>
          </div>
          <div
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: 'var(--radius-lg)',
              padding: '2rem',
              textAlign: 'center',
              minWidth: '280px',
              backdropFilter: 'blur(8px)',
            }}
          >
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', color: '#fff' }}>
              ShopSphere Catalog
            </h3>
            <p style={{ fontSize: '0.875rem', color: '#cbd5e1', marginBottom: '1.25rem' }}>
              Real-time catalog synchronized with Spring Boot backend
            </p>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--brand-accent)', marginBottom: '1rem' }}>
              {categories.length} Categories Active
            </div>
            <Link to="/products" className="btn btn-secondary" style={{ width: '100%' }}>
              Browse Catalog
            </Link>
          </div>
        </div>
      </section>

      <div className="container">
        {/* 2. Shop by Category */}
        <section style={{ marginBottom: '3rem' }}>
          <div className="section-header">
            <h2 className="section-title">Shop by Category</h2>
            <Link to="/products" className="section-link">
              All categories <ArrowRight size={14} />
            </Link>
          </div>

          {isLoading ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
              <Loader2 size={28} style={{ animation: 'spin 1s linear infinite', margin: '0 auto 0.5rem' }} />
              <p style={{ fontSize: '0.875rem' }}>Loading categories...</p>
            </div>
          ) : categories.length === 0 ? (
            <div className="placeholder-card" style={{ padding: '2rem' }}>
              <Layers size={36} style={{ color: 'var(--text-light)', margin: '0 auto 0.5rem' }} />
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No categories registered yet in database.</p>
            </div>
          ) : (
            <div className="category-grid">
              {categories.map((cat) => (
                <Link
                  to={`/products?categoryId=${cat.id}`}
                  key={cat.id}
                  className="category-card"
                >
                  <div className="category-icon">📦</div>
                  <div className="category-name">{cat.name}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    {cat.description || 'Verified Collection'}
                  </div>
                </Link>
              ))}
            </div>
          )}
        </section>

        {/* 3. Today's Deals Section */}
        {dealsProducts.length > 0 && (
          <section style={{ marginBottom: '3rem' }}>
            <div className="section-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Tag className="brand-highlight" size={22} style={{ color: 'var(--brand-accent)' }} />
                <h2 className="section-title">Latest Catalog Arrivals</h2>
              </div>
              <Link to="/products" className="section-link">
                See all products <ArrowRight size={14} />
              </Link>
            </div>
            <div className="product-grid">
              {dealsProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          </section>
        )}

        {/* 4. Promotional Banner Section */}
        <section className="promo-banner">
          <div>
            <span
              className="badge badge-info"
              style={{ marginBottom: '0.75rem', background: 'rgba(255,255,255,0.2)', color: '#fff' }}
            >
              <Award size={14} style={{ marginRight: '4px' }} /> ShopSphere Certified
            </span>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem' }}>
              Discover Quality Products
            </h2>
            <p style={{ color: '#e0e7ff', maxWidth: '600px', fontSize: '0.9375rem' }}>
              Shop verified inventory with complete transparency on pricing, stock availability, and automated invoicing.
            </p>
          </div>
          <Link
            to="/products"
            className="btn btn-primary"
            style={{ backgroundColor: '#ffffff', color: 'var(--primary)', fontWeight: 700 }}
          >
            Explore Catalog
          </Link>
        </section>

        {/* 5. Featured Products */}
        {featuredProducts.length > 0 && (
          <section style={{ marginBottom: '3rem' }}>
            <div className="section-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <TrendingUp size={22} style={{ color: 'var(--secondary)' }} />
                <h2 className="section-title">Featured Marketplace Items</h2>
              </div>
              <Link to="/products" className="section-link">
                Explore more <ArrowRight size={14} />
              </Link>
            </div>
            <div className="product-grid">
              {featuredProducts.map((product) => (
                <ProductCard key={`featured-${product.id}`} product={product} />
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
};

export default HomePage;
