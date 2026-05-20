package com.gymflow.repository;

import com.gymflow.domain.AuditLog;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    @Query(
        value = """
            select auditLog from AuditLog auditLog
            where auditLog.activityScope = com.gymflow.domain.enums.AuditActivityScope.GLOBAL_LOGIN
              and auditLog.actorRole = 'ORG_ADMIN'
              and (:status is null or auditLog.eventStatus = :status)
              and (:organizationId is null or auditLog.organization.id = :organizationId)
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.actorEmail) like :pattern
                or lower(auditLog.description) like :pattern
                or (auditLog.organization is not null and lower(auditLog.organization.name) like :pattern)
              )
            order by auditLog.timestamp desc
            """,
        countQuery = """
            select count(auditLog) from AuditLog auditLog
            where auditLog.activityScope = com.gymflow.domain.enums.AuditActivityScope.GLOBAL_LOGIN
              and auditLog.actorRole = 'ORG_ADMIN'
              and (:status is null or auditLog.eventStatus = :status)
              and (:organizationId is null or auditLog.organization.id = :organizationId)
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.actorEmail) like :pattern
                or lower(auditLog.description) like :pattern
                or (auditLog.organization is not null and lower(auditLog.organization.name) like :pattern)
              )
            """
    )
    Page<AuditLog> findGlobalLoginAuditPage(
        @Param("status") AuditEventStatus status,
        @Param("organizationId") Long organizationId,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("pattern") String pattern,
        Pageable pageable
    );

    @Query(
        value = """
            select auditLog from AuditLog auditLog
            where auditLog.organization.id = :organizationId
              and auditLog.activityScope = :scope
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.description) like :pattern
                or lower(auditLog.event) like :pattern
                or (auditLog.targetName is not null and lower(auditLog.targetName) like :pattern)
              )
            order by auditLog.timestamp desc
            """,
        countQuery = """
            select count(auditLog) from AuditLog auditLog
            where auditLog.organization.id = :organizationId
              and auditLog.activityScope = :scope
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.description) like :pattern
                or lower(auditLog.event) like :pattern
                or (auditLog.targetName is not null and lower(auditLog.targetName) like :pattern)
              )
            """
    )
    Page<AuditLog> findOrganizationAuditPage(
        @Param("organizationId") Long organizationId,
        @Param("scope") AuditActivityScope scope,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("pattern") String pattern,
        Pageable pageable
    );

    @Query(
        value = """
            select auditLog from AuditLog auditLog
            where auditLog.organization.id = :organizationId
              and auditLog.activityScope in :scopes
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.description) like :pattern
                or lower(auditLog.event) like :pattern
                or (auditLog.targetName is not null and lower(auditLog.targetName) like :pattern)
              )
            order by auditLog.timestamp desc
            """,
        countQuery = """
            select count(auditLog) from AuditLog auditLog
            where auditLog.organization.id = :organizationId
              and auditLog.activityScope in :scopes
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.description) like :pattern
                or lower(auditLog.event) like :pattern
                or (auditLog.targetName is not null and lower(auditLog.targetName) like :pattern)
              )
            """
    )
    Page<AuditLog> findOrganizationAuditPageByScopes(
        @Param("organizationId") Long organizationId,
        @Param("scopes") Collection<AuditActivityScope> scopes,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("pattern") String pattern,
        Pageable pageable
    );

    @Query(
        """
            select count(auditLog) from AuditLog auditLog
            where auditLog.activityScope = com.gymflow.domain.enums.AuditActivityScope.GLOBAL_LOGIN
              and auditLog.actorRole = 'ORG_ADMIN'
              and auditLog.eventStatus = :status
              and (:organizationId is null or auditLog.organization.id = :organizationId)
              and (:fromTime is null or auditLog.timestamp >= :fromTime)
              and (:toTime is null or auditLog.timestamp < :toTime)
              and (
                :pattern is null
                or lower(auditLog.actorName) like :pattern
                or lower(auditLog.actorEmail) like :pattern
                or lower(auditLog.description) like :pattern
                or (auditLog.organization is not null and lower(auditLog.organization.name) like :pattern)
              )
            """
    )
    long countGlobalLoginByStatus(
        @Param("status") AuditEventStatus status,
        @Param("organizationId") Long organizationId,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("pattern") String pattern
    );

    @Modifying
    @Transactional
    long deleteByTimestampBefore(Instant threshold);
}
