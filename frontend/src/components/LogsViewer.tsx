import React, { useEffect, useState } from 'react';
import { getLogs } from '../api';
import type { RequestLog } from '../types';

export const LogsViewer: React.FC = () => {
  const [logs, setLogs] = useState<RequestLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [selectedLog, setSelectedLog] = useState<RequestLog | null>(null);

  useEffect(() => {
    loadLogs();
  }, [page]);

  const loadLogs = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getLogs(page, 20);
      setLogs(data.content);
      setTotalPages(data.totalPages);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('ru-RU');
  };

  const truncateText = (text: string, maxLength: number = 100) => {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  if (loading && logs.length === 0) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">Логи запросов</h2>
        <button
          onClick={loadLogs}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          Обновить
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Таблица логов */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Дата
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Клиент
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Нейросеть
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                User ID
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Промпт
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Статус
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Токены
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Действия
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {logs.map((log) => (
              <tr key={log.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {formatDate(log.createdAt)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {log.clientApplicationName}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {log.neuralNetworkName}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {log.externalUserId.substring(0, 8)}...
                </td>
                <td className="px-6 py-4 text-sm text-gray-500 max-w-xs">
                  {truncateText(log.prompt, 60)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span
                    className={`px-2 py-1 text-xs font-semibold rounded-full ${
                      log.success
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {log.success ? 'Успех' : 'Ошибка'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {log.tokensUsed || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <button
                    onClick={() => setSelectedLog(log)}
                    className="text-indigo-600 hover:text-indigo-900"
                  >
                    Детали
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Пагинация */}
      <div className="flex justify-between items-center">
        <div className="text-sm text-gray-700">
          Страница {page + 1} из {totalPages}
        </div>
        <div className="flex space-x-2">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Назад
          </button>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Вперёд
          </button>
        </div>
      </div>

      {/* Модальное окно с деталями лога */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-black opacity-30" onClick={() => setSelectedLog(null)}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full p-6 max-h-[90vh] overflow-y-auto">
              <div className="flex justify-between items-start mb-4">
                <h3 className="text-xl font-bold text-gray-900">Детали запроса</h3>
                <button
                  onClick={() => setSelectedLog(null)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>

              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">Дата</label>
                    <div className="text-sm text-gray-900">{formatDate(selectedLog.createdAt)}</div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">Статус</label>
                    <div>
                      <span
                        className={`px-2 py-1 text-xs font-semibold rounded-full ${
                          selectedLog.success
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {selectedLog.success ? 'Успех' : 'Ошибка'}
                      </span>
                    </div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">Клиент</label>
                    <div className="text-sm text-gray-900">{selectedLog.clientApplicationName}</div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">Нейросеть</label>
                    <div className="text-sm text-gray-900">{selectedLog.neuralNetworkName}</div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">User ID</label>
                    <div className="text-sm text-gray-900 font-mono">{selectedLog.externalUserId}</div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">Токены</label>
                    <div className="text-sm text-gray-900">{selectedLog.tokensUsed || '-'}</div>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 block mb-2">Промпт</label>
                  <div className="bg-gray-50 p-4 rounded border border-gray-200 max-h-48 overflow-y-auto">
                    <pre className="text-sm text-gray-900 whitespace-pre-wrap">{selectedLog.prompt}</pre>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 block mb-2">Ответ</label>
                  <div className="bg-gray-50 p-4 rounded border border-gray-200 max-h-64 overflow-y-auto">
                    <pre className="text-sm text-gray-900 whitespace-pre-wrap">{selectedLog.response}</pre>
                  </div>
                </div>

                {selectedLog.errorMessage && (
                  <div>
                    <label className="text-sm font-medium text-red-500 block mb-2">Ошибка</label>
                    <div className="bg-red-50 p-4 rounded border border-red-200">
                      <pre className="text-sm text-red-700 whitespace-pre-wrap">{selectedLog.errorMessage}</pre>
                    </div>
                  </div>
                )}
              </div>

              <div className="mt-6 flex justify-end">
                <button
                  onClick={() => setSelectedLog(null)}
                  className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700"
                >
                  Закрыть
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

