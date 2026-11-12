package com.eventlab.domain.user.service;

import com.eventlab.domain.user.dto.UserCreateRequest;
import com.eventlab.domain.user.dto.UserResponse;
import com.eventlab.domain.user.mapper.UserMapper;
import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> findAll() {
        return userMapper.findAll();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {

        if(userMapper.countByEmail(request.email()) > 0){
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String passwordHash = passwordEncoder.encode(request.password());

        userMapper.insertUser(request.email(), passwordHash, request.nickname());

        return userMapper.findByEmail(request.email());
    }
}
