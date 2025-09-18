package com.armando0405.tuboletascraper.dao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consulta_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "total_shows", nullable = false)
    private Integer totalShows;

    @Column(name = "content_hash", length = 64, unique = true, nullable = false)
    private String contentHash;

    @Column(name = "query_url", length = 500, nullable = false)
    private String queryUrl;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShowSnapshot> shows = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}