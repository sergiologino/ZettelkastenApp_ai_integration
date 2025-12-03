import React, { useEffect, useState, useMemo } from 'react';
import { getNetworks, createNetwork, updateNetwork, deleteNetwork } from '../api';
import type { NeuralNetwork, NetworkCreateRequest } from '../types';

const normalizeMapping = (value: unknown): Record<string, any> => {
  if (!value) return {};
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      return typeof parsed === 'object' && parsed !== null ? parsed : {};
    } catch {
      return {};
    }
  }
  if (typeof value === 'object') {
    return value as Record<string, any>;
  }
  return {};
};

const buildEmptyForm = (): NetworkCreateRequest => ({
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
  connectionInstruction: null,
  costPerTokenRub: null,
  wordsPerToken: null,
  secondsPerToken: null,
});

const buildFormFromNetwork = (network: NeuralNetwork): NetworkCreateRequest => ({
  ...buildEmptyForm(),
  name: network.name ?? '',
  displayName: network.displayName ?? '',
  provider: network.provider?.toLowerCase() ?? network.name?.split('-')?.[0] ?? '',
  networkType: network.networkType ?? 'chat',
  apiUrl: network.apiUrl ?? '',
  modelName: network.modelName ?? '',
  isActive: network.isActive,
  isFree: network.isFree,
  priority: network.priority ?? 10,
  timeoutSeconds: network.timeoutSeconds ?? 60,
  maxRetries: network.maxRetries ?? 3,
  requestMapping: normalizeMapping(network.requestMapping),
  responseMapping: normalizeMapping(network.responseMapping),
  connectionInstruction: network.connectionInstruction ?? null,
  costPerTokenRub: network.costPerTokenRub ?? null,
  wordsPerToken: network.wordsPerToken ?? null,
  secondsPerToken: network.secondsPerToken ?? null,
});

