package com.yeoun.sales.repository;

import com.yeoun.sales.entity.Shipment;
import com.yeoun.sales.entity.ShipmentItem;
import com.yeoun.sales.enums.ShipmentStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    /** 최근 SHIPMENT_ID 조회 **/
    @Query(value = """
        SELECT SHIPMENT_ID
        FROM SHIPMENT
        WHERE SHIPMENT_ID LIKE :prefix || '%'
        ORDER BY SHIPMENT_ID DESC
        FETCH FIRST 1 ROWS ONLY
        """, nativeQuery = true)
    String findLastId(@Param("prefix") String prefix);

    /** 이미 예약된 수주인지 확인 */
    boolean existsByOrderId(String orderId);

    /** shipmentId로 단건 조회 */
    Optional<Shipment> findByShipmentId(String shipmentId);

    /** 출하 예약 상태 업데이트 */
    @Modifying
    @Query("""
        UPDATE Shipment s
        SET s.shipmentStatus = 'RESERVED'
        WHERE s.shipmentId = :shipmentId
    """)
    void updateStatusToReserved(@Param("shipmentId") String shipmentId);

   // 출하지시 취소
    Optional<Shipment> findByOrderIdAndShipmentStatusIn(
            String orderId,
            List<ShipmentStatus> statusList
    );

    
    //취소시 상태 여부
    boolean existsByOrderIdAndShipmentStatusIn(
            String orderId,
            List<ShipmentStatus> statusList
    );

    // 운송번호 자동생성
    @Query("""
    	    SELECT MAX(s.trackingNumber)
    	    FROM Shipment s
    	    WHERE s.trackingNumber LIKE :prefix%
    	""")
    	String findLastTrackingNumber(@Param("prefix") String prefix);




}
