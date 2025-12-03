// Типы для AI-интеграционного сервиса

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  expiresIn: number;
}

export interface NeuralNetwork {
  id: string;
  name: string;
  displayName: string;
  provider: string;
  networkType: string;
  apiUrl: string;
  modelName: string;
  isActive: boolean;
  isFree: boolean;
  priority: number;
  timeoutSeconds: number;
  maxRetries: number;
  createdAt: string;
  updatedAt: string;
  requestMapping?: Record<string, any> | null;
  responseMapping?: Record<string, any> | null;
  connectionInstruction?: string | null;
  costPerTokenRub?: number | string | null;
  wordsPerToken?: number | string | null;
  secondsPerToken?: number | string | null;
}

export interface NetworkCreateRequest {
  name: string;
  displayName: string;
  provider: string;
  networkType: string;
  apiUrl: string;
  apiKey: string;
  modelName: string;
  isActive: boolean;
  isFree: boolean;
  priority: number;
  timeoutSeconds: number;
  maxRetries: number;
  requestMapping: Record<string, any>;
  responseMapping: Record<string, any>;
  connectionInstruction?: string | null;
  costPerTokenRub?: number | string | null;
  wordsPerToken?: number | string | null;
  secondsPerToken?: number | string | null;
}

export interface ClientApplication {
  id: string;
  name: string;
  description: string;
  apiKey: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ClientCreateRequest {
  name: string;
  description: string;
  isActive: boolean;
}

export interface RequestLog {
  id: string;
  externalUserId: string;
  neuralNetworkId: string;
  neuralNetworkName: string;
  clientApplicationId: string;
  clientApplicationName: string;
  requestType: string;
  prompt: string;
  response: string;
  success: boolean;
  errorMessage?: string;
  tokensUsed?: number;
  createdAt: string;
}

export interface UsageStats {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  totalTokensUsed: number;
  requestsByNetwork: Record<string, number>;
  requestsByClient: Record<string, number>;
}

export interface ClientNetworkAccess {
  id: string;
  clientId: string;
  clientName: string;
  networkId: string;
  networkDisplayName: string;
  networkProvider: string;
  networkType: string;
  dailyRequestLimit?: number;
  monthlyRequestLimit?: number;
  createdAt: string;
  updatedAt: string;
}

export interface AccessStats {
  totalAccesses: number;
  accessesWithLimits: number;
  unlimitedAccesses: number;
}

export interface GrantAccessRequest {
  clientId: string;
  networkId: string;
  dailyRequestLimit?: number;
  monthlyRequestLimit?: number;
}

export interface NetworkLimit {
  id: string;
  neuralNetworkId: string;
  userType: string;
  limitPeriod: string;
  requestLimit: number;
}

