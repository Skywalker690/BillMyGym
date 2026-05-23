package com.gymflow.api;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.service.IndividualWhatsappReminderService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final IndividualWhatsappReminderService individualWhatsappReminderService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Member created successfully", memberService.createMember(request)));
    }

    @GetMapping
    public ResponseEntity<?> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "40") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String membershipStatus,
        @RequestParam(required = false) MemberStatus memberStatus
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Members fetched successfully",
            memberService.listMembers(page, size, search, membershipStatus, memberStatus)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Member fetched successfully", memberService.getMember(id)));
    }

    @GetMapping("/{id}/whatsapp-reminder/preview")
    public ResponseEntity<?> whatsappReminderPreview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "WhatsApp reminder preview generated",
            individualWhatsappReminderService.preview(id)
        ));
    }

    @PostMapping("/{id}/whatsapp-reminder/send")
    public ResponseEntity<?> sendWhatsappReminder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "WhatsApp reminder processed",
            individualWhatsappReminderService.send(id)
        ));
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
