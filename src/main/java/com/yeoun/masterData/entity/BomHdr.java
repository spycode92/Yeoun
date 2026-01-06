package com.yeoun.masterData.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NotFound;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BOM_HDR")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BomHdr implements Serializable {

    @Id
    @Column(name="BOM_HDR_ID", length = 50, nullable = false)
	private String bomHdrId; //BOM Header ID
    
    @Column(name = "BOM_ID") 
    private String bomId;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOM_ID", referencedColumnName = "BOM_ID", insertable = false, updatable = false)
    private List<BomMst> bomMstList = new ArrayList<>();

    @Column(name="BOM_HDR_NAME", length = 100, nullable = false)
	private String bomHdrName; //BOM Header Name
	
    @Column(name="BOM_HDR_TYPE", length = 50, nullable = false)
	private String bomHdrType; //BOM Header Type

    @Column(name="USE_YN", length = 1)
    private String useYn; //사용여부
    
    @Column(name="CREATED_ID", length = 7)
    private String createdId; //사용여부

    @Column(name="CREATED_DATE")
    private LocalDateTime createdDate;
    
    @Column(name="UPDATED_ID", length = 7)
    private String updatedId; //사용여부
    
    @Column(name="UPDATED_DATE")
    private LocalDateTime updatedDate;

    
}
