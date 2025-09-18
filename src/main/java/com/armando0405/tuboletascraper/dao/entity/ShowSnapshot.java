package com.armando0405.tuboletascraper.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "show_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ConsultaSnapshot snapshot;

    @Column(name = "show_unique_id", nullable = false, length = 200)
    private String showUniqueId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String venue;

    @Column(length = 100)
    private String city;

    @Column(name = "show_date")
    private LocalDate showDate;

    @Column(name = "show_time")
    private LocalTime showTime;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Lob
    @Column(name = "raw_html")
    private String rawHtml;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}