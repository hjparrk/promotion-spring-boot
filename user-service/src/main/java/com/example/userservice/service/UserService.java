package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.entity.UserLoginHistory;
import com.example.userservice.exception.DuplicateUserException;
import com.example.userservice.exception.UnauthorizedAccessException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserLoginHistoryRepository;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("User already exists with the email: " + email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .build();
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email " + email));

        if(!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedAccessException("Invalid Credentials");
        }

        return user;
    }

    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public User updateUser(Integer userId, String name) {
        User user = getUserById(userId);
        user.setName(name);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedAccessException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserLoginHistory> getUserLoginHistory(Integer userId) {
        User user = getUserById(userId);
        return userLoginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
    }
}
