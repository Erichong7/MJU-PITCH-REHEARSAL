package org.sopt.mjupitchrehearsal.user.service;

import org.sopt.mjupitchrehearsal.user.entity.User;
import org.sopt.mjupitchrehearsal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MVP는 로그인 없이 테스트 사용자 1명으로 고정합니다.
 * 서버가 켜질 때 테스트 사용자가 없으면 만듭니다. (빈 DB에서는 id=1)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestUserInitializer implements ApplicationRunner {

    static final String TEST_EMAIL = "test@pitch-rehearsal.dev";

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(TEST_EMAIL)
                        .nickname("테스트 사용자")
                        .build()));
        log.info("Test user ready: id={}", user.getId());
    }
}
