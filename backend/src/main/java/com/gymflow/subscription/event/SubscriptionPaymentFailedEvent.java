package com.gymflow.subscription.event;

import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.UsageMetric;
import com.gymflow.shared.events.DomainEvent;

public record SubscriptionPaymentFailedEvent(
    OrganizationSubscriptionPayment payment,
    UsageMetric usage,
    String billingSummary
) implements DomainEvent {
}
