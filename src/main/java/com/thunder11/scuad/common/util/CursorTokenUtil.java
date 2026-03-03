package com.thunder11.scuad.common.util;

import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.thunder11.scuad.jobposting.domain.type.JobStatus;

@Component
public class CursorTokenUtil {

    @Value("${jwt.secret:default-secret-key-must-be-changed-in-production}")
    private String secretKey;

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String VERSION = "v1";

    public String createToken(Long id, LocalDate endDate, JobStatus status) {
        if (id == null || endDate == null || status == null) {
            return null;
        }
        long epochMills = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String payload = String.format("%s|%d|%s|%s", VERSION, id, epochMills, status.name());
        String signature = generateHmac(payload);
        String rawToken = payload + ":" + signature;

        return Base64.getEncoder().withoutPadding().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    public CursorData decodeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String decodedToken = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decodedToken.split(":");
            if (parts.length != 2) throw new IllegalArgumentException("커서 토큰 형식이 잘못되었습니다.");
            String payload = parts[0];
            String signature = parts[1];

            if (!generateHmac(payload).equals(signature)) {
                throw new SecurityException("커서 토큰이 변조되었습니다.");
            }
            String[] dataParts = payload.split("\\|");
            if ("v1".equals(dataParts[0])) {
                long id = Long.parseLong(dataParts[1]);
                long epochMillis = Long.parseLong(dataParts[2]);
                LocalDate endDate = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
                JobStatus status = JobStatus.valueOf(dataParts[3]);

                return new CursorData(id, endDate, status);
            } else {
                throw new IllegalArgumentException("지원하지 않는 버전입니다.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("타당하지 않은 커서 토큰입니다.");
        }
    }

    private String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC을 생성하는데 실패했습니다.",e);
        }
    }

    public static class CursorData {
        public Long cursorId;
        public LocalDate cursorEndDate;
        public JobStatus cursorStatus;

        public CursorData(Long cursorId, LocalDate cursorEndDate, JobStatus cursorStatus) {
            this.cursorId = cursorId;
            this.cursorEndDate = cursorEndDate;
            this.cursorStatus = cursorStatus;
        }
    }
}
