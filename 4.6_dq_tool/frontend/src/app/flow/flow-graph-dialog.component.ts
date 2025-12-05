
import { Component, Inject, OnInit, AfterViewInit, ViewChild, ElementRef, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { DqFlow } from '../dq-flow.model';
import { DqFlowService, DrawflowNodeConfig } from '../dq-flow.service';
import Drawflow from 'drawflow';

@Component({
    selector: 'app-flow-graph-dialog',
    standalone: true,
    imports: [
        MatDialogModule,
        MatButtonModule,
        MatIconModule,
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

    constructor(
        public dialogRef: MatDialogRef<FlowGraphDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: DqFlow,
        private dqFlowService: DqFlowService
    ) {
        this.flow = data;
    }

    ngAfterViewInit(): void {
        if (this.drawflowElement) {
            this.editor = new Drawflow(this.drawflowElement.nativeElement);
            this.editor.reroute = true;
            this.editor.start();

            // Example: Load existing steps as nodes if logic exists
            // For now, we start with a blank canvas or basic setup
            this.loadFlow();
        }
    }

    loadFlow() {
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
        });

        // Add linear connections
        for (let i = 0; i < nodeIds.length - 1; i++) {
            const sourceId = nodeIds[i];
            const targetId = nodeIds[i + 1];
            // Connect output_1 of source to input_1 of target
            this.editor.addConnection(sourceId, targetId, 'output_1', 'input_1');
        }
    }

    onClose(): void {
        this.dialogRef.close();
    }
}
