import React, { useEffect, useState } from 'react';
import { getClients } from '../api';
import { getNetworks } from '../api';
import type { ClientApplication, NeuralNetwork } from '../types';

// Типы для управления доступом
interface ClientNetworkAccess {
  id: string;
  clientId: string;
  clientName: string;
  networkId: string;
  networkDisplayName: string;
  networkProvider: string;
  networkType: string;
  dailyRequestLimit: number | null;
  monthlyRequestLimit: number | null;
  createdAt: string;
  updatedAt: string;
}

interface GrantAccessRequest {
  clientId: string;
  networkId: string;
  dailyRequestLimit: number | null;
  monthlyRequestLimit: number | null;
}

interface AccessStats {
  totalAccesses: number;
  accessesWithLimits: number;
  unlimitedAccesses: number;
}

export const NetworkAccessManager: React.FC = () => {
  const [accesses, setAccesses] = useState<ClientNetworkAccess[]>([]);
  const [clients, setClients] = useState<ClientApplication[]>([]);
  const [networks, setNetworks] = useState<NeuralNetwork[]>([]);
  const [stats, setStats] = useState<AccessStats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingAccess, setEditingAccess] = useState<ClientNetworkAccess | null>(null);

  const [formData, setFormData] = useState<GrantAccessRequest>({
    clientId: '',
    networkId: '',
    dailyRequestLimit: null,
    monthlyRequestLimit: null,
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [accessesData, clientsData, networksData, statsData] = await Promise.all([
        fetchAccesses(),
        getClients(),
        getNetworks(),
        fetchStats(),
      ]);
      
      setAccesses(accessesData);
      setClients(clientsData);
      setNetworks(networksData);
      setStats(statsData);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchAccesses = async (): Promise<ClientNetworkAccess[]> => {
    const response = await fetch('/api/admin/access', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error(`Ошибка загрузки доступов: ${response.status}`);
    }
    
    return response.json();
  };

  const fetchStats = async (): Promise<AccessStats> => {
    const response = await fetch('/api/admin/access/stats', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error(`Ошибка загрузки статистики: ${response.status}`);
    }
    
    return response.json();
  };

  const handleGrantAccess = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      const response = await fetch('/api/admin/access', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        throw new Error(`Ошибка предоставления доступа: ${response.status}`);
      }

      setIsModalOpen(false);
      loadData();
    } catch (err: any) {
      alert('Ошибка: ' + err.message);
    }
  };

  const handleRevokeAccess = async (accessId: string) => {
    if (!confirm('Отозвать доступ? Это действие нельзя отменить.')) return;
    
    try {
      const response = await fetch(`/api/admin/access/${accessId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Ошибка отзыва доступа: ${response.status}`);
      }

      loadData();
    } catch (err: any) {
      alert('Ошибка: ' + err.message);
    }
  };

  const handleCreate = () => {
    setEditingAccess(null);
    setFormData({
      clientId: '',
      networkId: '',
      dailyRequestLimit: null,
      monthlyRequestLimit: null,
    });
    setIsModalOpen(true);
  };

  const handleEdit = (access: ClientNetworkAccess) => {
    setEditingAccess(access);
    setFormData({
      clientId: access.clientId,
      networkId: access.networkId,
      dailyRequestLimit: access.dailyRequestLimit,
      monthlyRequestLimit: access.monthlyRequestLimit,
    });
    setIsModalOpen(true);
  };

  const getLimitsDescription = (access: ClientNetworkAccess): string => {
    if (!access.dailyRequestLimit && !access.monthlyRequestLimit) {
      return 'Неограниченно';
    }
    
    const parts = [];
    if (access.dailyRequestLimit) {
      parts.push(`${access.dailyRequestLimit} дневных`);
    }
    if (access.monthlyRequestLimit) {
      parts.push(`${access.monthlyRequestLimit} месячных`);
    }
    
    return parts.join(', ');
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">Управление доступом клиентов к нейросетям</h2>
        <button
          onClick={handleCreate}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          + Предоставить доступ
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Статистика */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-blue-100 rounded-lg">
                <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">Всего доступов</p>
                <p className="text-2xl font-semibold text-gray-900">{stats.totalAccesses}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-yellow-100 rounded-lg">
                <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">С лимитами</p>
                <p className="text-2xl font-semibold text-gray-900">{stats.accessesWithLimits}</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className="p-2 bg-green-100 rounded-lg">
                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">Неограниченных</p>
                <p className="text-2xl font-semibold text-gray-900">{stats.unlimitedAccesses}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Список доступов */}
      <div className="grid gap-4">
        {accesses.map((access) => (
          <div key={access.id} className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <div className="flex items-center space-x-3">
                  <h3 className="text-lg font-semibold text-gray-900">
                    {access.clientName} → {access.networkDisplayName}
                  </h3>
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                    {access.networkProvider}
                  </span>
                  <span className="px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">
                    {access.networkType}
                  </span>
                </div>
                
                <div className="mt-2 text-sm text-gray-600">
                  <p><strong>Лимиты:</strong> {getLimitsDescription(access)}</p>
                  <p><strong>Создан:</strong> {new Date(access.createdAt).toLocaleString('ru-RU')}</p>
                  <p><strong>Обновлен:</strong> {new Date(access.updatedAt).toLocaleString('ru-RU')}</p>
                </div>
              </div>

              <div className="flex flex-col space-y-2 ml-4">
                <button
                  onClick={() => handleEdit(access)}
                  className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm transition-colors"
                >
                  Редактировать
                </button>
                <button
                  onClick={() => handleRevokeAccess(access.id)}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm transition-colors"
                >
                  Отозвать
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {accesses.length === 0 && (
        <div className="text-center py-12">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-900">Нет доступов</h3>
          <p className="mt-1 text-sm text-gray-500">Начните с предоставления доступа клиенту к нейросети.</p>
        </div>
      )}

      {/* Модальное окно предоставления доступа */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-black opacity-30" onClick={() => setIsModalOpen(false)}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
              <h3 className="text-xl font-bold text-gray-900 mb-4">
                {editingAccess ? 'Редактировать доступ' : 'Предоставить доступ'}
              </h3>

              <form onSubmit={handleGrantAccess} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Клиент *
                  </label>
                  <select
                    value={formData.clientId}
                    onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                    required
                  >
                    <option value="">Выберите клиента</option>
                    {clients.map((client) => (
                      <option key={client.id} value={client.id}>
                        {client.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Нейросеть *
                  </label>
                  <select
                    value={formData.networkId}
                    onChange={(e) => setFormData({ ...formData, networkId: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                    required
                  >
                    <option value="">Выберите нейросеть</option>
                    {networks.map((network) => (
                      <option key={network.id} value={network.id}>
                        {network.displayName} ({network.provider})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Дневной лимит запросов
                  </label>
                  <input
                    type="number"
                    min="1"
                    value={formData.dailyRequestLimit || ''}
                    onChange={(e) => setFormData({ 
                      ...formData, 
                      dailyRequestLimit: e.target.value ? parseInt(e.target.value) : null 
                    })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Оставьте пустым для неограниченного доступа"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Месячный лимит запросов
                  </label>
                  <input
                    type="number"
                    min="1"
                    value={formData.monthlyRequestLimit || ''}
                    onChange={(e) => setFormData({ 
                      ...formData, 
                      monthlyRequestLimit: e.target.value ? parseInt(e.target.value) : null 
                    })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Оставьте пустым для неограниченного доступа"
                  />
                </div>

                <div className="flex justify-end space-x-3 pt-4 border-t">
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                  >
                    Отмена
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                  >
                    {editingAccess ? 'Сохранить' : 'Предоставить доступ'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
