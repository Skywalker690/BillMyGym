package com.gymflow.repository;

import com.gymflow.domain.AuditLog;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    @Query("""
        select count(auditLog)
        from AuditLog auditLog
        where auditLog.organization.id = :organizationId
          and auditLog.event = :event
          and auditLog.timestamp >= :fromTime
          and auditLog.timestamp < :toTime
        """)
    long countByOrganizationIdAndEventBetween(
        Long organizationId,
        String event,
        Instant fromTime,
        Instant toTime
    );

    default Page<AuditLog> findGlobalLoginAuditPage(
        AuditEventStatus status,
        Long organizationId,
        Instant fromTime,
        Instant toTime,
        String pattern,
        Pageable pageable
    ) {
        return findAll(globalLoginSpecification(status, organizationId, fromTime, toTime, pattern), pageable);
    }

    default Page<AuditLog> findOrganizationAuditPage(
        Long organizationId,
        AuditActivityScope scope,
        Instant fromTime,
        Instant toTime,
        String pattern,
        Pageable pageable
    ) {
        return findAll(organizationAuditSpecification(organizationId, fromTime, toTime, pattern, List.of(scope)), pageable);
    }

    default Page<AuditLog> findOrganizationAuditPageByScopes(
        Long organizationId,
        Collection<AuditActivityScope> scopes,
        Instant fromTime,
        Instant toTime,
        String pattern,
        Pageable pageable
    ) {
        return findAll(organizationAuditSpecification(organizationId, fromTime, toTime, pattern, scopes), pageable);
    }

    default long countGlobalLoginByStatus(
        AuditEventStatus status,
        Long organizationId,
        Instant fromTime,
        Instant toTime,
        String pattern
    ) {
        return count(globalLoginSpecification(status, organizationId, fromTime, toTime, pattern));
    }

    @Modifying
    @Transactional
    long deleteByTimestampBefore(Instant threshold);

    private static Specification<AuditLog> globalLoginSpecification(
        AuditEventStatus status,
        Long organizationId,
        Instant fromTime,
        Instant toTime,
        String pattern
    ) {
        return (root, query, cb) -> {
            fetchOrganizationForPage(root, query);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("organization")));
            predicates.add(
                cb.or(
                    cb.equal(root.get("activityScope"), AuditActivityScope.GLOBAL_LOGIN),
                    cb.equal(root.get("category"), AuditCategory.AUTHENTICATION)
                )
            );
            if (status != null) {
                predicates.add(cb.equal(root.get("eventStatus"), status));
            }
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (fromTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fromTime));
            }
            if (toTime != null) {
                predicates.add(cb.lessThan(root.get("timestamp"), toTime));
            }
            if (pattern != null) {
                var organizationJoin = root.join("organization", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get("actorName")), pattern),
                        cb.like(cb.lower(root.get("actorEmail")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(organizationJoin.get("name")), pattern)
                    )
                );
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static Specification<AuditLog> organizationAuditSpecification(
        Long organizationId,
        Instant fromTime,
        Instant toTime,
        String pattern,
        Collection<AuditActivityScope> scopes
    ) {
        return (root, query, cb) -> {
            fetchOrganizationForPage(root, query);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            predicates.add(root.get("activityScope").in(scopes));
            if (fromTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fromTime));
            }
            if (toTime != null) {
                predicates.add(cb.lessThan(root.get("timestamp"), toTime));
            }
            if (pattern != null) {
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get("actorName")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("event")), pattern),
                        cb.and(
                            cb.isNotNull(root.get("targetName")),
                            cb.like(cb.lower(root.get("targetName")), pattern)
                        )
                    )
                );
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static void fetchOrganizationForPage(
        jakarta.persistence.criteria.Root<AuditLog> root,
        jakarta.persistence.criteria.CriteriaQuery<?> query
    ) {
        Class<?> resultType = query.getResultType();
        if (Long.class.equals(resultType) || long.class.equals(resultType)) {
            return;
        }
        root.fetch("organization", jakarta.persistence.criteria.JoinType.LEFT);
        query.distinct(true);
    }
}
