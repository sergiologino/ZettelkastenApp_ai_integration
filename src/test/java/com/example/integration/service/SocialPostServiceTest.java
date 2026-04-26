package com.example.integration.service;

import com.example.integration.dto.SocialPostRequestDTO;
import com.example.integration.dto.SocialPostResponseDTO;
import com.example.integration.model.ClientApplication;
import com.example.integration.model.ExternalUser;
import com.example.integration.model.RequestLog;
import com.example.integration.repository.ExternalUserRepository;
import com.example.integration.repository.RequestLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class SocialPostServiceTest {

    @Mock
    private ExternalUserRepository externalUserRepository;

    @Mock
    private RequestLogRepository requestLogRepository;

    private MockRestServiceServer server;
    private SocialPostService service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        service = new SocialPostService(restTemplate, externalUserRepository, requestLogRepository);
    }

    @Test
    void publishesTelegramPostWithoutPersistingCredentialValues() {
        ClientApplication client = client("client-a");
        ExternalUser user = user(client, "external-user-1");
        SocialPostRequestDTO request = telegramRequest();

        when(externalUserRepository.findByClientAppAndExternalUserId(client, "external-user-1"))
            .thenReturn(Optional.of(user));
        when(requestLogRepository.save(any(RequestLog.class)))
            .thenAnswer(invocation -> {
                RequestLog log = invocation.getArgument(0);
                if (log.getId() == null) {
                    log.setId(UUID.randomUUID());
                }
                return log;
            });

        server.expect(requestTo("https://api.telegram.org/bottelegram-token/sendMessage"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(withSuccess("""
                {
                  "ok": true,
                  "result": {
                    "message_id": 42
                  }
                }
                """, MediaType.APPLICATION_JSON));

        SocialPostResponseDTO response = service.publish(client, request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getPlatform()).isEqualTo("telegram");
        assertThat(response.getProviderPostId()).isEqualTo("42");

        ArgumentCaptor<RequestLog> logCaptor = ArgumentCaptor.forClass(RequestLog.class);
        verify(requestLogRepository, atLeastOnce()).save(logCaptor.capture());
        Map<String, Object> savedPayload = logCaptor.getAllValues().get(0).getRequestPayload();
        assertThat(savedPayload).containsEntry("credentialKeys", request.getCredentials().keySet());
        assertThat(savedPayload.toString()).doesNotContain("telegram-token");
    }

    @Test
    void publishesTelegramPhotoAttachmentWithoutPersistingFileContent() {
        ClientApplication client = client("client-a");
        ExternalUser user = user(client, "external-user-1");
        SocialPostRequestDTO request = telegramPhotoRequest();

        when(externalUserRepository.findByClientAppAndExternalUserId(client, "external-user-1"))
            .thenReturn(Optional.of(user));
        when(requestLogRepository.save(any(RequestLog.class)))
            .thenAnswer(invocation -> {
                RequestLog log = invocation.getArgument(0);
                if (log.getId() == null) {
                    log.setId(UUID.randomUUID());
                }
                return log;
            });

        server.expect(requestTo("https://api.telegram.org/bottelegram-token/sendPhoto"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""
                {
                  "ok": true,
                  "result": {
                    "message_id": 43
                  }
                }
                """, MediaType.APPLICATION_JSON));

        SocialPostResponseDTO response = service.publish(client, request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getProviderPostId()).isEqualTo("43");

        ArgumentCaptor<RequestLog> logCaptor = ArgumentCaptor.forClass(RequestLog.class);
        verify(requestLogRepository, atLeastOnce()).save(logCaptor.capture());
        Map<String, Object> savedPayload = logCaptor.getAllValues().get(0).getRequestPayload();
        assertThat(savedPayload.toString()).contains("photo.jpg");
        assertThat(savedPayload.toString()).doesNotContain("aGVsbG8=");
    }

    @Test
    void aggregatesSocialPostStatsByPlatform() {
        RequestLog telegramSuccess = log("social_post:telegram", "success");
        RequestLog xFailed = log("social_post:x", "failed");
        RequestLog aiLog = log("chat", "success");

        when(requestLogRepository.findAll()).thenReturn(List.of(telegramSuccess, xFailed, aiLog));

        var stats = service.getStats();

        assertThat(stats.getTotalPosts()).isEqualTo(2);
        assertThat(stats.getSuccessfulPosts()).isEqualTo(1);
        assertThat(stats.getFailedPosts()).isEqualTo(1);
        assertThat(stats.getPostsByPlatform()).containsEntry("telegram", 1L).containsEntry("x", 1L);
    }

    private SocialPostRequestDTO telegramRequest() {
        SocialPostRequestDTO request = new SocialPostRequestDTO();
        request.setUserId("external-user-1");
        request.setPlatform("telegram");
        request.setText("Hello from test");
        request.setCredentials(Map.of(
            "botToken", "telegram-token",
            "chatId", "chat-1"
        ));
        request.setOptions(Map.of("parseMode", "HTML"));
        return request;
    }

    private SocialPostRequestDTO telegramPhotoRequest() {
        SocialPostRequestDTO request = telegramRequest();
        SocialPostRequestDTO.Attachment attachment = new SocialPostRequestDTO.Attachment();
        attachment.setType("image");
        attachment.setFileName("photo.jpg");
        attachment.setContentType("image/jpeg");
        attachment.setBase64("aGVsbG8=");
        request.setAttachments(List.of(attachment));
        return request;
    }

    private ClientApplication client(String name) {
        ClientApplication client = new ClientApplication();
        client.setId(UUID.randomUUID());
        client.setName(name);
        client.setApiKey("aikey_test");
        client.setIsActive(true);
        return client;
    }

    private ExternalUser user(ClientApplication client, String externalUserId) {
        ExternalUser user = new ExternalUser();
        user.setId(UUID.randomUUID());
        user.setClientApp(client);
        user.setExternalUserId(externalUserId);
        return user;
    }

    private RequestLog log(String requestType, String status) {
        RequestLog log = new RequestLog();
        log.setRequestType(requestType);
        log.setStatus(status);
        log.setRequestPayload(Map.of("test", true));
        return log;
    }
}
