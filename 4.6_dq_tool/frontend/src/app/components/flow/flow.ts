
import { Component, OnInit, ChangeDetectorRef, ViewChild, AfterViewInit } from '@angular/core';
import { DqFlow } from '../../models/dq-flow.model';
import { DqFlowService } from '../../services/dq-flow.service';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FlowDialogComponent } from './flow-dialog.component';
import { FlowGraphDialogComponent } from './flow-graph-dialog.component';
import { ConfirmationDialogComponent } from '../shared/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-flow',
  standalone: true,
  imports: [
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatDialogModule,
    FormsModule,
    CommonModule
  ],
  templateUrl: './flow.html',
  styleUrl: './flow.css'
})
export class FlowComponent implements OnInit, AfterViewInit {
  dataSource = new MatTableDataSource<DqFlow>([]);
  displayedColumns: string[] = ['id', 'name', 'description', 'status', 'createdDate', 'modifiedDate', 'actions'];

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
    private dqFlowService: DqFlowService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.dataSource.filterPredicate = this.createFilter();
    this.loadFlows();
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
  }

  loadFlows(): void {
    this.dqFlowService.getAllFlows().subscribe((data: DqFlow[]) => {
      this.dataSource.data = data;
      this.cdr.detectChanges();
    });
  }

  createFilter(): (data: DqFlow, filter: string) => boolean {
    return (data: DqFlow, filter: string): boolean => {
      const searchTerms = JSON.parse(filter);

      // Global Filter
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

  openFlowDialog(flow?: DqFlow): void {
    const dialogRef = this.dialog.open(FlowDialogComponent, {
      width: '800px',
      data: flow || null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (flow && flow.id) {
          this.updateFlow(flow.id, result);
        } else {
          this.createFlow(result);
        }
      }
    });
  }

  createFlow(flow: DqFlow): void {
    this.dqFlowService.createFlow(flow).subscribe({
      next: () => {
        this.loadFlows();
      },
      error: (err) => {
        console.error('Error creating flow:', err);
      }
    });
  }

  updateFlow(id: number, flow: DqFlow): void {
    this.dqFlowService.updateFlow(id, flow).subscribe(() => {
      this.loadFlows();
    });
  }

  openGraphDialog(flow: DqFlow): void {
    const dialogRef = this.dialog.open(FlowGraphDialogComponent, {
      width: '95%',
      height: '90%',
      maxWidth: '100vw',
      data: flow
    });

    // Refresh flows after dialog closes to get updated data
    dialogRef.afterClosed().subscribe(() => {
      this.loadFlows();
    });
  }

  deleteFlow(id: number | undefined): void {
    if (id) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '350px',
        data: { title: 'Delete Flow', message: 'Are you sure you want to delete this flow?' }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.dqFlowService.deleteFlow(id).subscribe(() => {
            this.loadFlows();
          });
        }
      });
    }
  }
}
