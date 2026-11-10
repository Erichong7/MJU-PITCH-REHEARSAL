package org.sopt.mjupitchrehearsal.user.service;

import org.sopt.mjupitchrehearsal.global.exception.BusinessException;
import org.sopt.mjupitchrehearsal.global.exception.ErrorCode;
import org.sopt.mjupitchrehearsal.user.entity.User;
import org.sopt.mjupitchrehearsal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 요청의 사용자를 돌려줍니다.
 * MVP에서는 Authorization 헤더 없이 항상 테스트 사용자를 사용하고,
 * 로그인(JWT) 도입 후 이 클래스만 바꾸면 됩니다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return userRepository.findByEmail(TestUserInitializer.TEST_EMAIL)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
