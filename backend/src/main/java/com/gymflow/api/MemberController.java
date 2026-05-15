package com.gymflow.api;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Member created successfully", memberService.createMember(request)));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(ApiResponse.success("Members fetched successfully", memberService.listMembers()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member updated successfully", memberService.updateMember(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        memberService.softDeleteMember(id);
        return ResponseEntity.ok(ApiResponse.success("Member deleted successfully"));
    }
}
