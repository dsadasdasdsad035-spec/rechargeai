package com.wildai.fulfillment.dto;

import java.time.Instant;

public final class AdminFulfillmentRequests {

    private AdminFulfillmentRequests() {}

    public record AssignRequest(Long assigneeAdminId) {}

    public record AddNoteRequest(String content, Boolean userVisible) {}

    public record MarkSuccessRequest(Instant subscriptionStart, Instant subscriptionEnd, String remark) {}

    public record MarkFailedRequest(String failureReason, String remark) {}

    public record WaitUserRequest(String instruction) {}
}
