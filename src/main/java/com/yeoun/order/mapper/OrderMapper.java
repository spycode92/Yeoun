package com.yeoun.order.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.yeoun.order.dto.WorkOrderSearchDTO;
import com.yeoun.order.dto.WorkOrderValidateRequest;
import com.yeoun.order.dto.WorkScheduleDTO;
import com.yeoun.order.dto.WorkerListDTO;
import com.yeoun.order.dto.WorkerProcessDTO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.equipment.dto.EquipDowntimeDTO;
import com.yeoun.order.dto.WorkOrderListDTO;
import com.yeoun.order.dto.MaterialAvailabilityDTO;

@Mapper
@Repository
public interface OrderMapper {
	
	// 작업지시 리스트 모두 불러오기
	List<WorkOrderListDTO> selectOrderList (WorkOrderSearchDTO dto);
	
	// 작업자 리스트
	List<WorkerListDTO> selectWorkers ();
	
	// 제품수량에 따른 필요한 자재량 체크
	List<MaterialAvailabilityDTO> selectMaterials (@Param("prdId")String prdId, @Param("planQty")Integer planQty);

	// 모든 스케줄 불러오기
	List<WorkScheduleDTO> selectAllSchedules ();
	
	// 스케줄별 작업자 불러오기
	List<WorkerProcessDTO> selectWorkersBySchedule (@Param("id")Long id);

	// 특정 시간대에 특정 작업자가 작업중인지 확인
	Integer existWorkingSchedule (@Param("empId")String empId, @Param("now") LocalDateTime now);
	
	// 겹치는 라인 작업 확인
	Integer countLineOverlap (WorkOrderValidateRequest req);
	
	// 겹치는 작업자 확인
	Integer countWorkerOverlap (WorkOrderValidateRequest req);
	
}
