package com.example.cloudfour.userservice.domain.user.service.command;

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
public class UserProfileCommandService {

    private final UserRepository userRepository;

    @Transactional
    public void updateProfile(UUID userId, String nickname, String number) {
        User u = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (nickname != null && !nickname.isBlank() && !nickname.equals(u.getNickname())) {
            u.changeNickname(nickname);
            log.info("닉네임 변경: userId={}, nickname={}", userId, nickname);
        }
        if (number != null && !number.isBlank() && !number.equals(u.getNumber())) {
            u.changeNumber(number);
            log.info("전화번호 변경: userId={}, number={}", userId, number);
        }
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User u = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        u.softDelete();
        log.info("회원 탈퇴 처리: userId={}", userId);
    }
}
