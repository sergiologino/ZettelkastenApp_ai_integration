// API клиент для AI-интеграционного сервиса

import type {
  LoginRequest,
  LoginResponse,
  NeuralNetwork,
  NetworkCreateRequest,
  ClientApplication,
  ClientCreateRequest,
  RequestLog,
  UsageStats,
} from './types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8091';

// Вспомогательная функция для работы с токеном
const getToken = (): string | null => {
  return localStorage.getItem('ai_admin_token');
};

const setToken = (token: string): void => {
  localStorage.setItem('ai_admin_token', token);
};

export const clearToken = (): void => {
  localStorage.removeItem('ai_admin_token');
};

// Базовая функция для запросов
export async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token && !endpoint.includes('/auth/')) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      clearToken();
      throw new Error('Не авторизован. Войдите снова.');
    }
    const errorText = await response.text();
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  // Если ответ пустой (204 No Content)
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return {} as T;
  }

  return response.json();
}

// ==================== AUTH ====================

export const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
  const response = await fetchApi<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
  setToken(response.token);
  return response;
};

export const register = async (credentials: LoginRequest): Promise<LoginResponse> => {
  return fetchApi<LoginResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
};

// ==================== NETWORKS ====================

export const getNetworks = async (): Promise<NeuralNetwork[]> => {
  return fetchApi<NeuralNetwork[]>('/api/admin/networks');
};

export const getNetwork = async (id: string): Promise<NeuralNetwork> => {
  return fetchApi<NeuralNetwork>(`/api/admin/networks/${id}`);
};

export const createNetwork = async (data: NetworkCreateRequest): Promise<NeuralNetwork> => {
  return fetchApi<NeuralNetwork>('/api/admin/networks', {
    method: 'POST',
    body: JSON.stringify(data),
  });
};

export const updateNetwork = async (id: string, data: NetworkCreateRequest): Promise<NeuralNetwork> => {
  return fetchApi<NeuralNetwork>(`/api/admin/networks/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
};

export const deleteNetwork = async (id: string): Promise<void> => {
  return fetchApi<void>(`/api/admin/networks/${id}`, {
    method: 'DELETE',
  });
};

// ==================== CLIENTS ====================

export const getClients = async (): Promise<ClientApplication[]> => {
  return fetchApi<ClientApplication[]>('/api/admin/clients');
};

export const getClient = async (id: string): Promise<ClientApplication> => {
  return fetchApi<ClientApplication>(`/api/admin/clients/${id}`);
};

export const createClient = async (data: ClientCreateRequest): Promise<ClientApplication> => {
  return fetchApi<ClientApplication>('/api/admin/clients', {
    method: 'POST',
    body: JSON.stringify(data),
  });
};

export const updateClient = async (id: string, data: ClientCreateRequest): Promise<ClientApplication> => {
  return fetchApi<ClientApplication>(`/api/admin/clients/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
};

export const deleteClient = async (id: string): Promise<void> => {
  return fetchApi<void>(`/api/admin/clients/${id}`, {
    method: 'DELETE',
  });
};

export const regenerateApiKey = async (id: string): Promise<ClientApplication> => {
  return fetchApi<ClientApplication>(`/api/admin/clients/${id}/regenerate-key`, {
    method: 'POST',
  });
};

// ==================== LOGS ====================

export const getLogs = async (
  page: number = 0,
  size: number = 50,
  filters?: {
    clientId?: string;
    networkId?: string;
    success?: boolean;
  }
): Promise<{ content: RequestLog[]; totalElements: number; totalPages: number }> => {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });

  if (filters?.clientId) params.append('clientId', filters.clientId);
  if (filters?.networkId) params.append('networkId', filters.networkId);
  if (filters?.success !== undefined) params.append('success', filters.success.toString());

  return fetchApi<{ content: RequestLog[]; totalElements: number; totalPages: number }>(
    `/api/admin/logs?${params.toString()}`
  );
};

// ==================== STATS ====================

export const getStats = async (): Promise<UsageStats> => {
  return fetchApi<UsageStats>('/api/admin/stats');
};

