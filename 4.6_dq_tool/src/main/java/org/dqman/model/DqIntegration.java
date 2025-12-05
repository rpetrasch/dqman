package org.dqman.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class DqIntegration extends FlowComponent {
    private String type; // RDBMS, NOSQL, Web, CSV, Text
    private String url;

    @Column(name = "db_user")
    private String user;

    private String password;
}
