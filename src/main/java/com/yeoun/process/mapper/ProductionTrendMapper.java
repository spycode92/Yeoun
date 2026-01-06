package com.yeoun.process.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yeoun.process.dto.ImmediateActionRowDTO;
import com.yeoun.production.dto.TrendRowDTO;

// 생산 현황 추적 차트 전용 Mapper
@Mapper
public interface ProductionTrendMapper {
	
    // DAY (일)
    List<TrendRowDTO> plannedByDay(@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt);

    List<TrendRowDTO> completedByDay(@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt);

    // WEEK (주) - 월 기준 주차: "MM월 W주차"
    List<TrendRowDTO> plannedByWeek(@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt);

    List<TrendRowDTO> completedByWeek(@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt);

    // MONTH (월)
    List<TrendRowDTO> plannedByMonth(@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt);

    List<TrendRowDTO> completedByMonth(@Param("fromDt") LocalDateTime fromDt, @Param("toDt") LocalDateTime toDt);

    // ===========================
    // 즉시 조치 리스트 함께 사용
    List<ImmediateActionRowDTO> selectImmediateActions(@Param("limit") int limit);
    
}
