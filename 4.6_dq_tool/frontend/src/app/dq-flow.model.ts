
export const FlowStatusOptions = ['CREATED', 'VALIDATED', 'PRODUCTION', 'ARCHIVED'];
export const FlowStepTypeOptions = ['DATA SOURCE', 'DQ RULE', 'TRANSFORMATION', 'DECISION', 'DATA SINK'];

export interface DqFlowStep {
    id?: number; // Might be null for new steps not yet persisted
    name: string;
    description: string;
    type: string;
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
