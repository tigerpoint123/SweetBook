package com.ll.backend.domain.credit.controller;

import com.ll.backend.domain.credit.dto.CreditChargeRequest;
import com.ll.backend.domain.credit.dto.CreditChargeApiResponse;
import com.ll.backend.domain.credit.dto.CreditsApiResponse;
import com.ll.backend.domain.credit.dto.CreditTransactionsApiResponse;
import com.ll.backend.domain.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class CreditV1Controller {

    private final CreditService creditService;

    @GetMapping(value = "/credits", produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditsApiResponse getCredits() {
        return creditService.getCredits();
    }

    @PostMapping(
            value = "/credits/sandbox/charge",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditChargeApiResponse chargeCredit(
            @RequestBody CreditChargeRequest body
    ) {
        return creditService.chargeSandbox(body);
    }

    @GetMapping(value = "/credits/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public CreditTransactionsApiResponse listSandboxTransactions() {
        return creditService.listSandboxTransactions();
    }
}
