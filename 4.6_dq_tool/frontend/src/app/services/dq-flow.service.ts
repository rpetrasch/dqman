import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DqFlow, DqFlowStep } from '../models/dq-flow.model';

export interface DrawflowNodeConfig {
    name: string;
    inputs: number;
    outputs: number;
    posX: number;
    posY: number;
    className: string;
    data: any;
    html: string;
    typenode: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class DqFlowService {
    private apiUrl = 'http://localhost:8081/api/flows';

    constructor(private http: HttpClient) { }

    getAllFlows(): Observable<DqFlow[]> {
        return this.http.get<DqFlow[]>(this.apiUrl);
    }

    createFlow(flow: DqFlow): Observable<DqFlow> {
        return this.http.post<DqFlow>(this.apiUrl, flow);
    }

    updateFlow(id: number, flow: DqFlow): Observable<DqFlow> {
        return this.http.put<DqFlow>(`${this.apiUrl}/${id}`, flow);
    }

    deleteFlow(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    executeFlow(id: number): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/${id}/execute`, {});
    }

    validateFlowById(id: number): Observable<{ valid: boolean; errors: string[] }> {
        return this.http.get<{ valid: boolean; errors: string[] }>(`${this.apiUrl}/${id}/validate`);
    }

    // ToDo not used, can be removed. see backend
    validateFlow(flow: DqFlow): { valid: boolean; errors: string[] } {
        const errors: string[] = [];

        if (!flow.steps || flow.steps.length === 0) {
            errors.push('Flow must have at least one step');
            return { valid: false, errors };
        }

        // For a single step, it must be both initial and final
        if (flow.steps.length === 1) {
            const step = flow.steps[0];
            const isInitial = step.isInitial !== undefined ? step.isInitial : true;
            const isFinal = step.isFinal !== undefined ? step.isFinal : true;

            if (!isInitial || !isFinal) {
                errors.push('A single step must be both initial and final');
            }

            return {
                valid: errors.length === 0,
                errors
            };
        }

        // For multiple steps, validate initial and final steps
        let initialCount = 0;
        let finalCount = 0;

        flow.steps.forEach((step, index) => {
            // Determine if step is initial or final (using same logic as getFlowGraphNodes)
            const isInitial = step.isInitial !== undefined ? step.isInitial : (index === 0);
            const isFinal = step.isFinal !== undefined ? step.isFinal : (index === flow.steps.length - 1);

            if (isInitial) initialCount++;
            if (isFinal) finalCount++;

            // Check if step is both initial and final (only allowed for single step flows)
            if (isInitial && isFinal && flow.steps.length > 1) {
                errors.push(`Step "${step.name}" cannot be both initial and final when there are multiple steps`);
            }

            // Check for missing connections
            const hasInputs = step.predecessorIds && step.predecessorIds.length > 0;
            const hasOutputs = step.successorIds && step.successorIds.length > 0;

            // Non-initial steps must have at least one input
            if (!isInitial && !hasInputs) {
                errors.push(`Step "${step.name}" is not marked as initial but has no input connections`);
            }

            // Non-final steps must have at least one output
            if (!isFinal && !hasOutputs) {
                errors.push(`Step "${step.name}" is not marked as final but has no output connections`);
            }

            if ((step.type === 'DATA SOURCE' || step.type === 'DATA SINK') && !step.integrationId) {
                errors.push(`Step "${step.name}" is a Data Source or Data Sink but has no integration selected`);
            }

            if (step.type === 'DQ RULE' && !step.ruleId) {
                errors.push(`Step "${step.name}" is a DQ Rule but has no rule selected`);
            }

        });

        // Check for at least one initial step
        if (initialCount === 0) {
            errors.push('Flow must have at least one initial step (no inputs)');
        }

        // Check for at least one final step
        if (finalCount === 0) {
            errors.push('Flow must have at least one final step (no outputs)');
        }

        return {
            valid: errors.length === 0,
            errors
        };
    }


    getFlowGraphNodes(flow: DqFlow): DrawflowNodeConfig[] {
        const nodes: DrawflowNodeConfig[] = [];
        let x = 50;
        const y = 100;
        const gap = 300;

        if (!flow.steps || flow.steps.length === 0) {
            return [];
        }

        flow.steps.forEach((step, index) => {
            // Check for manual override first, otherwise use position-based logic
            const isFirst = step.isInitial !== undefined ? step.isInitial : (index === 0);
            const isLast = step.isFinal !== undefined ? step.isFinal : (index === flow.steps.length - 1);

            // Use saved position if available, otherwise calculate default position
            const posX = step.posX !== undefined ? step.posX : x;
            const posY = step.posY !== undefined ? step.posY : y;

            nodes.push({
                name: step.type, // Identifier for the node
                inputs: isFirst ? 0 : 1,
                outputs: isLast ? 0 : 1,
                posX: posX,
                posY: posY,
                className: this.getClassNameForType(step.type),
                data: { ...step, stepIndex: index }, // Add stepIndex to data
                html: `
                  <div class="flow-step-node">
                    <div class="step-title">${step.name}</div>
                    <div class="step-type">${step.type}</div>
                  </div>
                `,
                typenode: false
            });

            x += gap;
        });

        return nodes;
    }

    private getClassNameForType(type: string): string {
        switch (type) {
            case 'DATA SOURCE': return 'node-source';
            case 'DQ RULE': return 'node-rule';
            case 'TRANSFORMATION': return 'node-transform';
            case 'DECISION': return 'node-decision';
            case 'DATA SINK': return 'node-sink';
            default: return 'node-default';
        }
    }
}
