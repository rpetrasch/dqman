package org.dqman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class DqProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String status; // e.g., CREATED, STARTED, SUCCESS, FAILED
    private LocalDateTime createdDate;
    private LocalDateTime startedDate;
    private LocalDateTime finishedDate;
}
