package com.gymflow.api;

import com.gymflow.api.dto.MembershipDtos.CreateMembershipRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.MemberMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MembershipController {

    private final MemberMembershipService memberMembershipService;

    @PostMapping("/api/members/{memberId}/memberships")
    public ResponseEntity<?> createMembership(@PathVariable Long memberId, @Valid @RequestBody CreateMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                "Membership created successfully",
                memberMembershipService.createMembership(memberId, request)
            ));
    }

    @GetMapping("/api/members/{memberId}/memberships")
    public ResponseEntity<?> listMemberships(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Membership history fetched successfully",
            memberMembershipService.listMemberships(memberId)
        ));
    }
}
