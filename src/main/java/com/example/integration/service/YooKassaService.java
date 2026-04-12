package com.example.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Сервис для интеграции с ЮKassa (YooKassa)
 */
@Service
public class YooKassaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${yookassa.shop.id:}")
    private String shopId;

    @Value("${yookassa.secret.key:}")
    private String secretKey;

    @Value("${yookassa.api.url:https://api.yookassa.ru/v3}")
    private String apiUrl;

    @Value("${oauth.frontend.base:https://sergiologino-ai-integration-front-cd2e.twc1.net}")
    private String frontendUrl;

    public YooKassaService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Создать платеж через ЮKassa
     */
    public PaymentResult createPayment(String transactionId, BigDecimal amount, String currency, 
                                     String description, String userEmail) {
        try {
            String url = apiUrl + "/payments";
            
            Map<String, Object> requestData = new HashMap<>();
            
            // Сумма платежа
            Map<String, Object> amountData = new HashMap<>();
            amountData.put("value", amount.toString());
            amountData.put("currency", currency);
            requestData.put("amount", amountData);
            
            // Описание платежа
            requestData.put("description", description);
            
            // Чек для ФНС (обязательно для России)
            if (userEmail != null && !userEmail.isEmpty()) {
                Map<String, Object> receipt = new HashMap<>();
                
                Map<String, String> customer = new HashMap<>();
                customer.put("email", userEmail);
                receipt.put("customer", customer);
                
                List<Map<String, Object>> items = new ArrayList<>();
                Map<String, Object> item = new HashMap<>();
                item.put("description", description);
                item.put("quantity", "1");
                
                Map<String, String> itemAmount = new HashMap<>();
                itemAmount.put("value", amount.setScale(2).toString());
                itemAmount.put("currency", currency);
                item.put("amount", itemAmount);
                
                item.put("vat_code", 1); // 1 = без НДС
                item.put("payment_subject", "service");
                item.put("payment_mode", "full_prepayment");
                
                items.add(item);
                receipt.put("items", items);
                requestData.put("receipt", receipt);
            }
            
            // Параметры подтверждения
            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("type", "redirect");
            confirmation.put("return_url", frontendUrl + "/payment/success?transactionId=" + transactionId);
            requestData.put("confirmation", confirmation);
            
            requestData.put("capture", true);
            
            // Metadata для идентификации
            Map<String, String> metadata = new HashMap<>();
            metadata.put("transactionId", transactionId);
            metadata.put("userEmail", userEmail);
            requestData.put("metadata", metadata);

            HttpHeaders headers = createHeaders();
            headers.set("Idempotence-Key", UUID.randomUUID().toString());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                String paymentId = responseJson.get("id").asText();
                String paymentUrl = null;
                
                if (responseJson.has("confirmation") && responseJson.get("confirmation").has("confirmation_url")) {
                    paymentUrl = responseJson.get("confirmation").get("confirmation_url").asText();
                }
                
                return new PaymentResult(true, paymentId, paymentUrl, "Платеж создан успешно", response.getBody());
            } else {
                return new PaymentResult(false, null, null, 
                    "Ошибка HTTP: " + response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new PaymentResult(false, null, null, 
                "Ошибка создания платежа: " + e.getMessage(), null);
        }
    }

    /**
     * Проверить статус платежа
     */
    public PaymentStatusResult getPaymentStatus(String paymentId) {
        try {
            String url = apiUrl + "/payments/" + paymentId;

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                String status = responseJson.get("status").asText();
                String amountValue = responseJson.get("amount").get("value").asText();
                
                return new PaymentStatusResult(true, status, amountValue, null, response.getBody());
            } else {
                return new PaymentStatusResult(false, null, null, 
                    "Ошибка HTTP: " + response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            return new PaymentStatusResult(false, null, null, 
                "Ошибка получения статуса: " + e.getMessage(), null);
        }
    }

    /**
     * Валидация webhook от ЮKassa
     */
    public WebhookValidationResult validateWebhook(String requestBody) {
        try {
            JsonNode webhookData = objectMapper.readTree(requestBody);
            
            String eventType = webhookData.get("event").asText();
            JsonNode paymentObject = webhookData.get("object");
            String paymentId = paymentObject.get("id").asText();
            String status = paymentObject.get("status").asText();
            
            String transactionId = null;
            if (paymentObject.has("metadata") && paymentObject.get("metadata").has("transactionId")) {
                transactionId = paymentObject.get("metadata").get("transactionId").asText();
            }

            return new WebhookValidationResult(true, paymentId, transactionId, status, eventType, null, requestBody);

        } catch (Exception e) {
            return new WebhookValidationResult(false, null, null, null, null,
                "Ошибка валидации webhook: " + e.getMessage(), requestBody);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String auth = shopId + ":" + secretKey;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        
        return headers;
    }

    public static class PaymentResult {
        private final boolean success;
        private final String paymentId;
        private final String paymentUrl;
        private final String message;
        private final String rawResponse;

        public PaymentResult(boolean success, String paymentId, String paymentUrl, 
                           String message, String rawResponse) {
            this.success = success;
            this.paymentId = paymentId;
            this.paymentUrl = paymentUrl;
            this.message = message;
            this.rawResponse = rawResponse;
        }

        public boolean isSuccess() { return success; }
        public String getPaymentId() { return paymentId; }
        public String getPaymentUrl() { return paymentUrl; }
        public String getMessage() { return message; }
        public String getRawResponse() { return rawResponse; }
    }

    public static class PaymentStatusResult {
        private final boolean success;
        private final String status;
        private final String amount;
        private final String message;
        private final String rawResponse;

        public PaymentStatusResult(boolean success, String status, String amount, 
                                 String message, String rawResponse) {
            this.success = success;
            this.status = status;
            this.amount = amount;
            this.message = message;
            this.rawResponse = rawResponse;
        }

        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public String getAmount() { return amount; }
        public String getMessage() { return message; }
        public String getRawResponse() { return rawResponse; }
    }

    public static class WebhookValidationResult {
        private final boolean success;
        private final String paymentId;
        private final String transactionId;
        private final String status;
        private final String eventType;
        private final String error;
        private final String rawData;

        public WebhookValidationResult(boolean success, String paymentId, String transactionId,
                                     String status, String eventType, String error, String rawData) {
            this.success = success;
            this.paymentId = paymentId;
            this.transactionId = transactionId;
            this.status = status;
            this.eventType = eventType;
            this.error = error;
            this.rawData = rawData;
        }

        public boolean isSuccess() { return success; }
        public String getPaymentId() { return paymentId; }
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getEventType() { return eventType; }
        public String getError() { return error; }
        public String getRawData() { return rawData; }
    }
}

