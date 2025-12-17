package com.yeoun.sales.repository;

import com.yeoun.sales.entity.Client;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, String> {

    /** 거래처명 검색 */
    List<Client> findByClientNameContaining(String keyword);

    /** 거래처구분(CUSTOMER / SUPPLIER) 검색 */
    List<Client> findByClientType(String clientType);

    /** 검색박스: 이름 + 유형 동시필터 */
    @Query("""
    	    SELECT c 
    	    FROM Client c 
    	    WHERE (:type IS NULL OR c.clientType = :type)
    	      AND (
    	            :keyword IS NULL OR 
    	            c.clientName LIKE %:keyword% OR
    	            c.businessNo LIKE %:keyword% OR
    	            c.managerName LIKE %:keyword%
    	      )
    	""")
    	List<Client> search(
    	        @Param("keyword") String keyword,
    	        @Param("type") String type
    	);    
    
    
    /**협력사 기준 품목 또는 사업자명 검색  */  
    @Query("""
    		SELECT DISTINCT c
    		FROM Client c
    		JOIN ClientItem ci ON c.clientId = ci.clientId
    		JOIN MaterialMst m ON ci.materialId = m.matId
    		WHERE c.clientType = 'SUPPLIER'
    		  AND m.useYn = 'Y'
    		  AND (
    		        :keyword IS NULL OR
    		        c.clientName LIKE CONCAT('%', :keyword, '%') OR
    		        c.businessNo LIKE CONCAT('%', :keyword, '%') OR
    		        c.managerName LIKE CONCAT('%', :keyword, '%')
    		  )
    		  AND (
    		        :itemKeyword IS NULL OR
    		        ci.materialId LIKE CONCAT('%', :itemKeyword, '%') OR
    		        m.matName LIKE CONCAT('%', :itemKeyword, '%')
    		  )
    		""")
    		List<Client> searchSupplierByItem(
    		    @Param("keyword") String keyword,
    		    @Param("itemKeyword") String itemKeyword
    		);



    /*코드 생성*/
    @Query(value = """
    	    SELECT MAX(c.clientId)
    	    FROM Client c
    	    WHERE c.clientId LIKE :pattern
    	""")
    	String findMaxClientId(@Param("pattern") String pattern);
    
    /*사업자번호 중복체크*/
    boolean existsByBusinessNo(String businessNo);
    
    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE REPLACE(c.businessNo, '-', '') = :bizNo")
    boolean existsBizNoClean(@Param("bizNo") String bizNo);


}
