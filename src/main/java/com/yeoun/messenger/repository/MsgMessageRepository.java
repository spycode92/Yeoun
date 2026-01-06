package com.yeoun.messenger.repository;

import com.yeoun.messenger.entity.MsgMessage;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MsgMessageRepository extends JpaRepository<MsgMessage, Long> {

    List<MsgMessage> findByRoomId_RoomIdOrderBySentDate(Long roomId);
	
	// 메시지 내용 검색
    @Query("""
       SELECT DISTINCT m.roomId.roomId
       FROM MsgMessage m
       WHERE m.msgContent LIKE %:keyword%
       """)
	List<Long> findRoomIdByMessageContent(@Param("keyword") String keyword);

	// 검색어를 포함한 메시지 1개 찾기
	@Query("""
		SELECT m.msgContent
		FROM MsgMessage m
		WHERE m.roomId.roomId = :roomId
		AND m.msgContent LIKE %:keyword%
		ORDER BY m.sentDate desc
		""")
	List<String> findMatchedMessages(@Param("roomId") Long roomId,
									 @Param("keyword") String keyword);

	// 가장 최근 메시지 1개 찾기
	MsgMessage findTop1ByRoomId_RoomIdOrderByMsgIdDesc(Long roomId);

	// 특정 메시지 시간
	@Query("""
		SELECT m.sentDate 
		FROM MsgMessage m
		WHERE m.roomId.roomId = :roomId
		ORDER BY m.sentDate desc
		""")
	List<LocalDateTime> findSentDate(@Param("roomId") Long roomId);

    
    // 안 읽은 메시지 계산하는 쿼리
    @Query("""
        SELECT COUNT(*)
        FROM MsgMessage m
        WHERE m.roomId.roomId = :roomId
    	AND m.msgId > (
    		SELECT COALESCE(r.lastReadId, 0)
    		FROM MsgRelation r
    		WHERE r.roomId.roomId = :roomId
    		AND r.empId.empId = :empId
    	)
    	AND m.msgId <= :msgId
    	""")
    Integer countUnreadMessage(@Param("roomId") Long roomId,
    						   @Param("empId") String empId,
    						   @Param("msgId") Long msgId);


}
