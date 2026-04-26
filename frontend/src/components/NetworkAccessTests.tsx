import React, { useState } from 'react';

interface TestResult {
  name: string;
  status: 'pending' | 'loading' | 'success' | 'error' | 'info';
  message: string;
  data?: string;
}

export const NetworkAccessTests: React.FC = () => {
  const [results, setResults] = useState<TestResult[]>([]);
  const [isRunning, setIsRunning] = useState(false);

  const addResult = (result: TestResult) => {
    setResults(prev => [...prev.filter(r => r.name !== result.name), result]);
  };

  const runAllTests = async () => {
    setIsRunning(true);
    setResults([]);

    const tests = [
      testGetAllAccesses,
      testGetAccessStats,
      testGrantAccess,
      testRevokeAccess,
      testGetClientAccesses,
      testGetNetworkAccesses,
    ];

    for (const test of tests) {
      await test();
      await new Promise(resolve => setTimeout(resolve, 500)); // Небольшая задержка между тестами
    }

    setIsRunning(false);
  };

  const testGetAllAccesses = async () => {
    addResult({ name: 'Получение всех доступов', status: 'loading', message: 'Загружаем все доступы...' });
    
    try {
      const response = await fetch('http://localhost:8091/api/admin/access', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
      });
      
      const data = await response.json();
      
      if (response.ok) {
        addResult({ 
          name: 'Получение всех доступов', 
          status: 'success', 
          message: `✅ Получено ${Array.isArray(data) ? data.length : 0} доступов`,
          data: JSON.stringify(data, null, 2)
        });
      } else {
        addResult({ 
          name: 'Получение всех доступов', 
          status: 'error', 
          message: `❌ Ошибка: ${response.status}`,
          data: JSON.stringify(data, null, 2)
        });
      }
    } catch (error: any) {
      addResult({ 
        name: 'Получение всех доступов', 
        status: 'error', 
        message: `❌ Ошибка сети: ${error.message}`,
        data: error.message
      });
    }
  };

  const testGetAccessStats = async () => {
    addResult({ name: 'Статистика доступов', status: 'loading', message: 'Загружаем статистику...' });
    
    try {
      const response = await fetch('http://localhost:8091/api/admin/access/stats', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
      });
      
      const data = await response.json();
      
      if (response.ok) {
        addResult({ 
          name: 'Статистика доступов', 
          status: 'success', 
          message: `✅ Статистика получена: ${data.totalAccesses} всего, ${data.accessesWithLimits} с лимитами`,
          data: JSON.stringify(data, null, 2)
        });
      } else {
        addResult({ 
          name: 'Статистика доступов', 
          status: 'error', 
          message: `❌ Ошибка: ${response.status}`,
          data: JSON.stringify(data, null, 2)
        });
      }
    } catch (error: any) {
      addResult({ 
        name: 'Статистика доступов', 
        status: 'error', 
        message: `❌ Ошибка сети: ${error.message}`,
        data: error.message
      });
    }
  };

  const testGrantAccess = async () => {
    addResult({ name: 'Предоставление доступа', status: 'loading', message: 'Тестируем предоставление доступа...' });
    
    try {
      // Сначала получаем список клиентов и нейросетей
      const [clientsResponse, networksResponse] = await Promise.all([
        fetch('http://localhost:8091/api/admin/clients', {
          headers: { 'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}` },
        }),
        fetch('http://localhost:8091/api/admin/networks', {
          headers: { 'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}` },
        }),
      ]);

      if (!clientsResponse.ok) {
        addResult({ 
          name: 'Предоставление доступа', 
          status: 'error', 
          message: `❌ Ошибка получения клиентов: ${clientsResponse.status}`,
          data: await clientsResponse.text()
        });
        return;
      }

      if (!networksResponse.ok) {
        addResult({ 
          name: 'Предоставление доступа', 
          status: 'error', 
          message: `❌ Ошибка получения нейросетей: ${networksResponse.status}`,
          data: await networksResponse.text()
        });
        return;
      }

      const clients = await clientsResponse.json();
      const networks = await networksResponse.json();

      if (clients.length === 0 || networks.length === 0) {
        addResult({ 
          name: 'Предоставление доступа', 
          status: 'info', 
          message: '⚠️ Нет клиентов или нейросетей для тестирования',
          data: `Клиентов: ${clients.length}, Нейросетей: ${networks.length}`
        });
        return;
      }

      // Пытаемся предоставить доступ
      const testData = {
        clientId: clients[0].id,
        networkId: networks[0].id,
        dailyRequestLimit: 100,
        monthlyRequestLimit: 1000,
      };

      const response = await fetch('http://localhost:8091/api/admin/access', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
        body: JSON.stringify(testData),
      });

      const data = await response.json();

      if (response.ok) {
        addResult({ 
          name: 'Предоставление доступа', 
          status: 'success', 
          message: `✅ Доступ предоставлен: ${data.clientName} → ${data.networkDisplayName}`,
          data: JSON.stringify(data, null, 2)
        });
      } else {
        addResult({ 
          name: 'Предоставление доступа', 
          status: 'error', 
          message: `❌ Ошибка: ${response.status}`,
          data: JSON.stringify(data, null, 2)
        });
      }
    } catch (error: any) {
      addResult({ 
        name: 'Предоставление доступа', 
        status: 'error', 
        message: `❌ Ошибка сети: ${error.message}`,
        data: error.message
      });
    }
  };

  const testRevokeAccess = async () => {
    addResult({ name: 'Отзыв доступа', status: 'loading', message: 'Тестируем отзыв доступа...' });
    
    try {
      // Сначала получаем список доступов
      const response = await fetch('http://localhost:8091/api/admin/access', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
      });
      
      const accesses = await response.json();
      
      if (!Array.isArray(accesses) || accesses.length === 0) {
        addResult({ 
          name: 'Отзыв доступа', 
          status: 'info', 
          message: '⚠️ Нет доступов для отзыва',
          data: 'Список доступов пуст'
        });
        return;
      }

      // Пытаемся отозвать первый доступ
      const accessToRevoke = accesses[0];
      const revokeResponse = await fetch(`http://localhost:8091/api/admin/access/${accessToRevoke.id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
      });

      if (revokeResponse.ok) {
        addResult({ 
          name: 'Отзыв доступа', 
          status: 'success', 
          message: `✅ Доступ отозван: ${accessToRevoke.clientName} → ${accessToRevoke.networkDisplayName}`,
          data: `Отозван доступ ID: ${accessToRevoke.id}`
        });
      } else {
        const errorData = await revokeResponse.json();
        addResult({ 
          name: 'Отзыв доступа', 
          status: 'error', 
          message: `❌ Ошибка: ${revokeResponse.status}`,
          data: JSON.stringify(errorData, null, 2)
        });
      }
    } catch (error: any) {
      addResult({ 
        name: 'Отзыв доступа', 
        status: 'error', 
        message: `❌ Ошибка сети: ${error.message}`,
        data: error.message
      });
    }
  };

  const testGetClientAccesses = async () => {
    addResult({ name: 'Доступы клиента', status: 'loading', message: 'Получаем доступы клиента...' });
    
    try {
      // Сначала получаем список клиентов
      const clientsResponse = await fetch('http://localhost:8091/api/admin/clients', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}` },
      });
      
      if (!clientsResponse.ok) {
        addResult({ 
          name: 'Доступы клиента', 
          status: 'error', 
          message: `❌ Ошибка получения клиентов: ${clientsResponse.status}`,
          data: await clientsResponse.text()
        });
        return;
      }
      
      const clients = await clientsResponse.json();
      
      if (clients.length === 0) {
        addResult({ 
          name: 'Доступы клиента', 
          status: 'info', 
          message: '⚠️ Нет клиентов для тестирования',
          data: 'Список клиентов пуст'
        });
        return;
      }

      // Получаем доступы первого клиента
      const response = await fetch(`http://localhost:8091/api/admin/access/client/${clients[0].id}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
      });
      
      const data = await response.json();
      
      if (response.ok) {
        addResult({ 
          name: 'Доступы клиента', 
          status: 'success', 
          message: `✅ Получено ${Array.isArray(data) ? data.length : 0} доступов для клиента ${clients[0].name}`,
          data: JSON.stringify(data, null, 2)
        });
      } else {
        addResult({ 
          name: 'Доступы клиента', 
          status: 'error', 
          message: `❌ Ошибка: ${response.status}`,
          data: JSON.stringify(data, null, 2)
        });
      }
    } catch (error: any) {
      addResult({ 
        name: 'Доступы клиента', 
        status: 'error', 
        message: `❌ Ошибка сети: ${error.message}`,
        data: error.message
      });
    }
  };

  const testGetNetworkAccesses = async () => {
    addResult({ name: 'Доступы нейросети', status: 'loading', message: 'Получаем доступы нейросети...' });
    
    try {
      // Сначала получаем список нейросетей
      const networksResponse = await fetch('http://localhost:8091/api/admin/networks', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}` },
      });
      
      if (!networksResponse.ok) {
        addResult({ 
          name: 'Доступы нейросети', 
          status: 'error', 
          message: `❌ Ошибка получения нейросетей: ${networksResponse.status}`,
          data: await networksResponse.text()
        });
        return;
      }
      
      const networks = await networksResponse.json();
      
      if (networks.length === 0) {
        addResult({ 
          name: 'Доступы нейросети', 
          status: 'info', 
          message: '⚠️ Нет нейросетей для тестирования',
          data: 'Список нейросетей пуст'
        });
        return;
      }

      // Получаем доступы первой нейросети
      const response = await fetch(`http://localhost:8091/api/admin/access/network/${networks[0].id}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('ai_admin_token')}`,
        },
      });
      
      const data = await response.json();
      
      if (response.ok) {
        addResult({ 
          name: 'Доступы нейросети', 
          status: 'success', 
          message: `✅ Получено ${Array.isArray(data) ? data.length : 0} доступов для нейросети ${networks[0].displayName}`,
          data: JSON.stringify(data, null, 2)
        });
      } else {
        addResult({ 
          name: 'Доступы нейросети', 
          status: 'error', 
          message: `❌ Ошибка: ${response.status}`,
          data: JSON.stringify(data, null, 2)
        });
      }
    } catch (error: any) {
      addResult({ 
        name: 'Доступы нейросети', 
        status: 'error', 
        message: `❌ Ошибка сети: ${error.message}`,
        data: error.message
      });
    }
  };

  const getStatusIcon = (status: TestResult['status']) => {
    switch (status) {
      case 'loading': return '⏳';
      case 'success': return '✅';
      case 'error': return '❌';
      case 'info': return '⚠️';
      default: return '⏸️';
    }
  };

  const getStatusColor = (status: TestResult['status']) => {
    switch (status) {
      case 'loading': return 'text-blue-600';
      case 'success': return 'text-green-600';
      case 'error': return 'text-red-600';
      case 'info': return 'text-yellow-600';
      default: return 'text-gray-600';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">🧪 Тесты управления доступом</h2>
        <button
          onClick={runAllTests}
          disabled={isRunning}
          className={`px-4 py-2 rounded-lg transition-colors ${
            isRunning 
              ? 'bg-gray-400 text-white cursor-not-allowed' 
              : 'bg-indigo-600 text-white hover:bg-indigo-700'
          }`}
        >
          {isRunning ? '⏳ Запуск тестов...' : '🚀 Запустить все тесты'}
        </button>
      </div>

      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Результаты тестов</h3>
        </div>
        
        <div className="divide-y divide-gray-200">
          {results.length === 0 ? (
            <div className="px-6 py-8 text-center text-gray-500">
              Нажмите "Запустить все тесты" для проверки функциональности управления доступом
            </div>
          ) : (
            results.map((result, index) => (
              <div key={index} className="px-6 py-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <span className="text-lg">{getStatusIcon(result.status)}</span>
                    <div>
                      <h4 className="font-medium text-gray-900">{result.name}</h4>
                      <p className={`text-sm ${getStatusColor(result.status)}`}>
                        {result.message}
                      </p>
                    </div>
                  </div>
                  
                  {result.data && (
                    <details className="text-sm">
                      <summary className="cursor-pointer text-indigo-600 hover:text-indigo-800">
                        Показать данные
                      </summary>
                      <pre className="mt-2 p-3 bg-gray-100 rounded text-xs overflow-auto max-h-40">
                        {result.data}
                      </pre>
                    </details>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h4 className="font-medium text-blue-900 mb-2">📋 Описание тестов:</h4>
        <ul className="text-sm text-blue-800 space-y-1">
          <li><strong>Получение всех доступов</strong> - проверяет GET /api/admin/access</li>
          <li><strong>Статистика доступов</strong> - проверяет GET /api/admin/access/stats</li>
          <li><strong>Предоставление доступа</strong> - проверяет POST /api/admin/access</li>
          <li><strong>Отзыв доступа</strong> - проверяет DELETE /api/admin/access/&#123;id&#125;</li>
          <li><strong>Доступы клиента</strong> - проверяет GET /api/admin/access/client/&#123;id&#125;</li>
          <li><strong>Доступы нейросети</strong> - проверяет GET /api/admin/access/network/&#123;id&#125;</li>
        </ul>
      </div>
    </div>
  );
};
