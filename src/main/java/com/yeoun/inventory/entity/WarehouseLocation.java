package com.yeoun.inventory.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "WAREHOUSE_LOCATION")
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class WarehouseLocation {
	@Id 
	@Column(name = "LOCATION_ID", updatable = false)
	private String locationId; // 로케이션 고유ID
	
	@Column(nullable = false)
	private String zone; // 존
	
	@Column(nullable = false)
	private String rack; // 랙
	
	@Column(nullable = false)
	private String rackRow; // 로우
	
	@Column(nullable = false)
	private String rackCol; // 컬럼

    public String getLocationName() {
        return String.format("%s-%s-%s-%s", 
            zone != null ? zone : "", 
            rack != null ? rack : "", 
            rackRow != null ? rackRow : "", 
            rackCol != null ? rackCol : ""
        ).replaceAll("-+", "-").replaceAll("-$", ""); // 연속된 하이픈 제거 및 끝 하이픈 제거
    }
}
