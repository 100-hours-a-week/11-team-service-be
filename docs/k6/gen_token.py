"""
[테스트용 JWT 생성 스크립트]

목적: k6 부하 테스트 시 필요한 Bearer 토큰을 로컬 JWT_SECRET으로 생성
사용: python3 gen_token.py

의존성: pip install PyJWT
"""
import jwt
import time
import sys

# ── 설정 ────────────────────────────────────────────────────────────
# application.yml fallback 기본값 사용 (로컬 .env에 별도 값이 있다면 그 값으로 교체)
JWT_SECRET = "default-jwt-secret-key-for-development-only-min-256-bits"
USER_ID = 1            # V2 시드 데이터의 HOST 유저 (userId=1)
ROLE = "USER"
EXPIRATION_SEC = 3600  # 1시간
# ────────────────────────────────────────────────────────────────────

now = int(time.time())
payload = {
    "sub": str(USER_ID),
    "role": ROLE,
    "iat": now,
    "exp": now + EXPIRATION_SEC,
}

token = jwt.encode(payload, JWT_SECRET, algorithm="HS256")
print(f"\n✅ Bearer 토큰 생성 완료 (userId={USER_ID})")
print(f"\n{token}\n")
print("→ 아래 k6 스크립트의 BEARER_TOKEN 변수에 붙여넣으세요.")
