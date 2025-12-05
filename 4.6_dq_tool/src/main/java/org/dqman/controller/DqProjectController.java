package org.dqman.controller;

import org.dqman.model.DqProject;
import org.dqman.repository.DqProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200")
public class DqProjectController {

    @Autowired
    private DqProjectRepository dqProjectRepository;

    @GetMapping
    public List<DqProject> getAllProjects() {
        return dqProjectRepository.findAll();
    }

    @PostMapping
    public DqProject createProject(@RequestBody DqProject project) {
        return dqProjectRepository.save(project);
    }

    @GetMapping("/{id}")
    public DqProject getProjectById(@PathVariable Long id) {
        return dqProjectRepository.findById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public DqProject updateProject(@PathVariable Long id, @RequestBody DqProject projectDetails) {
        DqProject project = dqProjectRepository.findById(id).orElse(null);
        if (project != null) {
            project.setName(projectDetails.getName());
            project.setDescription(projectDetails.getDescription());
            project.setStatus(projectDetails.getStatus());
            project.setCreatedDate(projectDetails.getCreatedDate());
            project.setStartedDate(projectDetails.getStartedDate());
            project.setFinishedDate(projectDetails.getFinishedDate());
            return dqProjectRepository.save(project);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        dqProjectRepository.deleteById(id);
    }
}
