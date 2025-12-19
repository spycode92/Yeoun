package com.yeoun.production.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.yeoun.process.mapper.ProductionTrendMapper;
import com.yeoun.production.dto.ProductionTrendResponseDTO;
import com.yeoun.production.dto.TrendRowDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductionDashboardService {
	
    private final ProductionTrendMapper productionTrendMapper;

    // =====================================================================================
    /**
     * 생산 현황 추적 차트 데이터 생성
     */
    public ProductionTrendResponseDTO getProductionTrend(String range) {

        // 1) range 기본값/정규화
        String r = (range == null || range.isBlank()) ? "day" : range.toLowerCase();

        LocalDate today = LocalDate.now();

        // [fromDt, toDt) 범위로 조회 (toDt는 내일 0시)
        LocalDateTime toDt = today.plusDays(1).atStartOfDay();
        LocalDateTime fromDt;

        // 2) 기간 윈도우 결정
        if ("week".equals(r)) {
            // 최근 6주: (오늘 기준) 5주 전 월요일 0시부터
            fromDt = today.minusWeeks(5).with(DayOfWeek.MONDAY).atStartOfDay();
        } else if ("month".equals(r)) {
            // 최근 6개월: 5개월 전 1일 0시부터
            fromDt = today.minusMonths(5).withDayOfMonth(1).atStartOfDay();
        } else {
            // 기본 day: 최근 7일
            r = "day";
            fromDt = today.minusDays(6).atStartOfDay();
        }

        // 3) Mapper에서 집계 조회 
        List<TrendRowDTO> plannedRows = selectPlanned(r, fromDt, toDt);
        List<TrendRowDTO> completedRows = selectCompleted(r, fromDt, toDt);

        // 4) Map<label, cnt> 변환 (라벨 기준으로 빠르게 찾기 위해)
        Map<String, Long> plannedMap = toMap(plannedRows);
        Map<String, Long> completedMap = toMap(completedRows);

        // 5) 라벨 축 생성 (데이터가 없는 구간도 축에 포함 -> 차트 안 깨짐)
        List<String> labels = buildLabels(r, fromDt.toLocalDate(), today);
        
        labels = trimLabels(r, labels);

        // 6) 라벨 순서대로 값 매핑 (없으면 0으로 채움)
        List<Long> planned = labels.stream()
                .map(l -> plannedMap.getOrDefault(l, 0L))
                .toList();

        List<Long> completed = labels.stream()
                .map(l -> completedMap.getOrDefault(l, 0L))
                .toList();

        // 7) 응답 DTO 구성
        return ProductionTrendResponseDTO.builder()
                .range(r)
                .labels(labels)
                .planned(planned)
                .completed(completed)
                .build();
    }
    
    /**
     * 주/월 차트는 최근 구간만 보여주기
     * - week: 최근 4구간
     * - month: 최근 6구간
     */
    private List<String> trimLabels(String r, List<String> labels) {
        if (labels == null) return List.of();

        int keep;
        if ("week".equals(r)) keep = 4;
        else if ("month".equals(r)) keep = 6;
        else return labels; // day는 그대로

        if (labels.size() <= keep) return labels;
        return labels.subList(labels.size() - keep, labels.size());
    }

    // Mapper 호출 분기
    private List<TrendRowDTO> selectPlanned(String r, LocalDateTime fromDt, LocalDateTime toDt) {
        return switch (r) {
            case "week" -> productionTrendMapper.plannedByWeek(fromDt, toDt);
            case "month" -> productionTrendMapper.plannedByMonth(fromDt, toDt);
            default -> productionTrendMapper.plannedByDay(fromDt, toDt);
        };
    }

    private List<TrendRowDTO> selectCompleted(String r, LocalDateTime fromDt, LocalDateTime toDt) {
        return switch (r) {
            case "week" -> productionTrendMapper.completedByWeek(fromDt, toDt);
            case "month" -> productionTrendMapper.completedByMonth(fromDt, toDt);
            default -> productionTrendMapper.completedByDay(fromDt, toDt);
        };
    }

    // =========================
    // Map 변환
    // =========================
    private Map<String, Long> toMap(List<TrendRowDTO> rows) {
        if (rows == null) return Map.of();
        return rows.stream().collect(Collectors.toMap(
                TrendRowDTO::getLabel,
                r -> Optional.ofNullable(r.getCnt()).orElse(0L),
                (a, b) -> a
        ));
    }

    // =========================
    // 라벨 축 생성 (0 채우기용)
    // =========================
    private List<String> buildLabels(String r, LocalDate from, LocalDate toInclusive) {
        List<String> labels = new ArrayList<>();

        if ("month".equals(r)) {
            LocalDate start = from.withDayOfMonth(1);
            LocalDate end = toInclusive.withDayOfMonth(1);

            for (LocalDate d = start; !d.isAfter(end); d = d.plusMonths(1)) {
                labels.add(String.format("%04d-%02d", d.getYear(), d.getMonthValue()));
            }
            return labels;
        }

        if ("week".equals(r)) {

            // fromDt 기준으로 주 단위(월요일)로 돌되,
            // 라벨은 "12월 1주차" 처럼 월 기준 주차로 생성
            LocalDate start = from.with(DayOfWeek.MONDAY);
            LocalDate end = toInclusive.with(DayOfWeek.MONDAY);

            for (LocalDate d = start; !d.isAfter(end); d = d.plusWeeks(1)) {
                int month = d.getMonthValue();

                // 월 기준 주차: (해당 월 1일이 속한 주의 월요일) 기준으로 몇 주차인지 계산
                LocalDate firstDay = d.withDayOfMonth(1);
                LocalDate firstMon = firstDay.with(DayOfWeek.MONDAY);
                if (firstMon.isAfter(firstDay)) firstMon = firstMon.minusWeeks(1);

                long weekNo = java.time.temporal.ChronoUnit.WEEKS.between(firstMon, d) + 1;

                labels.add(month + "월 " + weekNo + "주차");
            }
            return labels;
        }


        // day
        for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
            labels.add(d.toString()); // YYYY-MM-DD
        }
        return labels;
    }


}
