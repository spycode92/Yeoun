package com.yeoun.order.mapper;

import java.util.List;

import com.yeoun.order.dto.WorkOrderSearchDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.order.dto.WorkOrderListDTO;
import com.yeoun.order.dto.MaterialAvailabilityDTO;

@Mapper
@Repository
public interface OrderMapper {
	
	// 작업지시 리스트 모두 불러오기
	List<WorkOrderListDTO> selectOrderList (WorkOrderSearchDTO dto);
	
	// 작업자 리스트
	List<EmpListDTO> selectWorkers ();
	
	// 제품수량에 따른 필요한 자재량 체크
	List<MaterialAvailabilityDTO> selectMaterials(@Param("prdId")String prdId, @Param("planQty")Integer planQty);

}
