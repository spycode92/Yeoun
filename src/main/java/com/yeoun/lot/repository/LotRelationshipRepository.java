package com.yeoun.lot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.lot.entity.LotRelationship;

@Repository
public interface LotRelationshipRepository extends JpaRepository<LotRelationship, Long> {


	Optional<LotRelationship> findByOutputLot_LotNoAndInputLot_LotNo(String outputLotNo, String inputLotNo);
	
	@Query("""
		    SELECT lr 
		    FROM LotRelationship lr
		    JOIN FETCH lr.outputLot ol
		    JOIN FETCH lr.inputLot il
		    LEFT JOIN FETCH il.material
		    WHERE il.lotNo = :inputLotNo
		    """)
	List<LotRelationship> findByInputLotNoWithFetch(@Param("inputLotNo") String inputLotNo);
	
	
	@Query("""
		    SELECT lr 
		    FROM LotRelationship lr
		    JOIN FETCH lr.inputLot il
		    LEFT JOIN FETCH il.material m
		    LEFT JOIN FETCH il.product p
		    WHERE lr.outputLot.lotNo = :outputLotNo
		    """)
	List<LotRelationship> findByOutputLotNoWithFullFetch(@Param("outputLotNo") String outputLotNo);

}
