package org.mju.mjupitchrehearsal.attempt.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

/**
 * 구간 안 멈춤 목록을 "startMs:pauseMs,startMs:pauseMs" 문자열 한 칸에 저장합니다.
 *
 * <p>구간은 JPQL bulk delete로 지우기 때문에(재전사), 별도 테이블을 두면 외래 키 때문에 지울 수 없습니다.
 * 문장 하나에 멈춤은 몇 개뿐이라 문자열로 충분합니다.
 */
@Converter
public class InnerPauseListConverter implements AttributeConverter<List<InnerPause>, String> {

    @Override
    public String convertToDatabaseColumn(List<InnerPause> pauses) {
        if (pauses == null || pauses.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (InnerPause p : pauses) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(p.startMs()).append(':').append(p.pauseMs());
        }
        return sb.toString();
    }

    @Override
    public List<InnerPause> convertToEntityAttribute(String value) {
        List<InnerPause> pauses = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return pauses;
        }
        for (String item : value.split(",")) {
            String[] parts = item.split(":");
            pauses.add(new InnerPause(Long.parseLong(parts[0].trim()), Long.parseLong(parts[1].trim())));
        }
        return pauses;
    }
}
