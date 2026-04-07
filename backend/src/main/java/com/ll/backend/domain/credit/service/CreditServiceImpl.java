package com.ll.backend.domain.credit.service;

import com.ll.backend.domain.credit.dto.CreditChargeApiResponse;
import com.ll.backend.domain.credit.dto.CreditChargeDataDto;
import com.ll.backend.domain.credit.dto.CreditChargeRequest;
import com.ll.backend.domain.credit.dto.CreditTransactionDto;
import com.ll.backend.domain.credit.dto.CreditsApiResponse;
import com.ll.backend.domain.credit.dto.CreditTransactionsApiResponse;
import com.ll.backend.domain.credit.dto.CreditTransactionsDataDto;
import com.ll.backend.global.client.dto.CreditBalanceData;
import com.ll.backend.global.client.dto.CreditChargeData;
import com.ll.backend.global.client.dto.CreditTransactionsData;
import com.ll.backend.global.client.dto.SweetbookApiResponse;
import com.ll.backend.global.client.SweetbookApiClient;

import java.util.List;
import java.util.Map;

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
        boolean success = response.success();
        String message = response.message();
        CreditBalanceData data = CreditBalanceData.fromNullable(response.data());

        return new CreditsApiResponse(
                success,
                message,
                data.toDto());
    }

    @Override
    public synchronized CreditChargeApiResponse chargeSandbox(CreditChargeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount은 1 이상이어야 합니다.");
        }
        String memo = request.memo() != null ? request.memo().trim() : "";

        SweetbookApiResponse<CreditChargeData> response =
                sweetbookApiClient.chargeSandboxCredit(Map.of("amount", request.amount(), "memo", memo));
        validateChargeSandboxResponse(response);

        CreditChargeData data = response.data();

        return new CreditChargeApiResponse(
                response.success(),
                response.message(),
                new CreditChargeDataDto(
                        data.transactionUid(),
                        data.amount(),
                        data.balanceAfter(),
                        data.currency()));
    }

    @Override
    public synchronized CreditTransactionsApiResponse listSandboxTransactions() {
        SweetbookApiResponse<CreditTransactionsData> response =
                sweetbookApiClient.getCreditTransactions(10, 0);
        validateCreditTransactionsResponse(response);

        CreditTransactionsData data = response.data();
        List<CreditTransactionDto> transactions = data.transactions()
                .stream()
                .map(CreditTransactionDto::from)
                .toList();

        return new CreditTransactionsApiResponse(
                response.success(),
                response.message(),
                new CreditTransactionsDataDto(
                        transactions,
                        data.total(),
                        data.limit(),
                        data.offset()));
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
        if (data.amount() == null || data.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "충전 응답 amount가 올바르지 않습니다.");
        }
        if (data.balanceAfter() == null || data.balanceAfter() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "충전 응답 balanceAfter가 올바르지 않습니다.");
        }
    }

    private static void validateCreditTransactionsResponse(
            SweetbookApiResponse<CreditTransactionsData> response) {
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
