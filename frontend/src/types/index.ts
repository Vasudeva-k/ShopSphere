// ShopSphere Data Types matching Backend API Entities & DTOs

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
}

export interface UserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  enabled: boolean;
  token: string;
  refreshToken: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  token: string;
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CategoryResponse extends Category {}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  enabled?: boolean;
  active?: boolean;
  categoryId?: number;
  categoryName?: string;
  imageUrl?: string;
  rating?: number;
  reviewCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  enabled: boolean;
  categoryId: number;
  createdAt: string;
  updatedAt: string;
}

export type ProductSortField =
  | 'id'
  | 'name'
  | 'price'
  | 'stockQuantity'
  | 'createdAt'
  | 'updatedAt';

export type SortDirection = 'asc' | 'desc';

export interface ProductQueryParams {
  page?: number;
  size?: number;
  sortBy?: ProductSortField;
  direction?: SortDirection;
  search?: string;
  enabled?: boolean;
  categoryId?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements?: number;
}

export interface AddCartItemRequest {
  productId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface CartItemResponse extends CartItem {}

export interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
  total: number;
  totalAmount?: number;
}

export interface CartResponse extends Cart {}

export interface WishlistItemResponse {
  id: number;
  productId: number;
  productName: string;
  productPrice: number;
  productEnabled: boolean;
  createdAt: string;
}

export interface WishlistItem {
  id: number;
  userId?: number;
  productId: number;
  productName: string;
  unitPrice?: number;
  productPrice?: number;
  productEnabled?: boolean;
  inStock?: boolean;
  addedAt?: string;
  createdAt?: string;
}

export interface WishlistQueryParams {
  page?: number;
  size?: number;
  sortBy?: 'id' | 'createdAt';
  direction?: SortDirection;
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export interface OrderItemResponse {
  id: number;
  productId: number | null;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderResponse {
  id: number;
  userId: number;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItemResponse[];
}

export interface OrderItem extends OrderItemResponse {}
export interface Order extends OrderResponse {}

export interface OrderQueryParams {
  page?: number;
  size?: number;
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED';

export interface PaymentResponse {
  id: number;
  orderId: number | null;
  amount: number;
  status: PaymentStatus;
  transactionId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentProcessRequest {
  simulateSuccess: boolean;
}

export interface Payment extends PaymentResponse {
  paymentMethod?: string;
}

export interface Review {
  id: number;
  userId: number;
  userFirstName?: string;
  userLastName?: string;
  productId: number;
  rating: number; // 1 to 5
  comment: string;
  createdAt: string;
  updatedAt: string;
}

export interface Invoice {
  id: number;
  orderId: number;
  invoiceNumber: string;
  subtotal: number;
  totalAmount: number;
  issuedAt: string;
}
