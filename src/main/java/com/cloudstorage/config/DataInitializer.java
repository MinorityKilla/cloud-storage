package com.cloudstorage.config;

import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.findByLogin("user").isEmpty()) {
            User user = new User();
            user.setLogin("user");
            user.setPassword("password");
            userRepository.save(user);
        }
    }
}