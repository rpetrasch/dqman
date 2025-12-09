import { Component, OnInit, ChangeDetectorRef, ViewChild, AfterViewInit } from '@angular/core';
import { DqProject } from '../../models/dq-project.model';
import { DqProjectService } from '../../services/dq-project.service';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CommonModule } from '@angular/common';
import { ProjectDialogComponent } from './project-dialog.component';
import { ConfirmationDialogComponent } from '../shared/confirmation-dialog/confirmation-dialog.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    CommonModule,
    FormsModule
  ],
  templateUrl: './projects.html',
  styleUrl: './projects.css'
})
export class ProjectsComponent implements OnInit, AfterViewInit {
  dataSource = new MatTableDataSource<DqProject>([]);
  displayedColumns: string[] = ['id', 'name', 'description', 'status', 'createdDate', 'startedDate', 'finishedDate', 'actions'];

  @ViewChild(MatSort) sort!: MatSort;

  // Filter values
  globalFilter = '';
  columnFilters = {
    id: '',
    name: '',
    description: '',
    status: ''
  };

  constructor(
    private dqProjectService: DqProjectService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.dataSource.filterPredicate = this.createFilter();
    this.loadProjects();
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
  }

  loadProjects(): void {
    this.dqProjectService.getAllProjects().subscribe((data: DqProject[]) => {
      this.dataSource.data = data;
      this.cdr.detectChanges();
    });
  }

  createFilter(): (data: DqProject, filter: string) => boolean {
    return (data: DqProject, filter: string): boolean => {
      const searchTerms = JSON.parse(filter);

      // Global Filter (matches any of the main fields)
      const globalMatch = !searchTerms.global ||
        (data.id?.toString().toLowerCase().includes(searchTerms.global) ||
          data.name.toLowerCase().includes(searchTerms.global) ||
          data.description?.toLowerCase().includes(searchTerms.global) ||
          data.status.toLowerCase().includes(searchTerms.global));

      // Column Filters
      const idMatch = !searchTerms.id || data.id?.toString().toLowerCase().includes(searchTerms.id);
      const nameMatch = !searchTerms.name || data.name.toLowerCase().includes(searchTerms.name);
      const descMatch = !searchTerms.description || data.description?.toLowerCase().includes(searchTerms.description);
      const statusMatch = !searchTerms.status || data.status.toLowerCase().includes(searchTerms.status);

      var result = globalMatch && idMatch && nameMatch && descMatch && statusMatch;
      if (result === undefined) {
        result = true;
      }
      console.log(result);
      return result;
    };
  }

  applyGlobalFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.globalFilter = filterValue.trim().toLowerCase();
    this.updateFilter();
  }

  applyColumnFilter(column: string, event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    // @ts-ignore
    this.columnFilters[column] = filterValue.trim().toLowerCase();
    this.updateFilter();
  }

  updateFilter() {
    this.dataSource.filter = JSON.stringify({
      global: this.globalFilter,
      ...this.columnFilters
    });
  }

  openProjectDialog(project?: DqProject): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '800px',
      data: project || null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (project && project.id) {
          this.updateProject(project.id, result);
        } else {
          this.createProject(result);
        }
      }
    });
  }

  createProject(project: DqProject): void {
    this.dqProjectService.createProject(project).subscribe({
      next: () => {
        this.loadProjects();
      },
      error: (err) => {
        console.error('Error creating project:', err);
      }
    });
  }

  updateProject(id: number, project: DqProject): void {
    this.dqProjectService.updateProject(id, project).subscribe({
      next: () => {
        this.loadProjects();
      },
      error: (err) => {
        console.error('Error updating project:', err);
      }
    });
  }

  deleteProject(id: number | undefined): void {
    if (id) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '350px',
        data: { title: 'Delete Project', message: 'Are you sure you want to delete this project?' }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.dqProjectService.deleteProject(id).subscribe({
            next: () => {
              this.loadProjects();
            },
            error: (err) => {
              console.error('Error deleting project:', err);
            }
          });
        }
      });
    }
  }
}
