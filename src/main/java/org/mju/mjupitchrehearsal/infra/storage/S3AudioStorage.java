package org.mju.mjupitchrehearsal.infra.storage;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.mju.mjupitchrehearsal.global.exception.BusinessException;
import org.mju.mjupitchrehearsal.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * 녹음 파일을 S3에 저장합니다. {@code STORAGE_TYPE=s3}일 때 사용합니다.
 *
 * <p>자격 증명: 액세스 키 설정({@code AWS_ACCESS_KEY_ID}·{@code AWS_SECRET_ACCESS_KEY})이 있으면 그 키를,
 * 없으면 AWS SDK 기본 체인을 씁니다. 로컬은 {@code .env}의 키, EC2는 서버에 붙인 IAM 역할로 접근하므로 키를 넣지 않습니다.
 * ({@code .env} 값은 Spring 설정으로만 들어오고 OS 환경변수가 아니라서 SDK가 직접 읽지 못하므로 여기서 넘겨 줍니다.)
 * 재생 링크(⑩·⑭)는 presigned URL이라 앱이 S3에서 바로 받고, Range 요청으로 startMs 위치부터 재생할 수 있습니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3AudioStorage implements AudioStorage {

    private static final String CONTENT_TYPE = "audio/mp4";

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3AudioStorage(@Value("${aws.region:ap-northeast-2}") String region,
                          @Value("${aws.s3.bucket:}") String bucket,
                          @Value("${aws.credentials.access-key:}") String accessKey,
                          @Value("${aws.credentials.secret-key:}") String secretKey) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("STORAGE_TYPE=s3 이면 AWS_S3_BUCKET을 설정해야 합니다.");
        }
        Region awsRegion = Region.of(region);
        boolean useKey = StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
        AwsCredentialsProvider credentials = useKey
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.builder().build();
        this.s3 = S3Client.builder().region(awsRegion).credentialsProvider(credentials).build();
        this.presigner = S3Presigner.builder().region(awsRegion).credentialsProvider(credentials).build();
        this.bucket = bucket;
        log.info("[S3Storage] bucket={} region={} credentials={}", bucket, region, useKey ? "access-key" : "default-chain");
    }

    @Override
    public void save(String key, byte[] content) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(CONTENT_TYPE)
                            .build(),
                    RequestBody.fromBytes(content));
            log.info("[S3Storage] saved {} ({} bytes)", key, content.length);
        } catch (SdkException e) {
            log.error("[S3Storage] save failed key={}", key, e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR);
        }
    }

    /** Whisper 전사용. 업로드 파일 이름(확장자)으로 형식을 판단하므로 파일 이름을 함께 돌려줍니다. */
    @Override
    public Resource load(String key) {
        try {
            byte[] bytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()).asByteArray();
            String filename = key.substring(key.lastIndexOf('/') + 1);
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "녹음 파일을 찾을 수 없습니다.");
        } catch (SdkException e) {
            log.error("[S3Storage] load failed key={}", key, e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "녹음 파일을 불러오지 못했습니다.");
        }
    }

    @Override
    public PlaybackUrl createPlaybackUrl(String key, Duration ttl) {
        try {
            PresignedGetObjectRequest presigned = presigner.presignGetObject(b -> b
                    .signatureDuration(ttl)
                    .getObjectRequest(r -> r
                            .bucket(bucket)
                            .key(key)
                            .responseContentType(CONTENT_TYPE)));
            return new PlaybackUrl(presigned.url().toString(),
                    LocalDateTime.ofInstant(presigned.expiration(), ZoneId.systemDefault()));
        } catch (SdkException e) {
            log.error("[S3Storage] presign failed key={}", key, e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "재생 링크를 만들지 못했습니다.");
        }
    }

    @PreDestroy
    void close() {
        presigner.close();
        s3.close();
    }
}
