import React, { useEffect, useState } from 'react';
import { getClients, createClient, updateClient, deleteClient, regenerateApiKey } from '../api';
import type { ClientApplication, ClientCreateRequest } from '../types';

export const ClientsManager: React.FC = () => {
  const [clients, setClients] = useState<ClientApplication[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingClient, setEditingClient] = useState<ClientApplication | null>(null);
  const [copiedKey, setCopiedKey] = useState<string>('');

  const [formData, setFormData] = useState<ClientCreateRequest>({
    name: '',
    description: '',
    isActive: true,
  });

  useEffect(() => {
    loadClients();
  }, []);

  const loadClients = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getClients();
      setClients(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingClient(null);
    setFormData({
      name: '',
      description: '',
      isActive: true,
    });
    setIsModalOpen(true);
  };

  const handleEdit = (client: ClientApplication) => {
    setEditingClient(client);
    setFormData({
      name: client.name,
      description: client.description,
      isActive: client.isActive,
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingClient) {
        await updateClient(editingClient.id, formData);
      } else {
        await createClient(formData);
      }
      setIsModalOpen(false);
      loadClients();
    } catch (err: any) {
      alert('Ошибка: ' + err.message);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Удалить клиента "${name}"?`)) return;
    
    try {
      console.log(`🔍 [Frontend] Попытка удаления клиента ${name} с ID: ${id}`);
      await deleteClient(id);
      console.log(`✅ [Frontend] Клиент ${name} успешно удален`);
      loadClients();
      alert(`Клиент "${name}" успешно удален!`);
    } catch (err: any) {
      console.error('❌ [Frontend] Ошибка удаления клиента:', err);
      alert('Ошибка удаления: ' + err.message);
    }
  };

  const handleRegenerateKey = async (id: string, name: string) => {
    if (!confirm(`Перегенерировать API ключ для "${name}"? Старый ключ перестанет работать!`)) return;
    
    try {
      await regenerateApiKey(id);
      loadClients();
      alert('API ключ успешно перегенерирован!');
    } catch (err: any) {
      alert('Ошибка: ' + err.message);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    setTimeout(() => setCopiedKey(''), 2000);
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
        <h2 className="text-2xl font-bold text-gray-900">Управление клиентскими приложениями</h2>
        <button
          onClick={handleCreate}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          + Добавить клиента
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Список клиентов */}
      <div className="grid gap-6">
        {clients.map((client) => (
          <div key={client.id} className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-start">
              <div className="flex-1">
                <div className="flex items-center space-x-3">
                  <h3 className="text-lg font-semibold text-gray-900">{client.name}</h3>
                  <span
                    className={`px-2 py-1 text-xs font-semibold rounded-full ${
                      client.isActive
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {client.isActive ? 'Активен' : 'Неактивен'}
                  </span>
                </div>
                <p className="text-sm text-gray-600 mt-1">{client.description}</p>
                
                <div className="mt-4 space-y-2">
                  <div>
                    <label className="text-xs font-medium text-gray-500">API Key:</label>
                    <div className="flex items-center space-x-2 mt-1">
                      <code className="flex-1 px-3 py-2 bg-gray-100 rounded text-sm font-mono">
                        {client.apiKey}
                      </code>
                      <button
                        onClick={() => copyToClipboard(client.apiKey)}
                        className="px-3 py-2 bg-indigo-100 text-indigo-700 rounded hover:bg-indigo-200 text-sm font-medium transition-colors"
                      >
                        {copiedKey === client.apiKey ? '✓ Скопировано' : '📋 Копировать'}
                      </button>
                    </div>
                  </div>

                  <div className="text-xs text-gray-500">
                    <div>ID: {client.id}</div>
                    <div>Создан: {new Date(client.createdAt).toLocaleString('ru-RU')}</div>
                    <div>Обновлён: {new Date(client.updatedAt).toLocaleString('ru-RU')}</div>
                  </div>
                </div>
              </div>

              <div className="flex flex-col space-y-2 ml-4">
                <button
                  onClick={() => handleEdit(client)}
                  className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 text-sm transition-colors"
                >
                  Редактировать
                </button>
                <button
                  onClick={() => handleRegenerateKey(client.id, client.name)}
                  className="px-4 py-2 bg-yellow-600 text-white rounded hover:bg-yellow-700 text-sm transition-colors"
                >
                  Новый ключ
                </button>
                <button
                  onClick={() => handleDelete(client.id, client.name)}
                  className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm transition-colors"
                >
                  Удалить
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Модальное окно редактирования */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-black opacity-30" onClick={() => setIsModalOpen(false)}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
              <h3 className="text-xl font-bold text-gray-900 mb-4">
                {editingClient ? 'Редактировать клиента' : 'Создать клиента'}
              </h3>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Название *
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                    required
                    placeholder="noteapp"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Описание *
                  </label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                    required
                    rows={3}
                    placeholder="AltaNote application"
                  />
                </div>

                <div className="flex items-center">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                      className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                    />
                    <span className="ml-2 text-sm text-gray-700">Активен</span>
                  </label>
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
                    {editingClient ? 'Сохранить' : 'Создать'}
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

