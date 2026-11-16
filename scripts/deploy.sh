#!/usr/bin/env bash
# EC2에서 실행: 현재 코드로 이미지를 만들고 서버를 다시 띄웁니다.
# 설정값은 ~/pitch.env (깃에 없음). 수동 배포도 이 스크립트 하나로 됩니다.
set -euo pipefail

APP=pitch
ENV_FILE="$HOME/pitch.env"

cd "$(dirname "$0")/.."

if [ ! -f "$ENV_FILE" ]; then
  echo "설정 파일이 없습니다: $ENV_FILE"
  exit 1
fi

echo "[1/3] 이미지 빌드"
# 빌드가 실패하면 여기서 멈추고, 기존 서버는 그대로 돌아갑니다.
docker build -t "$APP" .

echo "[2/3] 서버 교체"
docker rm -f "$APP" 2>/dev/null || true
mkdir -p "$HOME/uploads"
docker run -d --name "$APP" --restart unless-stopped \
  -p 8080:8080 \
  --env-file "$ENV_FILE" \
  -v "$HOME/uploads:/app/uploads" \
  "$APP"
docker image prune -f > /dev/null

echo "[3/3] 서버가 뜨는지 확인 (최대 3분)"
for _ in $(seq 1 36); do
  if curl -fs http://localhost:8080/v3/api-docs > /dev/null; then
    echo "배포 성공"
    exit 0
  fi
  sleep 5
done

echo "배포 실패: 서버가 응답하지 않습니다. 최근 로그:"
docker logs --tail 100 "$APP"
exit 1
