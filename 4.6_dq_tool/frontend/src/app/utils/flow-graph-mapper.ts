/**
 * Bidirectional mapper between Domain Models (DqFlow) and View Models (Drawflow Graph)
 * 
 * This mapper provides clean separation between:
 * - Backend domain model (DqFlow, DqFlowStep)
 * - Frontend view model (DrawflowGraph, DrawflowNode, DrawflowConnection)
 * 
 * All mapping logic is isolated here for testability and maintainability.
 */

import { DqFlow, DqFlowStep } from '../models/dq-flow.model';
import {
    DrawflowNode,
    DrawflowConnection,
    DrawflowGraph,
    DrawflowExportData
} from '../models/view/drawflow-graph.model';

// Re-export view model types for convenience
export type { DrawflowNode, DrawflowConnection, DrawflowGraph, DrawflowExportData };

/**
 * Converts a DqFlow domain model to a DrawflowGraph view model
 */
export class FlowGraphMapper {

    /**
     * Convert DqFlow (domain) to DrawflowGraph (view)
     * @param flow The domain model from backend
     * @returns The view model for Drawflow
     */
    static domainToView(flow: DqFlow): DrawflowGraph {
        if (!flow.id) {
            throw new Error('Flow must have an ID to convert to DrawflowGraph');
        }

        const nodes: DrawflowNode[] = [];
        const connections: DrawflowConnection[] = [];

        // Create nodes from steps
        flow.steps.forEach((step, index) => {
            const node = this.stepToNode(step, index);
            nodes.push(node);
        });

        // Create connections from successor relationships
        // We'll build connections after we have all nodes
        const stepIdToNodeIndex = new Map<number, number>();
        flow.steps.forEach((step, index) => {
            if (step.id) {
                stepIdToNodeIndex.set(step.id, index);
            }
        });

        flow.steps.forEach((step, sourceIndex) => {
            if (step.successorIds && step.successorIds.length > 0) {
                step.successorIds.forEach(successorId => {
                    const targetIndex = stepIdToNodeIndex.get(successorId);
                    if (targetIndex !== undefined) {
                        connections.push({
                            sourceNodeId: sourceIndex, // Using index as temporary nodeId
                            targetNodeId: targetIndex,
                            outputPort: 'output_1',
                            inputPort: 'input_1'
                        });
                    }
                });
            }
        });

        return {
            flowId: flow.id,
            nodes,
            connections
        };
    }

    /**
     * Convert DrawflowGraph (view) back to DqFlow (domain)
     * @param graph The view model from Drawflow
     * @param originalFlow The original flow (to preserve non-graph properties)
     * @returns Updated domain model for backend
     */
    static viewToDomain(graph: DrawflowGraph, originalFlow: DqFlow): DqFlow {
        const updatedFlow: DqFlow = {
            ...originalFlow,
            steps: []
        };

        // Convert nodes back to steps
        const nodeIdToStep = new Map<number, DqFlowStep>();

        graph.nodes.forEach(node => {
            const step = this.nodeToStep(node);
            updatedFlow.steps.push(step);
            nodeIdToStep.set(node.nodeId, step);
        });

        // Clear all successor/predecessor relationships
        updatedFlow.steps.forEach(step => {
            step.successorIds = [];
            step.predecessorIds = [];
        });

        // Rebuild relationships from connections
        graph.connections.forEach(conn => {
            const sourceNode = graph.nodes.find(n => n.nodeId === conn.sourceNodeId);
            const targetNode = graph.nodes.find(n => n.nodeId === conn.targetNodeId);

            if (sourceNode && targetNode) {
                const sourceStep = nodeIdToStep.get(sourceNode.nodeId);
                const targetStep = nodeIdToStep.get(targetNode.nodeId);

                if (sourceStep && targetStep && sourceStep.id && targetStep.id) {
                    // Add successor relationship
                    if (!sourceStep.successorIds) sourceStep.successorIds = [];
                    if (!sourceStep.successorIds.includes(targetStep.id)) {
                        sourceStep.successorIds.push(targetStep.id);
                    }

                    // Add predecessor relationship
                    if (!targetStep.predecessorIds) targetStep.predecessorIds = [];
                    if (!targetStep.predecessorIds.includes(sourceStep.id)) {
                        targetStep.predecessorIds.push(sourceStep.id);
                    }
                }
            }
        });

        return updatedFlow;
    }

