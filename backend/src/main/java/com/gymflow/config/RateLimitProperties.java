package com.gymflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Rule api = new Rule(180, 60);
    private Rule auth = new Rule(12, 60);
    private Rule passwordReset = new Rule(5, 300);
    private Rule notifications = new Rule(60, 60);

    @Getter
    @Setter
    public static class Rule {
        private int maxRequests;
        private long windowSeconds;

        public Rule() {
        }

        public Rule(int maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
