package com.yeoun.messenger.repository;

import com.yeoun.messenger.entity.MsgRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MsgRelationRepository extends JpaRepository<MsgRelation, Long> {

	// 두 사람이 속한 개인 채팅방이 있는지 조회
    @Query("""
       SELECT r.roomId.roomId
       FROM MsgRelation r
       JOIN MsgRelation r2 
         ON r.roomId.roomId = r2.roomId.roomId
       WHERE r.empId.empId = :targetUser
         AND r2.empId.empId = :authUser
         AND r.roomId.groupYn = 'N'
       """)
    Long findOneToOneRoom(@Param("authUser")String authUser, @Param("targetUser")String targetUser);
    
    
    // 마지막 조회 메시지 업데이트
    @Modifying
    @Query("""
        UPDATE MsgRelation r 
        SET r.lastReadId = :lastReadId 
        WHERE r.empId.empId = :empId 
        AND r.roomId.roomId = :roomId
    """)
    void updateLastRead(@Param("empId")String empId, @Param("roomId")Long roomId, @Param("lastReadId")Long lastReadId);

    // 방에 속한 모든 멤버를 조회
    List<MsgRelation> findByRoomId_RoomId(Long roomId);
    
    // 방에서 특정 사용자 한 명을 찾기
    Optional<MsgRelation> findByRoomId_RoomIdAndEmpId_EmpId(@Param("roomId")Long roomId, @Param("empId")String empId);
    
	// 참여자 이름 검색
	@Query("""
		SELECT DISTINCT r.roomId.roomId
		FROM MsgRelation r 
		JOIN r.empId e 
		WHERE e.empName LIKE %:keyword%
	   """)
	List<Long> findRoomIdByMemberName(@Param("keyword") String keyword);

    // 내가 속한 방 목록
    @Query("""
    	SELECT r.roomId.roomId
    	FROM MsgRelation r
        WHERE TRIM(r.participantYn) = 'Y'
        AND r.empId.empId = :empId
       """)
    List<Long> findRoomIdsByEmpId(@Param("empId") String empId);

	

}
