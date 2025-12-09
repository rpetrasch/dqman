import { Component, Inject, ChangeDetectorRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DataIntegration, IntegrationTypeOptions } from '../../models/data-integration.model';
import { DataIntegrationService } from '../../services/data-integration.service';

@Component({
  selector: 'app-integration-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
    FormsModule,
    CommonModule
  ],
  templateUrl: './integration-dialog.component.html',
  styles: [`
    .full-width {
      width: 100%;
    }
    .form-row {
      display: flex;
      gap: 16px;
    }
    .half-width {
      flex: 1;
    }
    .quarter-width {
      width: 25%;
    }
    .three-quarter-width {
      width: 75%;
    }
    mat-form-field {
      margin-bottom: 8px;
    }
    .test-result {
      margin-top: 16px;
      padding: 12px;
      border-radius: 4px;
      background-color: #f5f5f5;
      max-height: 200px;
      overflow-y: auto;
    }
    .test-error {
      background-color: #ffebee;
      color: #c62828;
    }
    .test-success {
      background-color: #e8f5e9;
      color: #2e7d32;
    }
    .test-loading {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 16px;
    }
  `]
})
export class IntegrationDialogComponent {
  integration: DataIntegration;
  typeOptions = IntegrationTypeOptions;
  isEditMode: boolean;
  typeDefault = this.typeOptions[0];
  isTesting = false;
  testResult: string[] | null = null;
  testError: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<IntegrationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DataIntegration | null,
    private integrationService: DataIntegrationService,
    private cdr: ChangeDetectorRef
  ) {
    if (data) {
      this.integration = { ...data };
      this.isEditMode = true;
    } else {
      this.integration = {
        name: '',
        description: '',
        type: this.typeDefault,
        url: '',
        user: '',
        password: ''
      };
      this.isEditMode = false;
    }
  }

  onTypeChange(newType: string): void {
    this.integration.type = newType;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.integration);
  }

  onTest(): void {
    console.log('Test button clicked');
    this.isTesting = true;
    this.testResult = null;
    this.testError = null;

    console.log('Integration state:', this.integration);

    // If the integration doesn't have an ID yet, we need to save it first
    if (!this.integration.id) {
      console.log('Creating new integration before testing...');
      // Save the integration first
      this.integrationService.createIntegration(this.integration).subscribe({
        next: (savedIntegration) => {
          console.log('Integration created:', savedIntegration);
          this.integration = savedIntegration;
          this.isEditMode = true;
          this.fetchMetadata(savedIntegration.id!);
        },
        error: (error) => {
          console.error('Failed to create integration:', error);
          setTimeout(() => {
            this.isTesting = false;
            this.testError = 'Failed to save integration before testing: ' + (error.message || 'Unknown error');
            this.cdr.detectChanges();
          });
        }
      });
    } else {
      console.log('Updating existing integration before testing...');
      // Update existing integration first
      this.integrationService.updateIntegration(this.integration.id, this.integration).subscribe({
        next: () => {
          console.log('Integration updated, fetching metadata...');
          this.fetchMetadata(this.integration.id!);
        },
        error: (error) => {
          console.error('Failed to update integration:', error);
          setTimeout(() => {
            this.isTesting = false;
            this.testError = 'Failed to update integration before testing: ' + (error.message || 'Unknown error');
            this.cdr.detectChanges();
          });
        }
      });
    }
  }

  private fetchMetadata(id: number): void {
    this.integrationService.getMetadata(id).subscribe({
      next: (metadata) => {
        setTimeout(() => {
          this.isTesting = false;
          if (metadata && metadata.length > 0) {
            // Check if the first element is an error message
            if (metadata[0]?.startsWith('Error:')) {
              this.testError = metadata[0];
            } else {
              this.testResult = metadata;
            }
          } else {
            this.testError = 'No metadata returned from the integration';
          }
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        setTimeout(() => {
          this.isTesting = false;
          console.error('Metadata fetch error:', error);

          // Try to extract detailed error message
          let errorMessage = 'Unknown error';
          if (error.error) {
            if (typeof error.error === 'string') {
              errorMessage = error.error;
            } else if (Array.isArray(error.error)) {
              errorMessage = error.error.join(', ');
            } else if (error.error.message) {
              errorMessage = error.error.message;
            }
          } else if (error.message) {
            errorMessage = error.message;
          } else if (error.statusText) {
            errorMessage = error.statusText;
          }

          this.testError = `Failed to fetch metadata (${error.status || 'unknown'}): ${errorMessage}`;
          this.cdr.detectChanges();
        });
      }
    });
  }
}

