package com.yeoun.main.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.main.entity.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
	
	@Query("""
			select s
			from Schedule s
			left join s.scheduleSharers ss
			where (s.scheduleStart <= :endDate and s.scheduleFinish >= :startDate)
			and (
			      s.scheduleType = 'company'
			      or (s.scheduleType = 'share' and ss.sharedEmp.empId = :empId)
			      or s.emp.empId = :empId
			    )
			""")
	List<Schedule> getIndividualSchedule(@Param("empId")String empId 
			, @Param("startDate")LocalDateTime startDate, @Param("endDate")LocalDateTime endDate);
	
}
