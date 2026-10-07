import { createContext } from 'react';
import type {
  User,
  LoginRequest,
  LoginResponse,
  UserCreateRequest,
  UserResponse,
} from '../types';

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<LoginResponse>;
  register: (data: UserCreateRequest) => Promise<UserResponse>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<UserResponse | null>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
