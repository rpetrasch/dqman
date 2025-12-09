package org.dqman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Data
@NoArgsConstructor
public class DqProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String status; // e.g., CREATED, STARTED, SUCCESS, FAILED
    private ZonedDateTime createdDate;
    private ZonedDateTime startedDate;
    private ZonedDateTime finishedDate;
    @ManyToOne
    private DqFlow flow;

    public DqProject(String name, String description, String status, ZonedDateTime createdDate,
            ZonedDateTime startedDate, ZonedDateTime finishedDate, DqFlow flow) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdDate = createdDate;
        this.startedDate = startedDate;
        this.finishedDate = finishedDate;
        this.flow = flow;
    }

    public DqProject(String name, String description, String status, ZonedDateTime createdDate,
            ZonedDateTime startedDate, ZonedDateTime finishedDate) {
        this(name, description, status, createdDate, startedDate, finishedDate, null);
    }
}
