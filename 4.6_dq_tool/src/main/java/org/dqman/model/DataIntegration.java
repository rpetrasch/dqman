package org.dqman.model;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DataIntegration extends FlowStepProcessor {

    private String url;
    @Column(name = "db_user")
    private String user;
    private String password;
    @ElementCollection
    private List<String> metadata;

    public DataIntegration(String name, String description, String type, String status, String url, String user,
            String password, List<String> metadata) {
        super(name, description, type, status);
        this.url = url;
        this.user = user;
        this.password = password;
        this.metadata = metadata;
    }

}
