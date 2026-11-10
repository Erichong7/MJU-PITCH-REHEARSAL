package org.sopt.mjupitchrehearsal.global.health;

import org.sopt.mjupitchrehearsal.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
public class HealthController {

    @Operation(summary = "서버 상태 확인", description = "배포 후 서버가 떠 있는지 확인합니다.")
    @GetMapping("/api/v1/health")
    public BaseResponse<String> health() {
        return BaseResponse.ok("OK");
    }
}
