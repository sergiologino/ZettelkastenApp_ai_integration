import React, { useEffect, useState } from 'react';
import { getNetworks, createNetwork, updateNetwork, deleteNetwork } from '../api';
import type { NeuralNetwork, NetworkCreateRequest } from '../types';

export const NetworksManager: React.FC = () => {
  const [networks, setNetworks] = useState<NeuralNetwork[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingNetwork, setEditingNetwork] = useState<NeuralNetwork | null>(null);
  const [showApiKey, setShowApiKey] = useState<boolean>(false);

  const [formData, setFormData] = useState<NetworkCreateRequest>({
    name: '',
    displayName: '',
    provider: '',
    networkType: 'chat',
    apiUrl: '',
    apiKey: '',
    modelName: '',
    isActive: true,
    isFree: false,
    priority: 10,
    timeoutSeconds: 60,
    maxRetries: 3,
    requestMapping: {},
    responseMapping: {},
  });

  useEffect(() => {
    loadNetworks();
  }, []);

  const loadNetworks = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getNetworks();
      setNetworks(data.sort((a, b) => a.priority - b.priority));
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingNetwork(null);
    setFormData({
      name: '',
      displayName: '',
      provider: '',
      networkType: 'chat',
      apiUrl: '',
      apiKey: '',
      modelName: '',
      isActive: true,
      isFree: false,
      priority: 10,
      timeoutSeconds: 60,
      maxRetries: 3,
      requestMapping: {},
      responseMapping: {},
    });
    setIsModalOpen(true);
  };

  const handleEdit = (network: NeuralNetwork) => {
    setEditingNetwork(network);
    setFormData({
      name: network.name,
      displayName: network.displayName,
      provider: network.provider,
      networkType: network.networkType,
      apiUrl: network.apiUrl,
      apiKey: '', // не показываем существующий ключ
      modelName: network.modelName,
      isActive: network.isActive,
      isFree: network.isFree,
      priority: network.priority,
      timeoutSeconds: network.timeoutSeconds,
      maxRetries: network.maxRetries,
      requestMapping: {},
      responseMapping: {},
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingNetwork) {
        await updateNetwork(editingNetwork.id, formData);
      } else {
        await createNetwork(formData);
      }
      setIsModalOpen(false);
      loadNetworks();
    } catch (err: any) {
      alert('Ошибка: ' + err.message);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Удалить нейросеть "${name}"?`)) return;
    
    try {
      await deleteNetwork(id);
      loadNetworks();
    } catch (err: any) {
      alert('Ошибка удаления: ' + err.message);
    }
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
        <h2 className="text-2xl font-bold text-gray-900">Управление нейросетями</h2>
        <button
          onClick={handleCreate}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          + Добавить нейросеть
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Список нейросетей */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Название
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Провайдер
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Модель
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Приоритет
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Статус
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Действия
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {networks.map((network) => (
              <tr key={network.id}>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{network.displayName}</div>
                  <div className="text-xs text-gray-500">{network.name}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {network.provider}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {network.modelName}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {network.priority}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span
                    className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      network.isActive
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {network.isActive ? 'Активна' : 'Неактивна'}
                  </span>
                  {network.isFree && (
                    <span className="ml-2 px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                      Free
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                  <button
                    onClick={() => handleEdit(network)}
                    className="text-indigo-600 hover:text-indigo-900"
                  >
                    Редактировать
                  </button>
                  <button
                    onClick={() => handleDelete(network.id, network.displayName)}
                    className="text-red-600 hover:text-red-900"
                  >
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Модальное окно редактирования */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-black opacity-30" onClick={() => setIsModalOpen(false)}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full p-6 max-h-[90vh] overflow-y-auto">
              <h3 className="text-xl font-bold text-gray-900 mb-4">
                {editingNetwork ? 'Редактировать нейросеть' : 'Создать нейросеть'}
              </h3>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Уникальное имя *
                    </label>
                    <input
                      type="text"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      placeholder="openai-gpt4o"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Отображаемое имя *
                    </label>
                    <input
                      type="text"
                      value={formData.displayName}
                      onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      placeholder="OpenAI GPT-4o"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Провайдер *
                    </label>
                    <select
                      value={formData.provider}
                      onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                    >
                      <option value="">Выберите...</option>
                      <option value="openai">OpenAI</option>
                      <option value="yandex">Yandex</option>
                      <option value="anthropic">Anthropic (Claude)</option>
                      <option value="mistral">Mistral</option>
                      <option value="sber">Sber (GigaChat)</option>
                      <option value="whisper">Whisper</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Тип сети *
                    </label>
                    <select
                      value={formData.networkType}
                      onChange={(e) => setFormData({ ...formData, networkType: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                    >
                      <option value="chat">Chat</option>
                      <option value="transcription">Transcription</option>
                      <option value="embedding">Embedding</option>
                    </select>
                  </div>

                  <div className="col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      API URL *
                    </label>
                    <input
                      type="url"
                      value={formData.apiUrl}
                      onChange={(e) => setFormData({ ...formData, apiUrl: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      placeholder="https://api.openai.com/v1"
                    />
                  </div>

                  <div className="col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      API Key {editingNetwork ? '(оставьте пустым, чтобы не менять)' : '*'}
                    </label>
                    <div className="relative">
                      <input
                        type={showApiKey ? 'text' : 'password'}
                        value={formData.apiKey}
                        onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                        required={!editingNetwork}
                        placeholder="sk-..."
                      />
                      <button
                        type="button"
                        onClick={() => setShowApiKey(!showApiKey)}
                        className="absolute right-3 top-2 text-gray-500 hover:text-gray-700"
                      >
                        {showApiKey ? '🙈' : '👁️'}
                      </button>
                    </div>
                  </div>

                  <div className="col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Название модели *
                    </label>
                    <input
                      type="text"
                      value={formData.modelName}
                      onChange={(e) => setFormData({ ...formData, modelName: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      placeholder="gpt-4o"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Приоритет *
                    </label>
                    <input
                      type="number"
                      value={formData.priority}
                      onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value) })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      min="1"
                      max="100"
                    />
                    <p className="text-xs text-gray-500 mt-1">Чем меньше, тем выше приоритет</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Таймаут (сек) *
                    </label>
                    <input
                      type="number"
                      value={formData.timeoutSeconds}
                      onChange={(e) => setFormData({ ...formData, timeoutSeconds: parseInt(e.target.value) })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      min="1"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Макс. повторов *
                    </label>
                    <input
                      type="number"
                      value={formData.maxRetries}
                      onChange={(e) => setFormData({ ...formData, maxRetries: parseInt(e.target.value) })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      required
                      min="0"
                      max="10"
                    />
                  </div>

                  <div className="flex items-center space-x-6">
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={formData.isActive}
                        onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                        className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                      />
                      <span className="ml-2 text-sm text-gray-700">Активна</span>
                    </label>

                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={formData.isFree}
                        onChange={(e) => setFormData({ ...formData, isFree: e.target.checked })}
                        className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                      />
                      <span className="ml-2 text-sm text-gray-700">Бесплатная</span>
                    </label>
                  </div>
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
                    {editingNetwork ? 'Сохранить' : 'Создать'}
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

