package com.example.integration.service;

import com.example.integration.factory.NeuralNetworkClient;
import com.example.integration.factory.NeuralNetworkClientFactory;
import com.example.integration.model.AnalysisRequest;
import com.example.integration.model.AnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class IntegrationService {

    private final NeuralNetworkClientFactory clientFactory;
    private final MappingService mappingService;

    public IntegrationService(NeuralNetworkClientFactory clientFactory, MappingService mappingService) {
        this.clientFactory = clientFactory;
        this.mappingService = mappingService;
    }

    public AnalysisResponse analyze(AnalysisRequest request) {
        String neuralNetworkName = request.getNeuralNetwork();
        String requestType = determineRequestType(request);

        // Получаем настройки сети
        NeuralNetworkClient client = clientFactory.getClient(neuralNetworkName);

        // Собираем тело запроса
        Map<String, Object> sourcePayload = Map.of(
                "content", request.getContent(),
                "fileUrl", request.getFileUrl(),
                "fileType", request.getFileType()
        );
        Map<String, Object> targetPayload = mappingService.buildRequestPayload(neuralNetworkName, requestType, sourcePayload);

        // Отправляем запрос
        String rawResponse = client.analyze(targetPayload);

        // Обработка ответа
        AnalysisResponse response = new AnalysisResponse();
        response.setAnnotation("Результат анализа: " + rawResponse);
        response.setTags(new String[]{"тег1", "тег2"});

        return response;
    }

    private String determineRequestType(AnalysisRequest request) {
        if ("audio".equals(request.getFileType())) {
            return "transcription";
        } else if ("image".equals(request.getFileType()) || "pdf".equals(request.getFileType())) {
            return "ocr";
        }
        return "default";
    }


    private String extractTextFromFile(String fileUrl, String fileType) {
        // Реализуйте OCR с помощью Apache Tika
        return "Распознанный текст файла";
    }

    private String transcribeAudio(String fileUrl) {
        // Реализуйте транскрибацию
        return "Транскрибированный текст аудио";
    }

    private AnalysisResponse sendToNeuralNetwork(String content, String neuralNetwork) {
        // Отправка текста на выбранную нейросеть
        AnalysisResponse response = new AnalysisResponse();
        response.setAnnotation("Результат анализа");
        response.setTags(new String[]{"тег1", "тег2"});
        return response;
    }
}
