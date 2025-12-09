
export const FlowStatusOptions = ['CREATED', 'VALIDATED', 'PRODUCTION', 'ARCHIVED'];
export const FlowStepTypeOptions = ['DATA SOURCE', 'DQ RULE', 'TRANSFORMATION', 'DECISION', 'DATA SINK'];

export interface DqFlowStep {
    id?: number; // Might be null for new steps not yet persisted
    name: string;
    description: string;
    type: string;
    isInitial?: boolean; // Manual override for initial step (no inputs)
    isFinal?: boolean; // Manual override for final step (no outputs)
    successorIds?: number[]; // IDs of successor steps
    predecessorIds?: number[]; // IDs of predecessor steps
    posX?: number; // X position in graph
    posY?: number; // Y position in graph
    integrationId?: number; // For DATA SOURCE and DATA SINK steps
    ruleId?: number; // For DQ RULE steps
    transformationId?: number; // For TRANSFORMATION steps
}

export interface DqFlow {
    id?: number;
    name: string;
    description: string;
    steps: DqFlowStep[];
    status: string;
    createdDate?: string;
    modifiedDate?: string;
}
