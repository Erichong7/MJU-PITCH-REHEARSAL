package org.mju.mjupitchrehearsal.infra.storage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 내 컴퓨터 디스크에 저장하는 임시 저장소. S3가 준비되기 전까지 사용합니다.
 * 재생 링크는 S3 presigned URL처럼 만료 시각과 서명을 붙여 {@code /api/v1/local-audio}로 제공합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalAudioStorage implements AudioStorage {

    private static final String HMAC = "HmacSHA256";

    private final Path root;
    private final String publicBaseUrl;
    private final byte[] signingKey;

    public LocalAudioStorage(@Value("${storage.local.dir:./uploads}") String dir,
                             @Value("${storage.local.public-base-url:http://localhost:8080}") String publicBaseUrl,
                             @Value("${storage.local.signing-secret:}") String signingSecret) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.signingKey = StringUtils.hasText(signingSecret)
                ? signingSecret.getBytes(StandardCharsets.UTF_8)
                : randomKey();
    }

    @Override
    public void save(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            log.info("[LocalStorage] saved {} ({} bytes)", target, content.length);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public Resource load(String key) {
        Path target = resolve(key);
        if (!Files.exists(target)) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "녹음 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(target);
    }

    @Override
    public PlaybackUrl createPlaybackUrl(String key, Duration ttl) {
        Instant expires = Instant.now().plus(ttl);
        long expiresEpoch = expires.getEpochSecond();
        String url = "%s/api/v1/local-audio?key=%s&expires=%d&signature=%s".formatted(
                publicBaseUrl,
                URLEncoder.encode(key, StandardCharsets.UTF_8),
                expiresEpoch,
                sign(key, expiresEpoch));
        return new PlaybackUrl(url, LocalDateTime.ofInstant(expires, ZoneId.systemDefault()));
    }

    /** 재생 링크의 서명과 만료를 확인합니다. */
    public boolean verify(String key, long expiresEpoch, String signature) {
        if (key == null || signature == null || Instant.now().getEpochSecond() > expiresEpoch) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(key, expiresEpoch).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String key, long expiresEpoch) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(signingKey, HMAC));
            byte[] raw = mac.doFinal((key + "|" + expiresEpoch).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return target;
    }

    /** 서명 키를 설정하지 않으면 서버를 켤 때마다 새로 만듭니다 (재시작하면 이전 링크는 무효). */
    private static byte[] randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
