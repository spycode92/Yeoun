package com.yeoun.order.entity;

import com.yeoun.emp.entity.Emp;
import com.yeoun.masterData.entity.ProcessMst;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "WORKER_PROCESS")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProcess {

    // 작업자 담당 공정 번호
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "WORKER_NEW_SEQ")
    @SequenceGenerator(
            name = "WORKER_NEW_SEQ",
            sequenceName = "WORKER_NEW_SEQ",
            allocationSize = 1
    )
    private Long wpId;

    // 스케줄 번호
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCHEDULE_ID", nullable = false)
    private WorkSchedule schedule;

    // 작업자 번호
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "WORKER_ID", nullable = false)
    private Emp worker;

    // 담당 공정
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROCESS_ID", nullable = false)
    private ProcessMst process;


}
