// Тест для проверки DELETE эндпойнта клиентов
// Выполните этот код в консоли браузера на странице ai-integration

async function testDeleteClient() {
  const token = localStorage.getItem('ai_admin_token');
  
  if (!token) {
    console.error('Токен не найден');
    return;
  }
  
  // Сначала получим список клиентов
  const clientsResponse = await fetch('http://localhost:8091/api/admin/clients', {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  
  if (!clientsResponse.ok) {
    console.error('Ошибка получения клиентов:', clientsResponse.status);
    return;
  }
  
  const clients = await clientsResponse.json();
  console.log('Клиенты:', clients);
  
  if (clients.length === 0) {
    console.log('Нет клиентов для тестирования');
    return;
  }
  
  // Найдем клиента "test"
  const testClient = clients.find(c => c.name === 'test');
  if (!testClient) {
    console.log('Клиент "test" не найден');
    return;
  }
  
  console.log('Найден клиент test:', testClient);
  
  // Попробуем удалить клиента
  const deleteResponse = await fetch(`http://localhost:8091/api/admin/clients/${testClient.id}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  
  console.log('Статус удаления:', deleteResponse.status);
  console.log('Заголовки ответа:', [...deleteResponse.headers.entries()]);
  
  if (!deleteResponse.ok) {
    const errorText = await deleteResponse.text();
    console.error('Ошибка удаления:', errorText);
  } else {
    console.log('Клиент успешно удален');
  }
}

// Запустить тест
testDeleteClient();
