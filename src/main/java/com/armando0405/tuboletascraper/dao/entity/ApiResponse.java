package com.armando0405.tuboletascraper.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private boolean hasChanges;
    private String message;
    private int totalShows;
    private Integer totalChanges;
    private List<String> changes;
    private LocalDateTime lastChecked;
    private List<Show> shows;
    private Long executionTimeMs;
}