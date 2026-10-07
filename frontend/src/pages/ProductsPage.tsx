import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Filter,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Loader2,
  PackageSearch,
  RotateCcw,
} from 'lucide-react';
import { ProductCard } from '../components/product/ProductCard';
import { productService } from '../services/productService';
import { categoryService } from '../services/categoryService';
import type { Product, Category, PageResponse, ProductSortField, SortDirection } from '../types';

export const ProductsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // URL state
  const searchQuery = searchParams.get('search') || '';
  const categoryIdParam = searchParams.get('categoryId');
  const selectedCategoryId = categoryIdParam ? Number(categoryIdParam) : undefined;
  const pageParam = searchParams.get('page');
  const currentPage = pageParam ? Math.max(0, Number(pageParam)) : 0;
  const sortByParam = (searchParams.get('sortBy') as ProductSortField) || 'createdAt';
  const directionParam = (searchParams.get('direction') as SortDirection) || 'desc';

  // Component state
  const [categories, setCategories] = useState<Category[]>([]);
  const [pageData, setPageData] = useState<PageResponse<Product> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load categories once
  useEffect(() => {
    let isMounted = true;
    const loadCategories = async () => {
      try {
        const data = await categoryService.getCategories();
        if (isMounted) {
          setCategories(data);
        }
      } catch {
        // Continue even if categories fail to load
      }
    };
    loadCategories();
    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch products whenever params change
  useEffect(() => {
    let isMounted = true;
    const fetchProducts = async () => {
      setIsLoading(true);
      setErrorMessage(null);
      try {
        const response = await productService.getProducts({
          page: currentPage,
          size: 20,
          sortBy: sortByParam,
          direction: directionParam,
          search: searchQuery || undefined,
          categoryId: selectedCategoryId,
          enabled: true,
        });

        if (isMounted) {
          setPageData(response);
        }
      } catch {
        if (isMounted) {
          setErrorMessage('Unable to load products. Please check your connection and try again.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    fetchProducts();

    return () => {
      isMounted = false;
    };
  }, [currentPage, sortByParam, directionParam, searchQuery, selectedCategoryId]);

  // Category map for quick ID -> name lookup
  const categoryMap = useMemo(() => {
    const map = new Map<number, string>();
    categories.forEach((c) => map.set(c.id, c.name));
    return map;
  }, [categories]);

  // Update query params helper
  const updateParams = (updates: Record<string, string | null>) => {
    const nextParams = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, val]) => {
      if (val === null || val === '') {
        nextParams.delete(key);
      } else {
        nextParams.set(key, val);
      }
    });
    setSearchParams(nextParams);
  };

  const handleCategorySelect = (id: number | null) => {
    updateParams({
      categoryId: id !== null ? String(id) : null,
      page: '0',
    });
  };

  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const [sortBy, direction] = e.target.value.split(':');
    updateParams({
      sortBy,
      direction,
      page: '0',
    });
  };

  const handlePageChange = (newPage: number) => {
    updateParams({
      page: String(newPage),
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const currentSortKey = `${sortByParam}:${directionParam}`;
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;
  const products = pageData?.content ?? [];
  const selectedCategoryName = selectedCategoryId ? categoryMap.get(selectedCategoryId) : undefined;

  return (
    <div className="container" style={{ paddingTop: '2rem' }}>
      {/* Header & Filter Bar */}
      <div
        className="page-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div>
          <h1 className="page-title">
            {searchQuery
              ? `Results for "${searchQuery}"`
              : selectedCategoryName
              ? `${selectedCategoryName}`
              : 'All Products'}
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            {isLoading
              ? 'Searching catalog...'
              : `Showing ${products.length > 0 ? currentPage * 20 + 1 : 0}-${Math.min(
                  (currentPage + 1) * 20,
                  totalElements
                )} of ${totalElements} products`}
          </p>
        </div>

        {/* Sort Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <SlidersHorizontal size={16} style={{ color: 'var(--text-muted)' }} />
            <label htmlFor="catalog-sort" style={{ fontSize: '0.875rem', fontWeight: 600 }}>
              Sort by:
            </label>
            <select
              id="catalog-sort"
              value={currentSortKey}
              onChange={handleSortChange}
              style={{
                padding: '0.4rem 0.8rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                backgroundColor: 'var(--bg-surface)',
                fontSize: '0.875rem',
                outline: 'none',
              }}
            >
              <option value="createdAt:desc">Newest Arrivals</option>
              <option value="price:asc">Price: Low to High</option>
              <option value="price:desc">Price: High to Low</option>
              <option value="name:asc">Name: A to Z</option>
              <option value="stockQuantity:desc">Stock: High to Low</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Catalog Layout */}
      <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr', gap: '2rem', marginTop: '1.5rem' }}>
        {/* Filter Sidebar */}
        <aside
          style={{
            background: 'var(--bg-surface)',
            padding: '1.25rem',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            height: 'fit-content',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '1.25rem',
              paddingBottom: '0.75rem',
              borderBottom: '1px solid var(--border-color)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Filter size={18} />
              <span style={{ fontWeight: 700 }}>Categories</span>
            </div>
            {(selectedCategoryId !== undefined || searchQuery) && (
              <button
                type="button"
                onClick={() => updateParams({ categoryId: null, search: null, page: '0' })}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--secondary)',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '2px',
                }}
              >
                <RotateCcw size={12} /> Clear
              </button>
            )}
          </div>

          <div>
            <ul style={{ listStyle: 'none', fontSize: '0.875rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <li>
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    cursor: 'pointer',
                    fontWeight: selectedCategoryId === undefined ? 700 : 400,
                    color: selectedCategoryId === undefined ? 'var(--secondary)' : 'inherit',
                  }}
                >
                  <input
                    type="radio"
                    name="categoryFilter"
                    checked={selectedCategoryId === undefined}
                    onChange={() => handleCategorySelect(null)}
                  />
                  All Categories
                </label>
              </li>
              {categories.map((cat) => (
                <li key={cat.id}>
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      cursor: 'pointer',
                      fontWeight: selectedCategoryId === cat.id ? 700 : 400,
                      color: selectedCategoryId === cat.id ? 'var(--secondary)' : 'inherit',
                    }}
                  >
                    <input
                      type="radio"
                      name="categoryFilter"
                      checked={selectedCategoryId === cat.id}
                      onChange={() => handleCategorySelect(cat.id)}
                    />
                    {cat.name}
                  </label>
                </li>
              ))}
            </ul>
          </div>
        </aside>

        {/* Product Grid Area */}
        <section>
          {/* Loading State */}
          {isLoading && (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '300px',
                gap: '1rem',
                color: 'var(--text-muted)',
              }}
            >
              <Loader2 size={36} style={{ animation: 'spin 1s linear infinite' }} />
              <p style={{ fontSize: '0.9375rem', fontWeight: 500 }}>Fetching products from ShopSphere...</p>
            </div>
          )}

          {/* Error State */}
          {!isLoading && errorMessage && (
            <div
              style={{
                background: 'var(--danger-bg)',
                color: 'var(--danger)',
                border: '1px solid #fecaca',
                padding: '1.5rem',
                borderRadius: 'var(--radius-lg)',
                textAlign: 'center',
              }}
            >
              <AlertCircle size={32} style={{ margin: '0 auto 0.75rem' }} />
              <h3 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '0.5rem' }}>Failed to Load Products</h3>
              <p style={{ fontSize: '0.875rem', marginBottom: '1rem' }}>{errorMessage}</p>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => updateParams({})}
                style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }}
              >
                Retry
              </button>
            </div>
          )}

          {/* Empty State */}
          {!isLoading && !errorMessage && products.length === 0 && (
            <div className="placeholder-card">
              <PackageSearch className="placeholder-icon" size={48} />
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>No Products Found</h3>
              <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.9375rem' }}>
                {searchQuery
                  ? `We couldn't find any products matching "${searchQuery}".`
                  : 'There are no active products in this category right now.'}
              </p>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => updateParams({ search: null, categoryId: null, page: '0' })}
              >
                View All Products
              </button>
            </div>
          )}

          {/* Products Grid */}
          {!isLoading && !errorMessage && products.length > 0 && (
            <>
              <div className="product-grid">
                {products.map((product) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    categoryName={product.categoryId ? categoryMap.get(product.categoryId) : undefined}
                  />
                ))}
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.5rem',
                    marginTop: '2rem',
                    paddingTop: '1.5rem',
                    borderTop: '1px solid var(--border-color)',
                  }}
                >
                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={currentPage === 0}
                    onClick={() => handlePageChange(currentPage - 1)}
                    style={{ padding: '0.5rem 0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                    aria-label="Previous page"
                  >
                    <ChevronLeft size={16} /> Previous
                  </button>

                  <span style={{ fontSize: '0.875rem', fontWeight: 600, padding: '0 0.75rem', color: 'var(--text-muted)' }}>
                    Page {currentPage + 1} of {totalPages}
                  </span>

                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => handlePageChange(currentPage + 1)}
                    style={{ padding: '0.5rem 0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                    aria-label="Next page"
                  >
                    Next <ChevronRight size={16} />
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
};

export default ProductsPage;
