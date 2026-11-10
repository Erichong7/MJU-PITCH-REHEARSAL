# MJU-PITCH-REHEARSAL

발표·면접 답변을 녹음하면 AI가 고칠 곳을 짚어 주고, 그 구간만 다시 연습하게 해 주는 **피치리허설** 서버입니다.

## 실행

```bash
cp .env.example .env   # 값 채우기
./gradlew bootRun      # 기본 프로필: local (H2 인메모리 DB)
```

- Swagger: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pitch`)

## 기술 결정

- **DB:** 로컬은 H2(MySQL 모드), 배포는 AWS RDS MySQL (`prod` 프로필, `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`)
- **AI:** OpenAI GPT (피드백·비교), 키는 `OPENAI_API_KEY` 하나로 사용. 구현 전까지는 `LLM_PROVIDER=fake`

## 패키지 구조 / 담당

| 패키지 | 담당 | 내용 |
| --- | --- | --- |
| `global` | 이수영 | 공통 응답, 에러 처리, Swagger 설정 |
| `practice` | 이수영 | ① 연습 만들기 |
| `infra/llm` | 이수영 | AI 연결, 프롬프트 |
| `attempt` | 홍정우 | 녹음 업로드·상태·전사·지표 (④⑤⑦⑧) |
| `feedback` | 홍정우 | 피드백 조회·재생 링크 (⑨⑩) |
| `retry` | 홍정우 | 재연습 업로드·비교 결과 (⑬⑭) |
| `infra/storage`, `infra/stt` | 홍정우 | S3 저장, Whisper |

남의 패키지는 직접 고치지 않고 주인에게 요청합니다.

## 브랜치 / 커밋 규칙

- 브랜치: `{type}/#{이슈번호}-{설명}` (예: `feat/#2-common-response`)
- 커밋: `{type}: 내용` — `feat`, `fix`, `setting`, `refactor`, `docs`, `chore`
- PR은 작게, 상대 개발자 확인 후 머지
