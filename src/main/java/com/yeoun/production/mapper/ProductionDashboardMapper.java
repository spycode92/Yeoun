package com.yeoun.production.mapper;

import com.yeoun.order.dto.ItemPlanAndOrderDTO;
import com.yeoun.order.dto.PlanAndOrderDashDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ProductionDashboardMapper {

    // 오늘 생산계획 카운트
    Integer countTodayProductionPlan ();

    // 오늘 생산계획 중 작업지시로 넘어간 갯수 카운트
    Integer countTodayProductionOrder ();

    // 오늘 생성된 작업지시 카운트
    Integer countTodayWorkOrder ();

    // 일정이 지연된 작업지시 카운트
    Integer countDelayedWorkOrder ();

    // 일별 생산계획 및 작업지시
    List<PlanAndOrderDashDTO> selectDailyPlanVsOrder (@Param("id") String id);

    // 주별 생산계획 및 작업지시
    List<PlanAndOrderDashDTO> selectWeeklyPlanVsOrder (@Param("id") String id);

    // 월별 생산계획 및 작업지시
    List<PlanAndOrderDashDTO> selectMonthlyPlanVsOrder (@Param("id") String id);

    // 품목별 생산계획 및 작업지시
    List<ItemPlanAndOrderDTO> selectItemPlanVsOrder ();
}
