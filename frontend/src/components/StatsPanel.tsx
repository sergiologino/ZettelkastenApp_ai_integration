import React, { useEffect, useState } from 'react';
import { getStats } from '../api';
import type { UsageStats } from '../types';

export const StatsPanel: React.FC = () => {
  const [stats, setStats] = useState<UsageStats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getStats();
      setStats(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
        Ошибка загрузки статистики: {error}
      </div>
    );
  }

  if (!stats) {
    return <div className="text-center text-gray-500">Нет данных</div>;
  }

  const successRate = stats.totalRequests > 0
    ? ((stats.successfulRequests / stats.totalRequests) * 100).toFixed(1)
    : '0';

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">Статистика использования</h2>
        <button
          onClick={loadStats}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          Обновить
        </button>
      </div>

      {/* Основные метрики */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="text-sm font-medium text-gray-500">Всего запросов</div>
          <div className="mt-2 text-3xl font-bold text-gray-900">{stats.totalRequests}</div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="text-sm font-medium text-gray-500">Успешных</div>
          <div className="mt-2 text-3xl font-bold text-green-600">{stats.successfulRequests}</div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="text-sm font-medium text-gray-500">Ошибок</div>
          <div className="mt-2 text-3xl font-bold text-red-600">{stats.failedRequests}</div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="text-sm font-medium text-gray-500">Успешность</div>
          <div className="mt-2 text-3xl font-bold text-indigo-600">{successRate}%</div>
        </div>
      </div>

      {/* Токены */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="text-sm font-medium text-gray-500 mb-2">Использовано токенов</div>
        <div className="text-3xl font-bold text-gray-900">
          {stats.totalTokensUsed.toLocaleString()}
        </div>
      </div>

      {/* Запросы по нейросетям */}
      {Object.keys(stats.requestsByNetwork).length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Запросы по нейросетям</h3>
          <div className="space-y-3">
            {Object.entries(stats.requestsByNetwork)
              .sort(([, a], [, b]) => b - a)
              .map(([network, count]) => (
                <div key={network} className="flex justify-between items-center">
                  <div className="flex items-center space-x-3">
                    <div className="w-3 h-3 bg-indigo-600 rounded-full"></div>
                    <span className="text-sm font-medium text-gray-700">{network}</span>
                  </div>
                  <span className="text-sm font-bold text-gray-900">{count}</span>
                </div>
              ))}
          </div>
        </div>
      )}

      {/* Запросы по клиентам */}
      {Object.keys(stats.requestsByClient).length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Запросы по клиентам</h3>
          <div className="space-y-3">
            {Object.entries(stats.requestsByClient)
              .sort(([, a], [, b]) => b - a)
              .map(([client, count]) => (
                <div key={client} className="flex justify-between items-center">
                  <div className="flex items-center space-x-3">
                    <div className="w-3 h-3 bg-green-600 rounded-full"></div>
                    <span className="text-sm font-medium text-gray-700">{client}</span>
                  </div>
                  <span className="text-sm font-bold text-gray-900">{count}</span>
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
};

