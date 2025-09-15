package com.example.cloudfour.userservice.domain.user.service.query;


import com.example.cloudfour.userservice.domain.user.converter.UserConverter;
import com.example.cloudfour.userservice.domain.user.dto.UserResponseDTO;
import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.exception.UserErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserException;
import com.example.cloudfour.userservice.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserProfileQueryService {

    private final UserRepository userRepository;

    public UserResponseDTO.MeResponseDTO getMyInfo(UUID userId) {
        User u = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        var dto = UserConverter.toMeResponseDTO(u);
        log.info("내 정보 조회: userId={}", userId);
        return dto;
    }
}