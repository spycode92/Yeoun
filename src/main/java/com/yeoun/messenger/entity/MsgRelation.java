package com.yeoun.messenger.entity;

import com.yeoun.emp.entity.Emp;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "MSG_RELATION")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
public class MsgRelation {

    // 관계 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RELATION_SEQ")
    @SequenceGenerator(
            name = "RELATION_SEQ",
            sequenceName = "RELATION_SEQ",
            allocationSize = 1
    )
    private long relationId;

    // 채팅방 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROOM_ID", nullable = false)
    private MsgRoom roomId;

    // 참여자 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMP_ID", nullable = false)
    private Emp empId;

    // 마지막 조회 메시지 ID
    @Column(name = "LAST_READ_ID")
    private Long lastReadId;

    // 채팅방 고정 여부
    @Column(nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String pinnedYn;

    // 채팅방 참여 상태
    @Column(nullable = false,  columnDefinition = "CHAR(1) DEFAULT 'Y'")
    private String participantYn;

    // 마지막 입장 일시
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime joinDate;

    // 마지막 확인 시간
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime activeDate;

    // 채팅방 고정 시간
    @Column
    private LocalDateTime pinnedDate;

    // 비고
    @Column
    private String remark;



}
