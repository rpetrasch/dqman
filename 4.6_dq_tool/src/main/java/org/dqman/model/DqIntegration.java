package org.dqman.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class DqIntegration extends FlowComponent {

    private String type; // RDBMS, NOSQL, Web, CSV, Text
    private String url;
    @Column(name = "db_user")
    private String user;
    private String password;
    @ElementCollection
    private List<String> metadataTablesFields;

    public DqIntegration(String type, String url, String user, String password) {
        this.type = type;
        this.url = url;
        this.user = user;
        this.password = password;
    }
}
