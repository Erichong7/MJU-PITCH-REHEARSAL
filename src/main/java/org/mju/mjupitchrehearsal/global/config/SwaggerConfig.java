package org.mju.mjupitchrehearsal.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("피치리허설 API")
                        .description("발표 녹음 → 구간 피드백 → 구간 재연습 → 1차·2차 비교")
                        .version("v1"))
                .servers(List.of(new Server().url("/").description("현재 서버")));
    }
}
