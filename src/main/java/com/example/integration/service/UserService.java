package com.example.integration.service;

import com.example.integration.dto.user.UserAuthResponse;
import com.example.integration.dto.user.UserLoginRequest;
import com.example.integration.dto.user.UserRegisterRequest;
import com.example.integration.model.OAuthState;
import com.example.integration.model.UserAccount;
import com.example.integration.repository.OAuthStateRepository;
import com.example.integration.repository.UserAccountRepository;
import com.example.integration.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final OAuthStateRepository oAuthStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(
            UserAccountRepository userAccountRepository,
            OAuthStateRepository oAuthStateRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userAccountRepository = userAccountRepository;
        this.oAuthStateRepository = oAuthStateRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public UserAuthResponse register(UserRegisterRequest request) {
        validateRegisterRequest(request);
        userAccountRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Пользователь с таким email уже существует");
                });

        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFullName(StringUtils.hasText(request.getFullName()) ? request.getFullName().trim() : null);
        user.setProvider("local");
        user.setProviderSubject(null);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        userAccountRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new UserAuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }

    public UserAuthResponse login(UserLoginRequest request) {
        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("Email и пароль обязательны");
        }
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(request.getEmail().trim().toLowerCase());
        UserAccount user = userOpt.orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));
        if (!user.isActive()) {
            throw new IllegalStateException("Пользователь деактивирован");
        }
        if (!StringUtils.hasText(user.getPasswordHash()) ||
                !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный email или пароль");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new UserAuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }

    public String createOAuthState(String provider, String redirectUri) {
        String state = UUID.randomUUID().toString();
        OAuthState st = new OAuthState(UUID.randomUUID(), state, provider, redirectUri);
        oAuthStateRepository.save(st);
        return state;
    }

    public boolean validateAndConsumeState(String state) {
        Optional<OAuthState> st = oAuthStateRepository.findByState(state);
        if (st.isEmpty()) {
            return false;
        }
        oAuthStateRepository.deleteByState(state);
        return true;
    }

    public UserAccount upsertOauthUser(String provider, String providerSubject, String email, String fullName) {
        Optional<UserAccount> exists = userAccountRepository.findByProviderAndProviderSubject(provider, providerSubject);
        if (exists.isPresent()) {
            UserAccount u = exists.get();
            if (StringUtils.hasText(email)) {
                u.setEmail(email.toLowerCase());
            }
            if (StringUtils.hasText(fullName)) {
                u.setFullName(fullName);
            }
            return userAccountRepository.save(u);
        }
        UserAccount user = new UserAccount();
        user.setProvider(provider);
        user.setProviderSubject(providerSubject);
        if (StringUtils.hasText(email)) {
            user.setEmail(email.toLowerCase());
        } else {
            // Провайдер не вернул email — сгенерируем псевдо-email
            user.setEmail(provider + "_" + providerSubject + "@oauth.local");
        }
        user.setFullName(fullName);
        user.setPasswordHash(null);
        user.setActive(true);
        return userAccountRepository.save(user);
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        if (request == null) throw new IllegalArgumentException("Некорректный запрос");
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("Email обязателен");
        }
        if (!StringUtils.hasText(request.getPassword()) || !StringUtils.hasText(request.getRepeatPassword())) {
            throw new IllegalArgumentException("Пароль и повтор пароля обязательны");
        }
        if (!Objects.equals(request.getPassword(), request.getRepeatPassword())) {
            throw new IllegalArgumentException("Пароль и повтор пароля не совпадают");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не короче 6 символов");
        }
    }
}


