import React, { useState, useEffect, useCallback, type ReactNode } from 'react';
import apiClient from '../services/api';
import {
  getAccessToken,
  getRefreshToken,
  getStoredUser,
  setTokens,
  setStoredUser,
  clearAuthStorage,
} from '../utils/authStorage';
import type {
  User,
  LoginRequest,
  LoginResponse,
  UserCreateRequest,
  UserResponse,
} from '../types';
import { AuthContext, type AuthContextType } from './auth-context-base';

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => getStoredUser());
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Load and verify current user on initial mount
  useEffect(() => {
    let isMounted = true;

    const initAuth = async () => {
      const token = getAccessToken();
      if (!token) {
        if (isMounted) {
          setUser(null);
          setIsLoading(false);
        }
        return;
      }

      try {
        const response = await apiClient.get<UserResponse>('/api/auth/me');
        if (isMounted) {
          const fetchedUser: User = {
            id: response.data.id,
            firstName: response.data.firstName,
            lastName: response.data.lastName,
            email: response.data.email,
            phone: response.data.phone,
            enabled: response.data.enabled,
            createdAt: response.data.createdAt,
            updatedAt: response.data.updatedAt,
          };
          setStoredUser(fetchedUser);
          setUser(fetchedUser);
        }
      } catch {
        // If /api/auth/me failed and token was cleared or invalid
        if (isMounted) {
          if (!getAccessToken()) {
            setUser(null);
          }
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    initAuth();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = useCallback(async (credentials: LoginRequest): Promise<LoginResponse> => {
    setIsLoading(true);
    try {
      const response = await apiClient.post<LoginResponse>('/api/auth/login', credentials);
      const { token, refreshToken, id, firstName, lastName, email, phone, enabled } = response.data;

      setTokens(token, refreshToken);

      const loggedInUser: User = {
        id,
        firstName,
        lastName,
        email,
        phone,
        enabled,
      };

      setStoredUser(loggedInUser);
      setUser(loggedInUser);

      return response.data;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (data: UserCreateRequest): Promise<UserResponse> => {
    setIsLoading(true);
    try {
      const response = await apiClient.post<UserResponse>('/api/users', data);
      return response.data;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) {
        await apiClient.post('/api/auth/logout', { refreshToken });
      }
    } catch {
      // Safely ignore logout API failure if token is already expired/revoked
    } finally {
      clearAuthStorage();
      setUser(null);
    }
  }, []);

  const refreshUser = useCallback(async (): Promise<UserResponse | null> => {
    try {
      const response = await apiClient.get<UserResponse>('/api/auth/me');
      const refreshedUser: User = {
        id: response.data.id,
        firstName: response.data.firstName,
        lastName: response.data.lastName,
        email: response.data.email,
        phone: response.data.phone,
        enabled: response.data.enabled,
        createdAt: response.data.createdAt,
        updatedAt: response.data.updatedAt,
      };
      setStoredUser(refreshedUser);
      setUser(refreshedUser);
      return response.data;
    } catch {
      return null;
    }
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user && !!getAccessToken(),
    isLoading,
    login,
    register,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthProvider;
