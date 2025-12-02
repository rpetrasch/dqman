import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { DqProject } from '../dq-project.model';
import { DqProjectService } from '../dq-project';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  imports: [MatTableModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatDatepickerModule, MatNativeDateModule, FormsModule, CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  projects: DqProject[] = [];
  displayedColumns: string[] = ['name', 'description', 'status', 'createdDate', 'startedDate', 'finishedDate', 'actions'];

  statusOptions = ['CREATED', 'STARTED', 'SUCCESS', 'FAILED'];

  newProject: DqProject = {
    name: '',
    description: '',
    status: 'CREATED',
    createdDate: new Date().toISOString(),
    startedDate: new Date().toISOString(),
    finishedDate: ''
  };

  editingProject: DqProject | null = null;

  constructor(
    private dqProjectService: DqProjectService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.dqProjectService.getAllProjects().subscribe((data: DqProject[]) => {
      this.projects = data;
      this.cdr.detectChanges();
    });
  }

  createProject(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    this.dqProjectService.createProject(this.newProject).subscribe({
      next: () => {
        this.resetForm();
        this.cdr.detectChanges();
        this.loadProjects();
      },
      error: (err) => {
        console.error('Error creating project:', err);
      }
    });
  }

  editProject(project: DqProject): void {
    this.editingProject = { ...project };
  }

  updateProject(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    if (this.editingProject && this.editingProject.id) {
      this.dqProjectService.updateProject(this.editingProject.id, this.editingProject).subscribe({
        next: () => {
          this.editingProject = null;
          this.cdr.detectChanges();
          this.loadProjects();
        },
        error: (err) => {
          console.error('Error updating project:', err);
        }
      });
    }
  }

  deleteProject(id: number | undefined): void {
    if (id) {
      this.dqProjectService.deleteProject(id).subscribe({
        next: () => {
          this.loadProjects();
        },
        error: (err) => {
          console.error('Error deleting project:', err);
        }
      });
    }
  }

  resetForm(): void {
    this.newProject = {
      name: '',
      description: '',
      status: 'CREATED',
      createdDate: new Date().toISOString(),
      startedDate: new Date().toISOString(),
      finishedDate: ''
    };
  }

  cancelEdit(): void {
    this.editingProject = null;
  }
}
