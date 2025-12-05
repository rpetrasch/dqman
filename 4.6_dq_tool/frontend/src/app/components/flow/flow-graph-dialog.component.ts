import { Component, Inject, OnInit, AfterViewInit, ViewChild, ElementRef, ViewEncapsulation, NgZone, ChangeDetectorRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { DqFlow, DqFlowStep } from '../../models/dq-flow.model';
import { DqFlowService, DrawflowNodeConfig } from '../../services/dq-flow.service';
import { FlowStepDialogComponent } from './flow-step-dialog.component';
import Drawflow from 'drawflow';

@Component({
    selector: 'app-flow-graph-dialog',
    standalone: true,
    imports: [
        MatDialogModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        CommonModule
    ],
    templateUrl: './flow-graph-dialog.component.html',
    styleUrls: ['./flow-graph-dialog.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class FlowGraphDialogComponent implements AfterViewInit {
    @ViewChild('drawflow') drawflowElement!: ElementRef;
    editor: any;
    flow: DqFlow;
    selectedNodeId: number | null = null;
    isExecuting = false;
    executionResult: string | null = null;
    executionError: string | null = null;

    constructor(
        public dialogRef: MatDialogRef<FlowGraphDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: DqFlow,
        private dqFlowService: DqFlowService,
        private dialog: MatDialog,
        private ngZone: NgZone,
        private cdr: ChangeDetectorRef
    ) {
        this.flow = data;
    }

    ngAfterViewInit(): void {
        if (this.drawflowElement) {
            this.editor = new Drawflow(this.drawflowElement.nativeElement);
            this.editor.reroute = true;
            this.editor.start();

            // Track node selection using click events
            this.editor.on('nodeSelected', (id: number) => {
                this.ngZone.run(() => {
                    console.log('Node selected:', id);
                    this.selectedNodeId = id;
                    this.cdr.detectChanges();
                });
            });

            this.editor.on('nodeUnselected', () => {
                this.ngZone.run(() => {
                    console.log('Node unselected');
                    this.selectedNodeId = null;
                    this.cdr.detectChanges();
                });
            });

            // Add click handler to track selection manually as backup
            this.editor.on('click', (e: any) => {
                console.log('Editor clicked', e);
            });

            this.loadFlow();
        }
    }

    loadFlow() {
        if (!this.editor) return;

        this.editor.clear(); // Clear existing nodes before loading
        this.selectedNodeId = null; // Reset selection
        this.cdr.detectChanges();

        const nodes = this.dqFlowService.getFlowGraphNodes(this.flow);
        const nodeIds: number[] = [];

        nodes.forEach(node => {
            const nodeId = this.editor.addNode(
                node.name,
                node.inputs,
                node.outputs,
                node.posX,
                node.posY,
                node.className,
                node.data,
                node.html,
                node.typenode
            );
            nodeIds.push(nodeId);

            // Add double-click handler to each node after it's created
            setTimeout(() => {
                const nodeElement = document.getElementById(`node-${nodeId}`);
                if (nodeElement) {
                    nodeElement.addEventListener('dblclick', () => {
                        this.ngZone.run(() => {
                            console.log('Node double-clicked:', nodeId);
                            this.onEditStep(nodeId);
                        });
                    });

                    nodeElement.addEventListener('click', (e) => {
                        this.ngZone.run(() => {
                            console.log('Node clicked:', nodeId);
                            this.selectedNodeId = nodeId;
                            this.cdr.detectChanges();
                        });
                    });
                }
            }, 100);
        });

        // Add linear connections
        for (let i = 0; i < nodeIds.length - 1; i++) {
            const sourceId = nodeIds[i];
            const targetId = nodeIds[i + 1];
            // Connect output_1 of source to input_1 of target
            this.editor.addConnection(sourceId, targetId, 'output_1', 'input_1');
        }
    }

    onAddStep(): void {
        const dialogRef = this.dialog.open(FlowStepDialogComponent, {
            width: '600px',
            data: null
        });

        dialogRef.afterClosed().subscribe((result: DqFlowStep) => {
            if (result) {
                // Add step to flow
                if (!this.flow.steps) {
                    this.flow.steps = [];
                }
                this.flow.steps.push(result);

                // Reload graph
                this.loadFlow();
            }
        });
    }

    onEditStep(nodeId: number): void {
        const nodeData = this.editor.getNodeFromId(nodeId);
        const stepIndex = nodeData?.data?.stepIndex;

        if (stepIndex !== undefined && this.flow.steps && this.flow.steps[stepIndex]) {
            const step = this.flow.steps[stepIndex];

            const dialogRef = this.dialog.open(FlowStepDialogComponent, {
                width: '600px',
                data: step
            });

            dialogRef.afterClosed().subscribe((result: DqFlowStep) => {
                if (result) {
                    this.flow.steps[stepIndex] = result;

                    // Reload graph
                    this.loadFlow();
                }
            });
        }
    }

    onDeleteStep(): void {
        if (this.selectedNodeId) {
            const nodeData = this.editor.getNodeFromId(this.selectedNodeId);
            const stepIndex = nodeData?.data?.stepIndex;

            if (stepIndex !== undefined && this.flow.steps) {
                this.flow.steps.splice(stepIndex, 1);

                // Reload graph
                this.loadFlow();
                this.selectedNodeId = null;
            }
        }
    }

    onExecute(): void {
        if (!this.flow.id) {
            this.executionError = 'Flow must be saved before execution';
            return;
        }

        this.isExecuting = true;
        this.executionResult = null;
        this.executionError = null;

        this.dqFlowService.executeFlow(this.flow.id).subscribe({
            next: (result) => {
                this.ngZone.run(() => {
                    this.isExecuting = false;

                    // Format the result for display
                    let output = `Flow "${result.flowName}" Execution Results\n`;
                    output += `${'='.repeat(50)}\n\n`;
                    output += `Status: ${result.status}\n`;
                    output += `Start Time: ${new Date(result.startTime).toLocaleString()}\n`;
                    output += `End Time: ${new Date(result.endTime).toLocaleString()}\n`;
                    output += `Total Steps: ${result.totalSteps}\n\n`;

                    if (result.steps && result.steps.length > 0) {
                        output += `Step Details:\n`;
                        output += `${'-'.repeat(50)}\n`;
                        result.steps.forEach((step: any, index: number) => {
                            output += `\n${index + 1}. ${step.stepName} (${step.stepType})\n`;
                            output += `   Status: ${step.status}\n`;
                            output += `   Message: ${step.message}\n`;
                            if (step.integrationName) {
                                output += `   Integration: ${step.integrationName}\n`;
                            }
                            if (step.ruleName) {
                                output += `   Rule: ${step.ruleName}\n`;
                            }
                        });
                    }

                    this.executionResult = output;
                    this.cdr.detectChanges();
                });
            },
            error: (error) => {
                this.ngZone.run(() => {
                    this.isExecuting = false;
                    this.executionError = `Execution failed: ${error.error?.message || error.message || 'Unknown error'}`;
                    this.cdr.detectChanges();
                });
            }
        });
    }

    onClose(): void {
        this.dialogRef.close(this.flow);
    }
}