export const NetworksManager: React.FC = () => {
  const [networks, setNetworks] = useState<NeuralNetwork[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingNetwork, setEditingNetwork] = useState<NeuralNetwork | null>(null);
  const [showApiKey, setShowApiKey] = useState<boolean>(false);

  const [formData, setFormData] = useState<NetworkCreateRequest>(() => buildEmptyForm());

  const providerSuggestions = [
    { value: 'openai', label: 'OpenAI' },
    { value: 'yandex', label: 'Yandex GPT' },
    { value: 'anthropic', label: 'Anthropic (Claude)' },
    { value: 'mistral', label: 'Mistral AI' },
    { value: 'sber', label: 'Sber GigaChat' },
    { value: 'whisper', label: 'Whisper' },
    { value: 'deepseek', label: 'DeepSeek' },
    { value: 'qwen', label: 'Qwen' },
    { value: 'pollinations', label: 'Pollinations' },
    { value: 'stability', label: 'Stability AI' },
    { value: 'leonardo', label: 'Leonardo AI' },
    { value: 'midjourney', label: 'Midjourney' },
    { value: 'sora2', label: 'OpenAI Sora 2' },
    { value: 'nanobanana', label: 'Nano Banana' },
    { value: 'runwayml', label: 'Runway ML' },
    { value: 'pika', label: 'Pika Labs' },
  ];

  const networkTypeLabels: Record<string, string> = {
    chat: 'Чат-боты и ассистенты',
    transcription: 'Транскрибация / ASR',
    embedding: 'Эмбеддинги',
    image_generation: 'Генерация изображений',
    video_generation: 'Генерация видео',
    other: 'Другие',
  };

  const groupedNetworks = useMemo(() => {
    const groups: Record<string, NeuralNetwork[]> = {};
    networks.forEach((network) => {
      const type = network.networkType || 'other';
      if (!groups[type]) {
        groups[type] = [];
      }
      groups[type].push(network);
    });
    Object.values(groups).forEach((items) => items.sort((a, b) => a.priority - b.priority));
    const typeOrder = ['chat', 'transcription', 'embedding', 'image_generation', 'video_generation', 'other'];
    return Object.entries(groups).sort((a, b) => {
      const idxA = typeOrder.indexOf(a[0]) !== -1 ? typeOrder.indexOf(a[0]) : typeOrder.length;
      const idxB = typeOrder.indexOf(b[0]) !== -1 ? typeOrder.indexOf(b[0]) : typeOrder.length;
      if (idxA === idxB) {
        return a[0].localeCompare(b[0]);
      }
      return idxA - idxB;
    });
  }, [networks]);

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
    setFormData(buildEmptyForm());
    setShowApiKey(false);
    setIsModalOpen(true);
  };

  const handleEdit = (network: NeuralNetwork) => {
    setEditingNetwork(network);
    setFormData(buildFormFromNetwork(network));
    setShowApiKey(false);
    setIsModalOpen(true);
  };

  useEffect(() => {
    if (!isModalOpen) {
      return;
    }
    if (editingNetwork) {
      setFormData(buildFormFromNetwork(editingNetwork));
    } else {
      setFormData(buildEmptyForm());
    }
  }, [editingNetwork, isModalOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload: NetworkCreateRequest = {
        ...formData,
        provider: formData.provider.trim(),
        networkType: formData.networkType,
        requestMapping: formData.requestMapping ?? {},
        responseMapping: formData.responseMapping ?? {},
      };
      if (editingNetwork) {
        await updateNetwork(editingNetwork.id, payload);
      } else {
        await createNetwork(payload);
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

  // Примеры запросов и ответов для разных провайдеров
  const getExamples = useMemo(() => {
    const examples: Record<string, { request: any; response: any }> = {
      openai: {
        request: {
          model: 'gpt-4o',
          messages: [
            { role: 'user', content: 'Напиши короткое стихотворение про кота' }
          ],
          temperature: 0.7,
          max_tokens: 150
        },
        response: {
          id: 'chatcmpl-123',
          object: 'chat.completion',
          created: 1677652288,
          choices: [
            {
              index: 0,
              message: {
                role: 'assistant',
                content: 'Усатый кот на окне сидит,\nНа улицу мечтательно глядит...'
              },
              finish_reason: 'stop'
            }
          ],
          usage: {
            prompt_tokens: 15,
            completion_tokens: 25,
            total_tokens: 40
          }
        }
      },
      yandex: {
        request: {
          modelUri: 'gpt://folder_id/yandexgpt-lite/latest',
          completionOptions: {
            stream: false,
            temperature: 0.6,
            maxTokens: 2000
          },
          messages: [
            { role: 'user', text: 'Напиши короткое стихотворение про кота' }
          ]
        },
        response: {
          result: {
            alternatives: [
              {
                message: {
                  role: 'assistant',
                  text: 'Усатый кот на окне сидит,\nНа улицу мечтательно глядит...'
                },
                status: 'ALTERNATIVE_STATUS_FINAL'
              }
            ],
            usage: {
              inputTextTokens: 15,
              completionTokens: 25,
              totalTokens: 40
            },
            modelVersion: '06.12.2023'
          }
        }
      },
      anthropic: {
        request: {
          model: 'claude-3-opus-20240229',
          max_tokens: 1024,
          messages: [
            { role: 'user', content: 'Напиши короткое стихотворение про кота' }
          ]
        },
        response: {
          id: 'msg_01XFDUDYJgAACzvnptvVoYEL',
          type: 'message',
          role: 'assistant',
          content: [
            {
              type: 'text',
              text: 'Усатый кот на окне сидит,\nНа улицу мечтательно глядит...'
            }
          ],
          model: 'claude-3-opus-20240229',
          stop_reason: 'end_turn',
          usage: {
            input_tokens: 15,
            output_tokens: 25
          }
        }
      },
      mistral: {
        request: {
          model: 'mistral-large-latest',
          messages: [
            { role: 'user', content: 'Напиши короткое стихотворение про кота' }
          ],
          temperature: 0.7,
          max_tokens: 150
        },
        response: {
          id: 'cmpl-123',
          object: 'chat.completion',
          created: 1677652288,
          model: 'mistral-large-latest',
          choices: [
            {
              index: 0,
              message: {
                role: 'assistant',
                content: 'Усатый кот на окне сидит,\nНа улицу мечтательно глядит...'
              },
              finish_reason: 'stop'
            }
          ],
          usage: {
            prompt_tokens: 15,
            completion_tokens: 25,
            total_tokens: 40
          }
        }
      },
      sber: {
        request: {
          model: 'GigaChat',
          messages: [
            { role: 'user', content: 'Напиши короткое стихотворение про кота' }
          ],
          temperature: 0.7,
          max_tokens: 512
        },
        response: {
          choices: [
            {
              message: {
                role: 'assistant',
                content: 'Усатый кот на окне сидит,\nНа улицу мечтательно глядит...'
              },
              index: 0,
              finish_reason: 'stop'
            }
          ],
          created: 1677652288,
          model: 'GigaChat',
          usage: {
            prompt_tokens: 15,
            completion_tokens: 25,
            total_tokens: 40
          }
        }
      },
      whisper: {
        request: {
          file: '<audio file binary>',
          model: 'whisper-1',
          language: 'ru',
          response_format: 'json'
        },
        response: {
          text: 'Привет, это пример транскрипции аудио файла.'
        }
      },
      pollinations: {
        request: {
          prompt: 'Synthwave city skyline at sunset, neon reflections',
          ratio: '1:1',
          style: 'vivid'
        },
        response: {
          id: 'poll-789',
          status: 'completed',
          output: [{ url: 'https://cdn.pollinations.ai/image/abc123.png' }]
        }
      },
      stability: {
        request: {
          model: 'sd3-turbo',
          prompt: 'Ultra realistic macro photo of dew on leaves',
          negative_prompt: 'text, logo',
          output_format: 'png',
          aspect_ratio: '3:2'
        },
        response: {
          id: 'sd3_123',
          status: 'finished',
          result: [{ image_base64: '<base64>' }]
        }
      },
      leonardo: {
        request: {
          prompt: 'Concept art of cyberpunk samurai in the rain',
          presetStyle: 'CREATIVE',
          imageDimensions: '1024x1024'
        },
        response: {
          generationId: 'gen_abc',
          status: 'COMPLETE',
          generated_images: [{ url: 'https://cdn.leonardo.ai/gen_abc.png' }]
        }
      },
      midjourney: {
        request: {
          prompt: 'Astronaut discovering an ancient temple on Mars --ar 16:9 --stylize 50',
          webhookUrl: 'https://yourapp.com/mj/webhook'
        },
        response: {
          jobId: 'mj-job-123',
          status: 'queued',
          progress: 0
        }
      },
      sora2: {
        request: {
          prompt: '4K aerial cinematic of morning fog over Tokyo with neon billboards turning on',
          duration_seconds: 12,
          resolution: '1920x1080',
          fps: 24
        },
        response: {
          job_id: 'sora_job_001',
          status: 'processing',
          eta_seconds: 45
        }
      },
      nanobanana: {
        request: {
          prompt: 'Cartoon banana astronaut dancing on Saturn rings',
          soundtrack: 'funk',
          duration: 8
        },
        response: {
          jobId: 'nano_42',
          status: 'completed',
          videoUrl: 'https://cdn.nanobanana.ai/videos/nano_42.mp4'
        }
      },
      runwayml: {
        request: {
          prompt: 'Slow motion shot of ink swirling in water with golden particles',
          model: 'gen-3-alpha',
          duration: 10,
          ratio: '9:16'
        },
        response: {
          id: 'runway_77',
          state: 'completed',
          assets: [{ url: 'https://runwayml.com/asset/runway_77.mp4' }]
        }
      },
      pika: {
        request: {
          prompt: 'Steampunk robot cat making coffee, cinematic lighting',
          camera_motion: 'orbit',
          duration: 6
        },
        response: {
          request_id: 'pika_req_8',
          status: 'rendering',
          preview: 'https://cdn.pika.art/previews/pika_req_8.gif'
        }
      }
    };

    const normalizedProvider = (formData.provider || '').toLowerCase().trim();
    const isOpenAiImage = normalizedProvider === 'openai' && formData.networkType === 'image_generation';

    if (isOpenAiImage) {
      return {
        request: {
          prompt: 'Minimalist poster of a fox made of origami triangles',
          size: '1024x1024',
          quality: 'high'
        },
        response: {
          data: [{ url: 'https://api.openai.com/v1/images/generated/dalle.png' }],
          created: 1700000000
        }
      };
    }

    const providerExample = normalizedProvider ? examples[normalizedProvider] : null;
    if (providerExample) {
      return providerExample;
    }

    if (formData.networkType === 'image_generation') {
      return {
        request: {
          prompt: 'Генерируй продуктовый рендер смартфона на белом фоне',
          size: '1024x1024',
          steps: 30
        },
        response: {
          status: 'queued',
          estimateSeconds: 25,
          outputs: []
        }
      };
    }

    if (formData.networkType === 'video_generation') {
      return {
        request: {
          prompt: 'Кинематографичный пролёт над футуристическим городом на рассвете',
          duration: 10,
          ratio: '16:9'
        },
        response: {
          jobId: 'video-demo-001',
          status: 'processing',
          etaSeconds: 60
        }
      };
    }
    
    return {
      request: { message: 'Выберите провайдера для отображения примера' },
      response: { result: 'Пример ответа появится после выбора провайдера' }
    };
  }, [formData.provider, formData.networkType]);

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
      {groupedNetworks.length === 0 ? (
        <div className="bg-white rounded-lg shadow px-6 py-10 text-center text-gray-500">
          Нейросети пока не добавлены.
        </div>
      ) : (
        <div className="space-y-6">
          {groupedNetworks.map(([type, items]) => {
            const label = networkTypeLabels[type] || `Другие (${type})`;
            return (
              <section key={type} className="space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-xs uppercase tracking-wide text-gray-400">Тип сети</p>
                    <h3 className="text-xl font-semibold text-gray-900">{label}</h3>
                  </div>
                  <span className="px-3 py-1 text-sm bg-gray-100 rounded-full text-gray-600">
                    {items.length} шт.
                  </span>
                </div>
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
                      {items.map((network) => (
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
              </section>
            );
          })}
        </div>
      )}

      {/* Модальное окно редактирования */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4 py-6">
            <div className="fixed inset-0 bg-black opacity-30" onClick={() => setIsModalOpen(false)}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full flex flex-col max-h-[90vh]">
              {/* Header */}
              <div className="px-6 pt-6 pb-4 border-b">
                <h3 className="text-xl font-bold text-gray-900">
                  {editingNetwork ? 'Редактировать нейросеть' : 'Создать нейросеть'}
                </h3>
              </div>

              {/* Scrollable content */}
              <div className="flex-1 overflow-y-auto px-6 py-4">
                <form onSubmit={handleSubmit} className="space-y-4" id="network-form">
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
                    <input
                      type="text"
                      list="provider-options"
                      value={formData.provider}
                      onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="openai / stability / midjourney ..."
                      required
                    />
                    <datalist id="provider-options">
                      {providerSuggestions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </datalist>
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
                      <option value="image_generation">Image generation</option>
                      <option value="video_generation">Video generation</option>
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
                      API Key {editingNetwork ? '(необязательно)' : '*'}
                      <span 
                        className="ml-2 text-blue-500 cursor-help" 
                        title={editingNetwork 
                          ? "При редактировании: если оставите поле пустым, API ключ останется прежним.&#10;Введите новый ключ только если хотите его изменить.&#10;&#10;Ключ хранится в БД в зашифрованном виде и не отображается из соображений безопасности."
                          : "API ключ для доступа к нейросети.&#10;Получите его в личном кабинете провайдера.&#10;&#10;Примеры:&#10;• OpenAI: sk-proj-...&#10;• Yandex: t1.9eu...&#10;• Anthropic: sk-ant-..."}
                      >
                        ℹ️
                      </span>
                    </label>
                    <div className="relative">
                      <input
                        type={showApiKey ? 'text' : 'password'}
                        value={formData.apiKey}
                        onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-indigo-500 focus:border-indigo-500"
                        required={!editingNetwork}
                        placeholder={editingNetwork ? "Оставьте пустым, чтобы не менять" : "sk-..."}
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
                      <span 
                        className="ml-2 text-blue-500 cursor-help" 
                        title="Для Yandex GPT используйте формат: gpt://folder_id/model/latest &#10;где folder_id - это ID вашего каталога в Yandex Cloud&#10;&#10;Пример: gpt://b1g123abc456def789gh/yandexgpt-lite/latest"
                      >
                        ℹ️
                      </span>
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

                  {/* Примеры запроса и ответа */}
                  <div className="col-span-2 border-t pt-4 mt-2">
                    <h4 className="text-sm font-semibold text-gray-700 mb-3">
                      📋 Примеры структуры запроса и ответа
                    </h4>
                    
                    <div className="space-y-4">
                      {/* Пример запроса */}
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Пример запроса к API
                        </label>
                        <pre className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-xs overflow-x-auto max-h-48 overflow-y-auto text-left">
                          <code className="text-gray-800 font-mono">
                            {JSON.stringify(getExamples.request, null, 2)}
                          </code>
                        </pre>
                      </div>

                      {/* Пример ответа */}
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Пример ответа от API
                        </label>
                        <pre className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-xs overflow-x-auto max-h-48 overflow-y-auto text-left">
                          <code className="text-gray-800 font-mono">
                            {JSON.stringify(getExamples.response, null, 2)}
                          </code>
                        </pre>
                      </div>
                    </div>
                  </div>
                </div>
                </form>
              </div>

              {/* Fixed footer with buttons */}
              <div className="px-6 py-4 border-t bg-gray-50 rounded-b-lg flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 bg-white"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  form="network-form"
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                >
                  {editingNetwork ? 'Сохранить' : 'Создать'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

