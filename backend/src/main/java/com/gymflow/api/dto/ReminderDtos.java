package com.gymflow.api.dto;

public class ReminderDtos {

    public record ReminderRunResponse(
        int processedMembers,
        int organizationsGrouped,
        int digestsSent
    ) {
    }
}
