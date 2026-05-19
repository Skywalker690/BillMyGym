package com.gymflow.api;

import com.gymflow.api.dto.PaymentDtos.RecordPaymentRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.PaymentLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentLedgerService paymentLedgerService;

    @PostMapping
    public ResponseEntity<?> recordPayment(
        @RequestHeader("X-Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody RecordPaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Payment recorded successfully", paymentLedgerService.recordPayment(request, idempotencyKey)));
    }

    @GetMapping
    public ResponseEntity<?> listPayments() {
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", paymentLedgerService.listPayments()));
    }
}
