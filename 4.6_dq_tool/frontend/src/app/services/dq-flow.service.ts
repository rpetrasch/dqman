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

    getFlowGraphNodes(flow: DqFlow): DrawflowNodeConfig[] {
        const nodes: DrawflowNodeConfig[] = [];
        let x = 50;
        const y = 100;
        const gap = 300;

        if (!flow.steps || flow.steps.length === 0) {
            return [];
        }

        flow.steps.forEach((step, index) => {
            const isFirst = index === 0;
            const isLast = index === flow.steps.length - 1;

            nodes.push({
                name: step.type, // Identifier for the node
                inputs: isFirst ? 0 : 1,
                outputs: isLast ? 0 : 1,
                posX: x,
                posY: y,
                className: this.getClassNameForType(step.type),
                data: { ...step },
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
