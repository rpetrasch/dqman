import { Component, Inject, OnInit, AfterViewInit, ViewChild, ElementRef, ViewEncapsulation, NgZone, ChangeDetectorRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { DqFlow, DqFlowStep } from '../../models/dq-flow.model';
import { DqFlowService, DrawflowNodeConfig } from '../../services/dq-flow.service';
import { FlowStepDialogComponent } from './flow-step-dialog.component';
import { formatDateTime } from '../../utils/date-utils';
import { FlowGraphMapper, DrawflowNode, DrawflowConnection } from '../../utils/flow-graph-mapper';
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
    validationResult: string | null = null;
    validationErrors: string[] = [];
    hasUnsavedChanges = false;  // Track if flow has been modified since last save
    isLoadingFlow = false;  // Flag to prevent tracking changes during load operations

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
            this.editor.curvature = 0.5; // Enable curved connections
            this.editor.start();

            // Track node selection using click events
            this.editor.on('nodeSelected', (id: number) => {
                // Drawflow events (like node clicks, selections) happen outside Angular's zone
                // => Angular doesn't automatically detect changes
                this.ngZone.run(() => {
                    console.log('Node selected:', id);
                    this.selectedNodeId = id;
                    this.cdr.detectChanges();  // => Manually trigger change detection
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

            // Track connection changes
            this.editor.on('connectionCreated', () => {
                this.ngZone.run(() => {
                    console.log('Connection created');
                    // Only mark as changed if not loading
                    if (!this.isLoadingFlow) {
                        this.hasUnsavedChanges = true;
                    }
                });
            });

            this.editor.on('connectionRemoved', () => {
                this.ngZone.run(() => {
                    console.log('Connection removed');
                    // Only mark as changed if not loading
                    if (!this.isLoadingFlow) {
                        this.hasUnsavedChanges = true;
                    }
                });
            });

            this.loadFlow();
        }
    }


    loadFlow(): void {
        if (!this.editor) return;

        // Set flag to prevent tracking changes during load
        this.isLoadingFlow = true;

        console.log('=== Loading flow ===');
        console.log('Domain model:', this.flow);
        console.log('RAW STEPS FROM BACKEND:', this.flow.steps.map((s, i) => ({
            index: i,
            name: s.name,
            id: s.id,
            hasId: !!s.id,
            isInitial: s.isInitial,
            isFinal: s.isFinal,
            successorIds: s.successorIds
        })));

        // Convert domain model to view model using mapper
        const graph = FlowGraphMapper.domainToView(this.flow);
        console.log('View model (graph):', graph);

        // Clear existing graph
        this.editor.clear();
        this.selectedNodeId = null;
        this.cdr.detectChanges();

        // Map to track view node index -> Drawflow node ID
        const viewNodeIndexToDrawflowId = new Map<number, number>();

        // Add nodes to Drawflow
        graph.nodes.forEach((node: DrawflowNode, index: number) => {
            console.log(`Adding node ${index}: ${node.name} (stepId: ${node.stepId})`);

            const drawflowNodeId = this.editor.addNode(
                node.name,
                node.inputs,
                node.outputs,
                node.posX,
                node.posY,
                node.className,
                {
                    stepId: node.stepId,
                    stepIndex: index,
                    type: node.type,
                    description: node.description,
                    metadata: node.metadata  // Include metadata to preserve isInitial, isFinal, etc.
                },
                node.html,
                false
            );

            viewNodeIndexToDrawflowId.set(index, drawflowNodeId);

            // Add event handlers to the node
            setTimeout(() => {
                const nodeElement = document.getElementById(`node-${drawflowNodeId}`);
                if (nodeElement) {
                    nodeElement.addEventListener('dblclick', () => {
                        this.ngZone.run(() => {
                            console.log('Node double-clicked:', drawflowNodeId);
                            this.onEditStep(drawflowNodeId);
                        });
                    });

                    nodeElement.addEventListener('click', () => {
                        this.ngZone.run(() => {
                            console.log('Node clicked:', drawflowNodeId);
                            this.selectedNodeId = drawflowNodeId;
                            this.cdr.detectChanges();
                        });
                    });
                }
            }, 100);
        });

        // Add connections after nodes are rendered
        setTimeout(() => {
            console.log(`Adding ${graph.connections.length} connections`);

            graph.connections.forEach((conn: DrawflowConnection, index: number) => {
                const sourceDrawflowId = viewNodeIndexToDrawflowId.get(conn.sourceNodeId);
                const targetDrawflowId = viewNodeIndexToDrawflowId.get(conn.targetNodeId);

                if (sourceDrawflowId !== undefined && targetDrawflowId !== undefined) {
                    console.log(`  Connection ${index}: Drawflow node ${sourceDrawflowId} -> ${targetDrawflowId}`);
                    try {
                        this.editor.addConnection(
                            sourceDrawflowId,
                            targetDrawflowId,
                            conn.outputPort,
                            conn.inputPort
                        );
                    } catch (e) {
                        console.warn('Failed to add connection:', e);
                    }
                } else {
                    console.error(`  ERROR: Could not map connection ${index} - source: ${sourceDrawflowId}, target: ${targetDrawflowId}`);
                }
            });

            // Force Drawflow to recalculate connection paths
            setTimeout(() => {
                if (this.editor && this.editor.updateConnectionNodes) {
                    Object.keys(this.editor.drawflow.drawflow.Home.data).forEach((nodeId: string) => {
                        this.editor.updateConnectionNodes('node-' + nodeId);
                    });
                }

                // Clear the loading flag after all connections are added
                this.isLoadingFlow = false;
            }, 50);

            console.log('=== Flow loaded successfully ===');
        }, 100);
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
                this.hasUnsavedChanges = true;  // Mark as changed

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
                    this.hasUnsavedChanges = true;  // Mark as changed

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
                this.hasUnsavedChanges = true;  // Mark as changed

                // Reload graph
                this.loadFlow();
                this.selectedNodeId = null;
            }
        }
    }

    onValidate(): void {
        this.validationResult = null;
        this.validationErrors = [];

        if (!this.flow.id) {
            this.validationErrors = ['Flow must be saved before validation'];
            this.cdr.detectChanges(); // Run outside Angular zone, need to do manual cdr call
            return;
        }

        // Warn if there are unsaved changes
        if (this.hasUnsavedChanges) {
            this.validationErrors = [
                '⚠️ Warning: You have unsaved changes.',
                'Validation will check the last saved version, not your current changes.',
                'Please save the flow first to validate the current state.'
            ];
            this.cdr.detectChanges();
            return;
        }

        // Call backend validation endpoint
        this.dqFlowService.validateFlowById(this.flow.id).subscribe({
            next: (result) => {
                if (result.valid) {
                    this.validationResult = '✓ Flow is valid and ready for execution';
                } else {
                    this.validationErrors = result.errors;
                }
                this.cdr.detectChanges();
            },
            error: (err) => {
                this.validationErrors = ['Validation failed: ' + err.message];
                this.cdr.detectChanges();
            }
        });
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
                    output += `${'='.repeat(30)}\n\n`;
                    output += `Status: ${result.status}\n`;
                    output += `Start Time: ${formatDateTime(result.startTime)}\n`;
                    output += `End Time: ${formatDateTime(result.endTime)}\n`;
                    output += `Total Steps: ${result.totalSteps}\n\n`;

                    if (result.steps && result.steps.length > 0) {
                        output += `Step Details:\n`;
                        output += `${'-'.repeat(30)}\n`;
                        result.steps.forEach((step: any, index: number) => {
                            output += `\n${index + 1}. ${step.stepName} (${step.stepType})\n`;
                            output += `   Status: ${step.status}\n`;

                            // Display step data if available
                            if (step.data) {
                                output += `   Data:\n`;
                                // Format the data object nicely
                                Object.keys(step.data).forEach(key => {
                                    const value = step.data[key];
                                    if (Array.isArray(value)) {
                                        output += `     ${key}: ${value.length} rows\n`;
                                        // Show first few rows as preview
                                        if (value.length > 0 && value.length <= 10) {
                                            value.forEach((row: any, rowIndex: number) => {
                                                output += `       ${rowIndex}: ${JSON.stringify(row)}\n`;
                                            });
                                        } else if (value.length > 10) {
                                            output += `       (showing first 5 rows)\n`;
                                            value.slice(0, 5).forEach((row: any, rowIndex: number) => {
                                                output += `       ${rowIndex}: ${JSON.stringify(row)}\n`;
                                            });
                                            output += `       ... and ${value.length - 5} more rows\n`;
                                        }
                                    } else {
                                        output += `     ${key}: ${value}\n`;
                                    }
                                });
                            }

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

    onSave(): void {
        this.saveFlow()
    }

    onSaveAndClose(): void {
        this.saveFlow()
        this.dialogRef.close(this.flow);
    }

    saveFlow(): void {
        // Ensure flow has an ID before saving
        if (!this.flow.id) {
            console.error('Cannot save flow without ID');
            return;
        }

        const flowId = this.flow.id;

        console.log('=== Saving flow (TWO-PHASE SAVE) ===');

        // Export current Drawflow state
        const exportData = this.editor.export();
        console.log('Drawflow export:', exportData);

        // Convert Drawflow export to view model
        const graph = FlowGraphMapper.fromDrawflowExport(exportData, flowId);
        console.log('Extracted graph from Drawflow:', graph);

        // Convert view model back to domain model
        const updatedFlow = FlowGraphMapper.viewToDomain(graph, this.flow);

        // DETAILED DIAGNOSTIC LOGGING
        console.log('=== DETAILED STEP ANALYSIS ===');
        updatedFlow.steps.forEach((step, index) => {
            console.log(`Step ${index}:`, {
                name: step.name,
                id: step.id,
                hasId: !!step.id,
                successorIds: step.successorIds,
                predecessorIds: step.predecessorIds
            });
        });

        console.log('=== CONNECTION ANALYSIS (BEFORE FIRST SAVE) ===');
        graph.connections.forEach((conn, index) => {
            const sourceNode = graph.nodes.find(n => n.nodeId === conn.sourceNodeId);
            const targetNode = graph.nodes.find(n => n.nodeId === conn.targetNodeId);
            console.log(`Connection ${index}:`, {
                source: sourceNode?.name,
                sourceStepId: sourceNode?.stepId,
                sourceHasId: !!sourceNode?.stepId,
                target: targetNode?.name,
                targetStepId: targetNode?.stepId,
                targetHasId: !!targetNode?.stepId,
                willBeSaved: !!(sourceNode?.stepId && targetNode?.stepId),
                reason: !(sourceNode?.stepId && targetNode?.stepId) ? 'One or both steps missing ID' : 'OK'
            });
        });

        // Check if we have any new steps (steps without IDs)
        const hasNewSteps = updatedFlow.steps.some(step => !step.id);
        const hasConnections = graph.connections.length > 0;

        if (hasNewSteps && hasConnections) {
            console.log('⚠️ DETECTED NEW STEPS WITH CONNECTIONS - Using two-phase save');

            // PHASE 1: Save to get IDs for new steps
            console.log('=== PHASE 1: Saving steps to get IDs ===');
            this.dqFlowService.updateFlow(flowId, updatedFlow).subscribe({
                next: (savedFlow) => {
                    this.ngZone.run(() => {
                        console.log('✓ Phase 1 complete - New steps now have IDs');
                        this.flow = savedFlow;

                        // Update the Drawflow graph with new step IDs
                        // We need to map old node IDs to new step IDs
                        const stepIndexToNewId = new Map<number, number>();
                        savedFlow.steps.forEach((step, index) => {
                            if (step.id) {
                                stepIndexToNewId.set(index, step.id);
                            }
                        });

                        console.log('Step index to new ID mapping:', Array.from(stepIndexToNewId.entries()));

                        // Update node data in Drawflow with new step IDs
                        Object.keys(this.editor.drawflow.drawflow.Home.data).forEach(nodeIdStr => {
                            const nodeId = parseInt(nodeIdStr);
                            const nodeData = this.editor.drawflow.drawflow.Home.data[nodeId];
                            const stepIndex = nodeData.data?.stepIndex;

                            if (stepIndex !== undefined) {
                                const newStepId = stepIndexToNewId.get(stepIndex);
                                if (newStepId) {
                                    nodeData.data.stepId = newStepId;
                                    console.log(`Updated Drawflow node ${nodeId} with stepId ${newStepId}`);
                                }
                            }
                        });

                        // PHASE 2: Re-extract and save connections
                        console.log('=== PHASE 2: Saving connections ===');

                        // Re-export Drawflow (now with updated step IDs)
                        const updatedExportData = this.editor.export();
                        const updatedGraph = FlowGraphMapper.fromDrawflowExport(updatedExportData, flowId);
                        const finalFlow = FlowGraphMapper.viewToDomain(updatedGraph, savedFlow);

                        console.log('=== CONNECTION ANALYSIS (AFTER PHASE 1) ===');
                        updatedGraph.connections.forEach((conn, index) => {
                            const sourceNode = updatedGraph.nodes.find(n => n.nodeId === conn.sourceNodeId);
                            const targetNode = updatedGraph.nodes.find(n => n.nodeId === conn.targetNodeId);
                            console.log(`Connection ${index}:`, {
                                source: sourceNode?.name,
                                sourceStepId: sourceNode?.stepId,
                                target: targetNode?.name,
                                targetStepId: targetNode?.stepId,
                                willBeSaved: !!(sourceNode?.stepId && targetNode?.stepId)
                            });
                        });

                        console.log('Final domain model to save:', {
                            name: finalFlow.name,
                            steps: finalFlow.steps.map(s => ({
                                name: s.name,
                                id: s.id,
                                isInitial: s.isInitial,
                                isFinal: s.isFinal,
                                successorIds: s.successorIds,
                                predecessorIds: s.predecessorIds
                            }))
                        });

                        // Second save with connections
                        this.dqFlowService.updateFlow(flowId, finalFlow).subscribe({
                            next: (finalSavedFlow) => {
                                this.ngZone.run(() => {
                                    console.log('✓ Phase 2 complete - Connections saved');
                                    this.flow = finalSavedFlow;
                                    this.hasUnsavedChanges = false;  // Clear the flag after successful save
                                    console.log('✅ TWO-PHASE SAVE COMPLETE');

                                    // Reload to show saved state
                                    this.loadFlow();
                                    this.cdr.detectChanges();
                                });
                            },
                            error: (error) => {
                                this.ngZone.run(() => {
                                    console.error('❌ Phase 2 failed:', error);
                                    this.cdr.detectChanges();
                                });
                            }
                        });
                    });
                },
                error: (error) => {
                    this.ngZone.run(() => {
                        console.error('❌ Phase 1 failed:', error);
                        this.cdr.detectChanges();
                    });
                }
            });
        } else {
            // No new steps or no connections - single save is fine
            console.log('✓ No new steps detected - Using single-phase save');

            console.log('Domain model to save:', {
                name: updatedFlow.name,
                steps: updatedFlow.steps.map(s => ({
                    name: s.name,
                    id: s.id,
                    successorIds: s.successorIds,
                    predecessorIds: s.predecessorIds
                }))
            });

            // Save to backend
            this.dqFlowService.updateFlow(flowId, updatedFlow).subscribe({
                next: (savedFlow) => {
                    this.ngZone.run(() => {
                        this.flow = savedFlow;
                        this.hasUnsavedChanges = false;  // Clear the flag after successful save
                        console.log('✅ Flow saved successfully');

                        // Reload to show saved state
                        this.loadFlow();
                        this.cdr.detectChanges();
                    });
                },
                error: (error) => {
                    this.ngZone.run(() => {
                        console.error('❌ Error saving flow:', error);
                        this.cdr.detectChanges();
                    });
                }
            });
        }
    }

    onAutoLayout(): void {
        // Clear positions to trigger auto-layout
        this.flow.steps.forEach(step => {
            step.posX = undefined;
            step.posY = undefined;
        });

        // Reload the graph with auto-calculated positions
        this.loadFlow();
    }

    onClose(): void {
        this.dialogRef.close(this.flow);
    }
}
