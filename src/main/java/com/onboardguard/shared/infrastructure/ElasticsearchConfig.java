package com.onboardguard.shared.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Elasticsearch infrastructure and boundary configuration for OnboardGuard.
 *
 * @EnableElasticsearchRepositories — STRICTLY isolates ES repositories to the
 * watchlist.elasticsearch package.
 * CRITICAL: Prevents Spring from trying to map our
 * PostgreSQL JPA repositories as ES documents.
 *
 * Who uses Elasticsearch in this project (CQRS Pattern):
 * WatchlistSyncListener     → (WRITE) Listens to WatchlistEntryUpdatedEvent and pushes
 * AdvancedScreeningStrategy → (READ) Queries ES during Candidate Onboarding
 *
 * Connection is tuned for Remote/Cloud Enterprise environments:
 * - URL sanitization prevents underlying Java socket errors.
 * - Timeouts ensure the Screening Engine fails fast.
 * - Dynamically adapts to internal VPC clusters (No Auth) vs Secure Cloud clusters (Auth).
 */
@Configuration
// Require both app.elasticsearch.enabled=true AND spring.data.elasticsearch.repositories.enabled=true
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.uris:}")
    private String esUrl;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5000}")
    private long connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30000}")
    private long socketTimeout;

    // --- NEW CLUSTER CONNECTION PARAMETERS ---

    @Value("${spring.elasticsearch.keep-con-alive:true}")
    private boolean keepConAlive;

    @Value("${spring.elasticsearch.ignore-cert-check:true}")
    private boolean ignoreCertCheck;

    @Value("${spring.elasticsearch.use-sticky-connect:true}")
    private boolean useStickyConnect;

    @Override
    public ClientConfiguration clientConfiguration() {

        // Defensive handling: fallback to localhost if missing
        if (esUrl == null || esUrl.isBlank()) {
            log.warn("spring.elasticsearch.uris is not configured; falling back to localhost:9200");
            esUrl = "http://localhost:9200";
        }

        // Normalize URL
        if (esUrl.matches("^:?(\\d+)$") || esUrl.matches("^:+\\d+$")) {
            esUrl = "http://localhost" + (esUrl.startsWith(":") ? esUrl : ":" + esUrl);
        }

        boolean isSecure = esUrl.startsWith("https://");
        boolean hasAuth = (username != null && !username.isBlank());
        String cleanUrl = esUrl.replace("http://", "").replace("https://", "");

        log.info("Connecting to Elasticsearch at: {} (Secure: {}, Auth: {}, KeepAlive: {}, IgnoreCert: {}, Sticky: {})",
                cleanUrl, isSecure, hasAuth, keepConAlive, ignoreCertCheck, useStickyConnect);

        // 1. Initialize the Base Builder
        ClientConfiguration.MaybeSecureClientConfigurationBuilder baseBuilder =
                ClientConfiguration.builder().connectedTo(cleanUrl);

        // 2. Handle SSL & Certificate Overrides
        if (isSecure) {
            if (ignoreCertCheck) {
                try {
                    SSLContext trustingSslContext = createTrustAllSslContext();
                    // Bypass SSL verification (Useful for self-signed Dev/Test clusters)
                    baseBuilder.usingSsl(trustingSslContext, (hostname, session) -> true);
                    log.warn("SSL Certificate validation is DISABLED (ignorecerticheck=true). Do not use in Production!");
                } catch (Exception e) {
                    log.error("Failed to configure trusting SSL Context. Falling back to default SSL.", e);
                    baseBuilder.usingSsl();
                }
            } else {
                baseBuilder.usingSsl(); // Standard secure SSL validation
            }
        }

        // 3. Configure Timeouts
        ClientConfiguration.TerminalClientConfigurationBuilder terminalBuilder = baseBuilder
                .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                .withSocketTimeout(Duration.ofMillis(socketTimeout));

        // 4. Handle Keep-Alive and Sticky Connections via HTTP Headers
        HttpHeaders headers = new HttpHeaders();
        if (keepConAlive) {
            headers.add("Connection", "keep-alive");
            headers.add("Cookie", "ROUTEID=.sticky");
        }
        if (useStickyConnect) {
            // Adds support for session persistence if the cluster sits behind an AWS/GCP Load Balancer
            headers.add("Cookie", "ROUTEID=.sticky");
        }

        if (!headers.isEmpty()) {
            terminalBuilder.withDefaultHeaders(headers);
        }

        // 5. Apply Credentials if provided
        if (hasAuth) {
            terminalBuilder.withBasicAuth(username, password);
        }

        return terminalBuilder.build();
    }

    /**
     * Utility method to generate an SSL Context that blindly trusts all incoming certificates.
     */
    private SSLContext createTrustAllSslContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc;
    }
}