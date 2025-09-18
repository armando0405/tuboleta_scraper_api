package com.armando0405.tuboletascraper.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "previous_snapshot_id")
    private ConsultaSnapshot previousSnapshot;

    @ManyToOne
    @JoinColumn(name = "new_snapshot_id", nullable = false)
    private ConsultaSnapshot newSnapshot;

    @Lob
    @Column(name = "change_summary")
    private String changeSummary;

    @Column(name = "total_changes")
    private Integer totalChanges;

    @CreationTimestamp
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;
}