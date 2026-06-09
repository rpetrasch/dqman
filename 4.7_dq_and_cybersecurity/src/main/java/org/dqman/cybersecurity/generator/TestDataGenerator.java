package org.dqman.cybersecurity.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dqman.cybersecurity.model.RawLogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates realistic test log events and produces them to Kafka input topics.
 * Cycles through all attack scenarios defined in the spec to provide signal
 * for both in-session (streaming) and post-session detection rules.
 */
@Component
@ConditionalOnProperty(name = "app.generator.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TestDataGenerator {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.email}")    private String emailTopic;
    @Value("${app.kafka.topics.idp}")      private String idpTopic;
    @Value("${app.kafka.topics.network}")  private String networkTopic;
    @Value("${app.kafka.topics.geolocation}") private String geoTopic;

    private final AtomicInteger scenarioCounter = new AtomicInteger(0);
    private final Random rng = new Random();

    /**
     * Fires on each scheduled tick and cycles through the seven scenario types
     * in round-robin order (index mod 7).  Each invocation produces a self-contained
     * burst of Kafka messages that exercise one detection rule end-to-end.
     */
    @Scheduled(fixedDelayString = "${app.generator.interval-ms}")
    public void generate() {
        int scenario = scenarioCounter.getAndIncrement() % 7;
        switch (scenario) {
            case 0 -> normalSession();
            case 1 -> impossibleTravel();
            case 2 -> sessionHijacking();
            case 3 -> dataExfiltration();
            case 4 -> bruteForce();
            case 5 -> offHoursAccess();
            case 6 -> accountSharing();
        }
    }

    // ----------------------------------------------------------------
    // Normal session — baseline behavior for john.doe in Berlin
    // ----------------------------------------------------------------
    private void normalSession() {
        String sessionId = "SESSION-NORMAL-" + UUID.randomUUID().toString().substring(0, 8);
        String deviceId = "CORP-LAPTOP-001";
        String ip = "10.0.0.42";

        send(idpTopic, loginEvent(sessionId, "john.doe", deviceId, ip, "DE", "SUCCESS"));
        for (int i = 0; i < rng.nextInt(10) + 5; i++) {
            send(emailTopic, emailReadEvent(sessionId, "john.doe", deviceId, ip));
        }
        for (int i = 0; i < rng.nextInt(3); i++) {
            send(emailTopic, attachmentDownloadEvent(sessionId, "john.doe", deviceId, ip, "report-Q" + i + ".pdf"));
        }
        send(idpTopic, logoutEvent(sessionId, "john.doe", deviceId, ip));
        log.info("Generated: normal session {}", sessionId);
    }

    // ----------------------------------------------------------------
    // Impossible travel: Berlin 95 min ago → logout 90 min ago → Bangkok now
    // The 90-minute gap is within the 3-hour impossible-travel threshold.
    // ----------------------------------------------------------------
    private void impossibleTravel() {
        Instant berlinLogin  = Instant.now().minus(95, ChronoUnit.MINUTES);
        Instant berlinLogout = Instant.now().minus(90, ChronoUnit.MINUTES);
        Instant bangkokLogin = Instant.now();

        String sessionBerlin  = "SESSION-TRAVEL-BER-" + UUID.randomUUID().toString().substring(0, 8);
        String sessionBangkok = "SESSION-TRAVEL-BKK-" + UUID.randomUUID().toString().substring(0, 8);

        send(idpTopic, loginEventAt(sessionBerlin, "john.doe", "CORP-LAPTOP-001", "10.0.0.42", "DE", "SUCCESS", berlinLogin));
        send(idpTopic, logoutEventAt(sessionBerlin, "john.doe", "CORP-LAPTOP-001", "10.0.0.42", berlinLogout));

        send(idpTopic, loginEventAt(sessionBangkok, "john.doe", "UNKNOWN-DEV-999", "203.150.1.55", "TH", "SUCCESS", bangkokLogin));
        send(idpTopic, logoutEventAt(sessionBangkok, "john.doe", "UNKNOWN-DEV-999", "203.150.1.55", bangkokLogin.plus(10, ChronoUnit.MINUTES)));
        log.info("Generated: impossible travel scenario sessions {} and {}", sessionBerlin, sessionBangkok);
    }

    // ----------------------------------------------------------------
    // Session hijacking: starts on known laptop, mid-session switches device+IP
    // ----------------------------------------------------------------
    private void sessionHijacking() {
        String sessionId = "SESSION-HIJACK-" + UUID.randomUUID().toString().substring(0, 8);

        send(idpTopic, loginEvent(sessionId, "john.doe", "CORP-LAPTOP-001", "10.0.0.42", "DE", "SUCCESS"));
        send(emailTopic, emailReadEvent(sessionId, "john.doe", "CORP-LAPTOP-001", "10.0.0.42"));

        // Mid-session: unknown device, different IP — classic session hijack signal
        send(emailTopic, emailReadEvent(sessionId, "john.doe", "UNKNOWN-DEV-999", "185.220.101.50"));
        send(emailTopic, attachmentDownloadEvent(sessionId, "john.doe", "UNKNOWN-DEV-999", "185.220.101.50", "salary-data.xlsx"));
        log.info("Generated: session hijacking scenario {}", sessionId);
    }

    // ----------------------------------------------------------------
    // Data exfiltration: 60 attachment downloads + suspicious SMTP relay
    // Triggers SUSPICIOUS_SMTP_RELAY (streaming) and EXCESSIVE_DOWNLOADS (post-session).
    // ----------------------------------------------------------------
    private void dataExfiltration() {
        String sessionId = "SESSION-EXFIL-" + UUID.randomUUID().toString().substring(0, 8);
        String deviceId = "CORP-LAPTOP-001";
        String ip = "10.0.0.42";

        send(idpTopic, loginEvent(sessionId, "john.doe", deviceId, ip, "DE", "SUCCESS"));
        for (int i = 0; i < 60; i++) {
            send(emailTopic, attachmentDownloadEvent(sessionId, "john.doe", deviceId, ip, "confidential-" + i + ".pdf"));
        }
        send(networkTopic, smtpRelayEvent(sessionId, "john.doe", ip, "mail.attacker-domain.ru"));
        send(idpTopic, logoutEvent(sessionId, "john.doe", deviceId, ip));
        log.info("Generated: data exfiltration scenario {}", sessionId);
    }

    // ----------------------------------------------------------------
    // Brute force: 10+ failed IdP auth events in 2-minute window
    // ----------------------------------------------------------------
    private void bruteForce() {
        String sessionId = "SESSION-BRUTE-" + UUID.randomUUID().toString().substring(0, 8);
        String ip = "185.220.101.42";

        for (int i = 0; i < 12; i++) {
            send(idpTopic, loginEvent(sessionId, "john.doe", "UNKNOWN-DEV-999", ip, "RU", "FAILED"));
        }
        // Eventually succeeds
        send(idpTopic, loginEvent(sessionId, "john.doe", "UNKNOWN-DEV-999", ip, "RU", "SUCCESS"));
        log.info("Generated: brute force scenario {}", sessionId);
    }

    // ----------------------------------------------------------------
    // Off-hours access: login at 03:00 UTC, short session, bulk download
    // ----------------------------------------------------------------
    private void offHoursAccess() {
        String sessionId = "SESSION-OFFHOURS-" + UUID.randomUUID().toString().substring(0, 8);
        Instant loginAt = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS);

        send(idpTopic, loginEventAt(sessionId, "john.doe", "CORP-LAPTOP-001", "192.168.1.10", "DE", "SUCCESS", loginAt));
        for (int i = 0; i < 15; i++) {
            send(emailTopic, attachmentDownloadEventAt(sessionId, "john.doe", "CORP-LAPTOP-001",
                    "192.168.1.10", "report-" + i + ".pdf", loginAt.plus(i * 2L, ChronoUnit.MINUTES)));
        }
        send(idpTopic, logoutEventAt(sessionId, "john.doe", "CORP-LAPTOP-001", "192.168.1.10",
                loginAt.plus(30, ChronoUnit.MINUTES)));
        log.info("Generated: off-hours access scenario {}", sessionId);
    }

    // ----------------------------------------------------------------
    // Account sharing: overlapping sessions, same user, different devices.
    // Session 1: t-15min → t-5min; Session 2: t-10min → now.
    // Overlap window: t-10min → t-5min.  Both CLOSED for post-session detection.
    // ----------------------------------------------------------------
    private void accountSharing() {
        Instant start1  = Instant.now().minus(15, ChronoUnit.MINUTES);
        Instant logout1 = Instant.now().minus(5,  ChronoUnit.MINUTES);
        Instant start2  = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant logout2 = Instant.now();

        String session1 = "SESSION-SHARE-1-" + UUID.randomUUID().toString().substring(0, 8);
        String session2 = "SESSION-SHARE-2-" + UUID.randomUUID().toString().substring(0, 8);

        send(idpTopic, loginEventAt(session1, "john.doe", "CORP-LAPTOP-001", "10.0.0.42", "DE", "SUCCESS", start1));
        send(idpTopic, loginEventAt(session2, "john.doe", "PERSONAL-MOB-001", "192.168.1.10", "DE", "SUCCESS", start2));
        send(emailTopic, emailReadEvent(session1, "john.doe", "CORP-LAPTOP-001", "10.0.0.42"));
        send(emailTopic, emailReadEvent(session2, "john.doe", "PERSONAL-MOB-001", "192.168.1.10"));
        send(idpTopic, logoutEventAt(session1, "john.doe", "CORP-LAPTOP-001", "10.0.0.42", logout1));
        send(idpTopic, logoutEventAt(session2, "john.doe", "PERSONAL-MOB-001", "192.168.1.10", logout2));
        log.info("Generated: account sharing scenario {} and {}", session1, session2);
    }

    // ----------------------------------------------------------------
    // Event builders
    // ----------------------------------------------------------------

    private RawLogEvent loginEvent(String sessionId, String username, String deviceId,
                                   String ip, String country, String status) {
        return loginEventAt(sessionId, username, deviceId, ip, country, status, Instant.now());
    }

    private RawLogEvent loginEventAt(String sessionId, String username, String deviceId,
                                     String ip, String country, String status, Instant eventTime) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "LOGIN");
        payload.put("auth_status", status);
        payload.put("country", country);
        payload.put("mfa_used", true);
        return event(idpTopic, sessionId, username, deviceId, ip, eventTime, payload);
    }

    private RawLogEvent logoutEvent(String sessionId, String username, String deviceId, String ip) {
        return logoutEventAt(sessionId, username, deviceId, ip, Instant.now());
    }

    private RawLogEvent logoutEventAt(String sessionId, String username, String deviceId, String ip, Instant eventTime) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "LOGOUT");
        return event(idpTopic, sessionId, username, deviceId, ip, eventTime, payload);
    }

    private RawLogEvent emailReadEvent(String sessionId, String username, String deviceId, String ip) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "EMAIL_READ");
        payload.put("subject", "Q" + rng.nextInt(4) + " Report");
        payload.put("folder", "INBOX");
        return event(emailTopic, sessionId, username, deviceId, ip, Instant.now(), payload);
    }

    private RawLogEvent attachmentDownloadEvent(String sessionId, String username, String deviceId,
                                                String ip, String filename) {
        return attachmentDownloadEventAt(sessionId, username, deviceId, ip, filename, Instant.now());
    }

    private RawLogEvent attachmentDownloadEventAt(String sessionId, String username, String deviceId,
                                                  String ip, String filename, Instant eventTime) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "ATTACHMENT_DOWNLOAD");
        payload.put("filename", filename);
        payload.put("size_bytes", rng.nextInt(5_000_000) + 10_000);
        return event(emailTopic, sessionId, username, deviceId, ip, eventTime, payload);
    }

    private RawLogEvent smtpRelayEvent(String sessionId, String username, String ip, String smtpDest) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "SMTP_RELAY");
        payload.put("smtp_destination", smtpDest);
        payload.put("port", 25);
        return event(networkTopic, sessionId, username, null, ip, Instant.now(), payload);
    }

    private RawLogEvent event(String topic, String sessionId, String username, String deviceId,
                              String ip, Instant eventTime, Map<String, Object> payload) {
        RawLogEvent e = new RawLogEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setSessionId(sessionId);
        e.setUsername(username);
        e.setDeviceId(deviceId);
        e.setIp(ip);
        e.setEventTimeUtc(eventTime);
        e.setIngestionTime(Instant.now());
        e.setPayload(payload);
        return e;
    }

    private void send(String topic, RawLogEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafka.send(topic, event.getEventId(), json);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize test event: {}", ex.getMessage());
        }
    }
}
