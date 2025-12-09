import { Component, OnInit, ChangeDetectorRef, ViewChild, AfterViewInit } from '@angular/core';
import { DataIntegration } from '../../models/data-integration.model';
import { DataIntegrationService } from '../../services/data-integration.service';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { IntegrationDialogComponent } from './integration-dialog.component';
import { ConfirmationDialogComponent } from '../shared/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-integration',
  standalone: true,
  imports: [
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    CommonModule,
    MatDialogModule,
    MatIconModule
  ],
  templateUrl: './integration.html',
  styleUrl: './integration.css'
})
export class IntegrationComponent implements OnInit, AfterViewInit {
  dataSource = new MatTableDataSource<DataIntegration>([]);
  displayedColumns: string[] = ['id', 'name', 'description', 'type', 'url', 'user', 'actions'];

  @ViewChild(MatSort) sort!: MatSort;

  // Filter values
  globalFilter = '';
  columnFilters = {
    id: '',
    name: '',
    description: '',
    type: '',
    url: '',
    user: ''
  };

  constructor(
    private dataIntegrationService: DataIntegrationService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.dataSource.filterPredicate = this.createFilter();
    this.loadIntegrations();
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
  }

  loadIntegrations(): void {
    this.dataIntegrationService.getAllIntegrations().subscribe((data: DataIntegration[]) => {
      this.dataSource.data = data;
      this.cdr.detectChanges();
    });
  }

  createFilter(): (data: DataIntegration, filter: string) => boolean {
    return (data: DataIntegration, filter: string): boolean => {
      const searchTerms = JSON.parse(filter);

      // Global Filter
      const globalMatch = !searchTerms.global ||
        (data.id?.toString().toLowerCase().includes(searchTerms.global) ||
          data.name.toLowerCase().includes(searchTerms.global) ||
          data.description?.toLowerCase().includes(searchTerms.global) ||
          data.type?.toLowerCase().includes(searchTerms.global) ||
          data.url?.toLowerCase().includes(searchTerms.global) ||
          data.user?.toLowerCase().includes(searchTerms.global));

      // Column Filters
      const idMatch = !searchTerms.id || data.id?.toString().toLowerCase().includes(searchTerms.id);
      const nameMatch = !searchTerms.name || data.name.toLowerCase().includes(searchTerms.name);
      const descMatch = !searchTerms.description || data.description?.toLowerCase().includes(searchTerms.description);
      const typeMatch = !searchTerms.type || data.type?.toLowerCase().includes(searchTerms.type);
      const urlMatch = !searchTerms.url || data.url?.toLowerCase().includes(searchTerms.url);
      const userMatch = !searchTerms.user || data.user?.toLowerCase().includes(searchTerms.user);

      var result = globalMatch && idMatch && nameMatch && descMatch && typeMatch && urlMatch && userMatch;
      if (result === undefined) {
        result = true;
      }
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

  openIntegrationDialog(integration?: DataIntegration): void {
    const dialogRef = this.dialog.open(IntegrationDialogComponent, {
      width: '800px',
      data: integration || null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (integration && integration.id) {
          this.updateIntegration(integration.id, result);
        } else {
          this.createIntegration(result);
        }
      }
    });
  }

  createIntegration(integration: DataIntegration): void {
    this.dataIntegrationService.createIntegration(integration).subscribe({
      next: () => {
        this.loadIntegrations();
      },
      error: (err) => {
        console.error('Error creating integration:', err);
      }
    });
  }

  updateIntegration(id: number, integration: DataIntegration): void {
    this.dataIntegrationService.updateIntegration(id, integration).subscribe(() => {
      this.loadIntegrations();
    });
  }

  deleteIntegration(id: number | undefined): void {
    if (id) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '350px',
        data: { title: 'Delete Integration', message: 'Are you sure you want to delete this integration?' }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.dataIntegrationService.deleteIntegration(id).subscribe(() => {
            this.loadIntegrations();
          });
        }
      });
    }
  }

  testIntegration(integration: DataIntegration): void {
    if (integration.id) {
      this.dataIntegrationService.getMetadata(integration.id).subscribe({
        next: (metadata) => {
          console.log('Metadata for integration:', metadata);
          alert('Test successful! Check console for metadata.');
        },
        error: (error) => {
          console.error('Test failed:', error);
          alert('Test failed: ' + (error.error?.message || error.message || 'Unknown error'));
        }
      });
    }
  }
}
