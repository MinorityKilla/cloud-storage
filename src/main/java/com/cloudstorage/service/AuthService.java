package com.cloudstorage.service;

import com.cloudstorage.dto.LoginRequest;
import com.cloudstorage.dto.LoginResponse;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByLogin(request.getLogin());

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            String token = UUID.randomUUID().toString();
            User user = userOpt.get();
            user.setAuthToken(token);
            userRepository.save(user);
            return new LoginResponse(token);
        }

        throw new RuntimeException("Bad credentials");
    }

    public void logout(String token) {
        Optional<User> userOpt = userRepository.findByAuthToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAuthToken(null);
            userRepository.save(user);
        }
    }

    public User getUserByToken(String token) {
        return userRepository.findByAuthToken(token)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
    }
}