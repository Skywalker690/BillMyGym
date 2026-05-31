package com.gymflow.service;

import java.util.Collection;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportCacheService {

    private static final String DASHBOARD_REPORTS = "dashboardReports";
    private static final String REVENUE_REPORTS = "revenueReports";

    private final CacheManager cacheManager;

    public void evictOrganization(Long organizationId) {
        if (organizationId == null) {
            return;
        }
        evict(DASHBOARD_REPORTS, organizationId);
        evict(DASHBOARD_REPORTS, "reminders:" + organizationId);
        evict(REVENUE_REPORTS, organizationId);
        evict(REVENUE_REPORTS, "segmentation:" + organizationId);
    }

    public void evictOrganizations(Collection<Long> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return;
        }
        organizationIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .forEach(this::evictOrganization);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
