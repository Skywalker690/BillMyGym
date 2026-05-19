package com.gymflow.subscription.event;

import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.UsageMetric;
import com.gymflow.shared.events.DomainEvent;
import java.time.LocalDate;

public record SubscriptionPaymentPaidEvent(
    Organization organization,
    OrganizationSubscriptionPayment payment,
    UsageMetric usage,
    LocalDate periodStart,
    LocalDate periodEnd,
    String billingSummary
) implements DomainEvent {
}
