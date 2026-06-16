package com.gymflow.api;

import com.gymflow.api.dto.MemberDtos.BulkMemberImportIssue;
import com.gymflow.api.response.ApiErrorResponse;
import com.gymflow.api.response.ApiFieldError;
import com.gymflow.api.dto.OrganizationDtos.ReplaceOrganizationAdminRequest;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.service.MemberService;
import com.gymflow.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<?> listOrganizations() {
        return ResponseEntity.ok(ApiResponse.success("Organizations fetched successfully", organizationService.listOrganizations()));
    }

    @GetMapping("/directory")
    public ResponseEntity<?> organizationDirectory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean includeAllTypes,
        @RequestParam(defaultValue = "false") boolean pilotOnly,
        @RequestParam(required = false) OrganizationType organizationType,
        @RequestParam(required = false) OrganizationStatus status,
        @RequestParam(required = false) SubscriptionPlan plan
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization directory fetched successfully",
            organizationService.directory(page, size, search, includeAllTypes, pilotOnly, organizationType, status, plan)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Organization fetched successfully", organizationService.getOrganization(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", organizationService.updateOrganization(id, request)));
    }

    @PutMapping("/{id}/admin")
    public ResponseEntity<?> replaceOrganizationAdmin(@PathVariable Long id, @Valid @RequestBody ReplaceOrganizationAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization admin replaced successfully",
            organizationService.replaceOrganizationAdmin(id, request)
        ));
    }

    @PostMapping(path = "/{id}/members/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUploadMembers(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        var response = memberService.bulkImportMembers(id, file);
        if (!response.errors().isEmpty()) {
            List<ApiFieldError> errors = response.errors().stream()
                .map(this::toFieldError)
                .toList();
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                    "Bulk member upload failed. Fix the highlighted rows and try again.",
                    "BULK_MEMBER_IMPORT_VALIDATION",
                    errors
                ));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Members imported successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> hardDeleteOrganization(@PathVariable Long id) {
        organizationService.hardDeleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("Organization permanently deleted successfully"));
    }

    private ApiFieldError toFieldError(BulkMemberImportIssue issue) {
        String field = issue.rowNumber() == null
            ? issue.field()
            : "rows[" + issue.rowNumber() + "]." + issue.field();
        return new ApiFieldError(field, issue.message());
    }
}
