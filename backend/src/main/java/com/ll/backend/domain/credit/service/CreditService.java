package com.ll.backend.domain.credit.service;

import com.ll.backend.domain.credit.dto.CreditChargeRequest;
import com.ll.backend.domain.credit.dto.CreditChargeApiResponse;
import com.ll.backend.domain.credit.dto.CreditsApiResponse;
import com.ll.backend.domain.credit.dto.CreditTransactionsApiResponse;

public interface CreditService {

    CreditsApiResponse getCredits();

    CreditChargeApiResponse chargeSandbox(CreditChargeRequest request);

    CreditTransactionsApiResponse listSandboxTransactions();
}
