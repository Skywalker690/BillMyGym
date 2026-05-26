package com.gymflow.service;

import com.gymflow.api.dto.ReferralDtos.AdminReferralAnalyticsResponse;
import com.gymflow.api.dto.ReferralDtos.ReferralDashboardResponse;
import com.gymflow.api.dto.ReferralDtos.ReferralEntry;
import com.gymflow.api.dto.ReferralDtos.RewardEntry;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.Referral;
import com.gymflow.domain.ReferralReward;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.ReferralStatus;
import com.gymflow.domain.enums.RewardType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.ReferralRepository;
import com.gymflow.repository.ReferralRewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralService {

    private final OrganizationRepository organizationRepository;
    private final ReferralRepository referralRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final PlatformSettingService platformSettingService;

    @Transactional
    public void generateReferralCodeForOrganization(Organization organization) {
        if (organization.getReferralCode() == null) {
            String baseCode = organization.getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (baseCode.length() > 10) {
                baseCode = baseCode.substring(0, 10);
            }
            String uniqueCode = baseCode + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            organization.setReferralCode(uniqueCode);
            organizationRepository.save(organization);
        }
    }

    @Transactional
    public void processReferralSignup(Organization newOrganization, String referralCodeUsed) {
        if (referralCodeUsed == null || referralCodeUsed.isBlank()) {
            return;
        }

        if (!platformSettingService.getSettings().isReferralsEnabled()) {
            log.info("Referral signup ignored because referrals are disabled. Referred org: {}", newOrganization.getId());
            return;
        }

        Organization referrer = organizationRepository.findByReferralCode(referralCodeUsed).orElse(null);
        if (referrer == null) {
            log.warn("Invalid referral code used during signup: {}", referralCodeUsed);
            return;
        }

        if (newOrganization.getOrganizationType() == OrganizationType.DEMO || referrer.getOrganizationType() == OrganizationType.DEMO) {
            log.info("Referral ignored because demo workspaces are not eligible. Referrer: {}, Referred: {}", referrer.getId(), newOrganization.getId());
            return;
        }

        if (referrer.getId().equals(newOrganization.getId()) ||
            referrer.getAdminEmail().equalsIgnoreCase(newOrganization.getAdminEmail()) ||
            referrer.getAdminPhoneNumber().equals(newOrganization.getAdminPhoneNumber())) {
            log.warn("Self-referral or abuse detected. Referrer: {}, Referred: {}", referrer.getId(), newOrganization.getId());
            return; // Anti-abuse check
        }

        Referral referral = new Referral();
        referral.setReferrer(referrer);
        referral.setReferred(newOrganization);
        referral.setReferralCodeUsed(referralCodeUsed);
        referral.setStatus(ReferralStatus.PENDING);
        referralRepository.save(referral);
        log.info("Pending referral created. Referrer: {}, Referred: {}", referrer.getId(), newOrganization.getId());
    }

    @Transactional
    public void fulfillReferralRewards(Organization referredOrg) {
        if (referredOrg.getOrganizationType() == OrganizationType.DEMO) {
            return;
        }
        Referral referral = referralRepository.findByReferred(referredOrg).orElse(null);
        if (referral == null || referral.getStatus() != ReferralStatus.PENDING) {
            return; // Not referred, or already fulfilled/rejected
        }

        Organization referrer = referral.getReferrer();
        if (referrer.getOrganizationType() == OrganizationType.DEMO) {
            referral.setStatus(ReferralStatus.REJECTED);
            referralRepository.save(referral);
            return;
        }
        long successfulReferrals = referralRepository.countByReferrer(referrer);
        long milestone = successfulReferrals + 1; // including this one

        ReferralReward reward = new ReferralReward();
        reward.setOrganization(referrer);

        if (milestone == 6) {
            reward.setRewardType(RewardType.SIX_MONTH_PRO);
            reward.setMonthsRewarded(6);
            referral.setRewardType(RewardType.SIX_MONTH_PRO);
            referrer.setSixMonthRewardClaimed(true);
        } else {
            reward.setRewardType(RewardType.GYM_COIN);
            reward.setAmount(BigDecimal.valueOf(300));
            referral.setRewardType(RewardType.GYM_COIN);
            
            BigDecimal currentBalance = referrer.getGymCoinBalance() != null ? referrer.getGymCoinBalance() : BigDecimal.ZERO;
            BigDecimal earnedBalance = referrer.getTotalCoinsEarned() != null ? referrer.getTotalCoinsEarned() : BigDecimal.ZERO;
            referrer.setGymCoinBalance(currentBalance.add(BigDecimal.valueOf(300)));
            referrer.setTotalCoinsEarned(earnedBalance.add(BigDecimal.valueOf(300)));
        }

        referral.setStatus(ReferralStatus.REWARDED);
        
        referralRepository.save(referral);
        referralRewardRepository.save(reward);
        organizationRepository.save(referrer);

        log.info("Referral reward unlocked. Referrer: {}, Referred: {}, RewardType: {}", referrer.getId(), referredOrg.getId(), reward.getRewardType());
    }

    @Transactional
    public ReferralDashboardResponse getDashboard(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (organization.getReferralCode() == null) {
            generateReferralCodeForOrganization(organization);
        }

        List<Referral> referrals = referralRepository.findByReferrer(organization);
        List<ReferralReward> rewards = referralRewardRepository.findByOrganization(organization);

        long totalReferrals = referrals.size();
        long successfulReferrals = referrals.stream().filter(r -> r.getStatus() == ReferralStatus.REWARDED).count();
        long pendingReferrals = referrals.stream().filter(r -> r.getStatus() == ReferralStatus.PENDING).count();

        long remainingForNextMilestone = 6 - (successfulReferrals % 6);
        if (remainingForNextMilestone == 6 && successfulReferrals > 0) {
            // Already hit the milestone or exactly on a multiple of 6? Wait, it's 1-5 coins, 6th is 6mo pro.
            // A simple logic: next milestone is always 6 - (successful % 6)
        }

        List<ReferralEntry> recentReferrals = referrals.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(5)
            .map(r -> new ReferralEntry(
                r.getReferred().getName(),
                r.getStatus(),
                r.getRewardType(),
                r.getCreatedAt().toInstant()
            ))
            .collect(Collectors.toList());

        List<RewardEntry> recentRewards = rewards.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(5)
            .map(r -> new RewardEntry(
                r.getRewardType(),
                r.getAmount(),
                r.getMonthsRewarded(),
                r.getCreatedAt().toInstant()
            ))
            .collect(Collectors.toList());

        return new ReferralDashboardResponse(
            organization.getReferralCode(),
            totalReferrals,
            successfulReferrals,
            pendingReferrals,
            organization.getGymCoinBalance(),
            organization.getTotalCoinsEarned(),
            organization.isSixMonthRewardClaimed(),
            remainingForNextMilestone,
            recentReferrals,
            recentRewards
        );
    }

    @Transactional(readOnly = true)
    public AdminReferralAnalyticsResponse getAnalytics() {
        long totalSignups = referralRepository.count();
        long totalSuccessful = referralRepository.findAll().stream().filter(r -> r.getStatus() == ReferralStatus.REWARDED).count();
        
        List<ReferralReward> allRewards = referralRewardRepository.findAll();
        BigDecimal totalCoins = allRewards.stream()
            .filter(r -> r.getRewardType() == RewardType.GYM_COIN)
            .map(ReferralReward::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        long totalSixMonths = allRewards.stream()
            .filter(r -> r.getRewardType() == RewardType.SIX_MONTH_PRO)
            .count();

        return new AdminReferralAnalyticsResponse(
            totalSignups,
            totalSuccessful,
            totalCoins,
            totalSixMonths
        );
    }
}