    /**
     * Extract DrawflowGraph from Drawflow editor export data
     * @param exportData The raw export from editor.export()
     * @param flowId The flow ID
     * @returns DrawflowGraph view model
     */
    static fromDrawflowExport(exportData: DrawflowExportData, flowId: number): DrawflowGraph {
        const nodes: DrawflowNode[] = [];
        const connections: DrawflowConnection[] = [];
        const drawflowData = exportData.drawflow.Home.data;

        // Extract nodes
        Object.keys(drawflowData).forEach(nodeIdStr => {
            const nodeId = parseInt(nodeIdStr);
            const nodeData = drawflowData[nodeId];

            const node: DrawflowNode = {
                nodeId: nodeId,
                stepId: nodeData.data?.stepId || null,
                name: nodeData.name,
                type: nodeData.data?.type || 'UNKNOWN',
                description: nodeData.data?.description,
                posX: nodeData.pos_x,
                posY: nodeData.pos_y,
                className: nodeData.class,
                inputs: Object.keys(nodeData.inputs || {}).length,
                outputs: Object.keys(nodeData.outputs || {}).length,
                html: nodeData.html,
                metadata: nodeData.data?.metadata
            };

            nodes.push(node);

            // Extract connections from outputs
            if (nodeData.outputs) {
                Object.keys(nodeData.outputs).forEach(outputKey => {
                    const output = nodeData.outputs[outputKey];
                    if (output.connections) {
                        output.connections.forEach((conn: any) => {
                            connections.push({
                                sourceNodeId: nodeId,
                                targetNodeId: parseInt(conn.node),
                                outputPort: outputKey,
                                inputPort: conn.output
                            });
                        });
                    }
                });
            }
        });

        return {
            flowId,
            nodes,
            connections
        };
    }

    /**
     * Convert a DqFlowStep to a DrawflowNode
     */
    private static stepToNode(step: DqFlowStep, index: number): DrawflowNode {
        const className = this.getNodeClassName(step.type);
        const html = this.generateNodeHtml(step);

        // Determine inputs and outputs based on isInitial/isFinal flags
        // If flags are not set, default to: first step is initial, last step is final
        const isInitial = step.isInitial !== undefined ? step.isInitial : false;
        const isFinal = step.isFinal !== undefined ? step.isFinal : false;

        return {
            nodeId: index, // Temporary, will be assigned by Drawflow
            stepId: step.id || null,
            name: step.name,
            type: step.type,
            description: step.description,
            posX: step.posX || this.calculateAutoPosition(index).x,
            posY: step.posY || this.calculateAutoPosition(index).y,
            className: className,
            inputs: isInitial ? 0 : 1,  // Initial steps have no inputs
            outputs: isFinal ? 0 : 1,   // Final steps have no outputs
            html: html,
            metadata: {
                integrationId: (step as any).integrationId,
                ruleId: (step as any).ruleId,
                transformationId: (step as any).transformationId,
                isInitial: step.isInitial,
                isFinal: step.isFinal
            }
        };
    }

    /**
     * Convert a DrawflowNode back to a DqFlowStep
     */
    private static nodeToStep(node: DrawflowNode): DqFlowStep {
        const step: DqFlowStep = {
            id: node.stepId || undefined,
            name: node.name,
            type: node.type,
            description: node.description || '',
            posX: node.posX,
            posY: node.posY,
            successorIds: [],
            predecessorIds: [],
            isInitial: node.metadata?.isInitial,
            isFinal: node.metadata?.isFinal,
            integrationId: node.metadata?.integrationId,
            ruleId: node.metadata?.ruleId,
            transformationId: node.metadata?.transformationId
        };

        return step;
    }

    /**
     * Get CSS class name based on step type
     */
    private static getNodeClassName(type: string): string {
        const classMap: { [key: string]: string } = {
            'DATA SOURCE': 'node-data-source',
            'DQ RULE': 'node-dq-rule',
            'TRANSFORMATION': 'node-transformation',
            'DECISION': 'node-decision',
            'DATA SINK': 'node-data-sink'
        };
        return classMap[type] || 'node-default';
    }

    /**
     * Generate HTML content for a node
     */
    private static generateNodeHtml(step: DqFlowStep): string {
        const icon = this.getStepIcon(step.type);
        return `
            <div class="node-content">
                <div class="node-icon">${icon}</div>
                <div class="node-title">${step.name}</div>
                <div class="node-type">${step.type}</div>
            </div>
        `;
    }

    /**
     * Get icon for step type
     */
    private static getStepIcon(type: string): string {
        const iconMap: { [key: string]: string } = {
            'DATA SOURCE': '📥',
            'DQ RULE': '✓',
            'TRANSFORMATION': '⚙️',
            'DECISION': '🔀',
            'DATA SINK': '📤'
        };
        return iconMap[type] || '📋';
    }

    /**
     * Calculate auto-layout position for a step
     */
    private static calculateAutoPosition(index: number): { x: number; y: number } {
        const HORIZONTAL_SPACING = 300;
        const VERTICAL_SPACING = 150;
        const START_X = 100;
        const START_Y = 100;
        const NODES_PER_ROW = 3;

        const row = Math.floor(index / NODES_PER_ROW);
        const col = index % NODES_PER_ROW;

        return {
            x: START_X + (col * HORIZONTAL_SPACING),
            y: START_Y + (row * VERTICAL_SPACING)
        };
    }
}
