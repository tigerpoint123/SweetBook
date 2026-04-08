package com.ll.backend.domain.credit.service;

import com.ll.backend.domain.credit.dto.CreditChargeApiResponse;
import com.ll.backend.domain.credit.dto.CreditChargeRequest;
import com.ll.backend.domain.credit.dto.CreditsApiResponse;
import com.ll.backend.domain.credit.dto.CreditTransactionsApiResponse;
import com.ll.backend.global.client.dto.credit.CreditBalanceData;
import com.ll.backend.global.client.dto.credit.CreditChargeData;
import com.ll.backend.global.client.dto.credit.CreditChargeRequestPayload;
import com.ll.backend.global.client.dto.credit.CreditTransactionsData;
import com.ll.backend.global.client.dto.book.SweetbookApiResponse;
import com.ll.backend.global.client.SweetbookApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditServiceImpl implements CreditService {

    private final SweetbookApiClient sweetbookApiClient;

    @Override
    public synchronized CreditsApiResponse getCredits() {
        SweetbookApiResponse<CreditBalanceData> response = sweetbookApiClient.getCredits();
        return CreditsApiResponse.from(response);
    }

    @Override
    public synchronized CreditChargeApiResponse chargeSandbox(CreditChargeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }

        SweetbookApiResponse<CreditChargeData> response =
                sweetbookApiClient.chargeSandboxCredit(new CreditChargeRequestPayload(request.amount(), request.memo()));
        validateChargeSandboxResponse(response);
        return CreditChargeApiResponse.from(response);
    }

    @Override
    public synchronized CreditTransactionsApiResponse listSandboxTransactions() {
        SweetbookApiResponse<CreditTransactionsData> response =
                sweetbookApiClient.getCreditTransactions(10, 0);
        validateCreditTransactionsResponse(response);
        return CreditTransactionsApiResponse.from(response);
    }

    private static void validateChargeSandboxResponse(SweetbookApiResponse<CreditChargeData> response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "충전 응답이 비어 있습니다.");
        }

        if (!response.success()) {
            String msg = response.message() != null ? response.message() : "충전에 실패했습니다.";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
        }

        CreditChargeData data = response.data();
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "충전 응답 data가 비어 있습니다.");
        }
    }

    private static void validateCreditTransactionsResponse(SweetbookApiResponse<CreditTransactionsData> response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "거래내역 응답이 비어 있습니다.");
        }
        if (!response.success()) {
            String msg = response.message() != null ? response.message() : "거래내역 조회에 실패했습니다.";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
        }
        if (response.data() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "거래내역 응답 data가 비어 있습니다.");
        }
    }

}
