package com.yeoun.pay.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yeoun.pay.dto.PayCalcStatusDTO;
import com.yeoun.pay.enums.CalcStatus;
import com.yeoun.pay.repository.PayrollPayslipRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayCalcStatusService {

    private final PayrollPayslipRepository repo;

    public PayCalcStatusDTO getStatus(String yyyymm) {

        long totalCount     = repo.countByPayYymm(yyyymm);
        long confirmedCount  = repo.countByPayYymmAndCalcStatus(yyyymm, CalcStatus.CONFIRMED);
        long simulatedCount  = repo.countByPayYymmAndCalcStatus(yyyymm, CalcStatus.SIMULATED);
        long calculatedCount = repo.countByPayYymmAndCalcStatus(yyyymm, CalcStatus.CALCULATED);


        BigDecimal totAmt = repo.sumTotalByYymm(yyyymm);
        BigDecimal dedAmt   = repo.sumDeductByYymm(yyyymm);
        BigDecimal netAmt   = repo.sumNetByYymm(yyyymm);

        // 첫 번째 상태값(READY / SIMULATED / CALCULATED / CONFIRMED)
        String calcStatus = repo.findFirstStatusByYyyymm(yyyymm)
                                .orElse("READY");

        // 🔥 확정 정보 조회
        List<Object[]> rows = repo.findLastConfirmInfo(yyyymm);

        String confirmUser = null;
        LocalDateTime confirmDate = null;

        if (rows != null && !rows.isEmpty()) {
            Object[] info = rows.get(0);

            if (info.length == 2) {
                confirmUser = info[0] != null ? info[0].toString() : null;
                confirmDate = (LocalDateTime) info[1];
            }
        }



        return PayCalcStatusDTO.builder()
                .payYymm(yyyymm)
                .totalCount(totalCount)
                .simulatedCount(simulatedCount)
                .calculatedCount(calculatedCount)
                .confirmedCount(confirmedCount)
                .totAmt(totAmt)
                .dedAmt(dedAmt)
                .netAmt(netAmt)
                .calcStatus(calcStatus)
                .confirmUser(confirmUser)    
                .confirmDate(confirmDate)    
                .build();
    }
}