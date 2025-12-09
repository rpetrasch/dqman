/**
 * Unit tests for FlowGraphMapper
 * These tests verify the bidirectional conversion between domain and view models
 */

import { FlowGraphMapper, DrawflowGraph, DrawflowNode, DrawflowConnection } from './flow-graph-mapper';
import { DqFlow, DqFlowStep } from '../models/dq-flow.model';

describe('FlowGraphMapper', () => {

    describe('domainToView', () => {
        it('should convert a simple flow with no connections', () => {
            const domainFlow: DqFlow = {
                id: 1,
                name: 'Test Flow',
                description: 'Test Description',
                status: 'ACTIVE',
                steps: [
                    {
                        id: 10,
                        name: 'Step 1',
                        type: 'DATA SOURCE',
                        description: 'First step',
                        posX: 100,
                        posY: 100,
                        successorIds: [],
                        predecessorIds: []
                    },
                    {
                        id: 20,
                        name: 'Step 2',
                        type: 'DQ RULE',
                        description: 'Second step',
                        posX: 400,
                        posY: 100,
                        successorIds: [],
                        predecessorIds: []
                    }
                ]
            };

            const graph = FlowGraphMapper.domainToView(domainFlow);

            expect(graph.flowId).toBe(1);
            expect(graph.nodes.length).toBe(2);
            expect(graph.connections.length).toBe(0);

            expect(graph.nodes[0].stepId).toBe(10);
            expect(graph.nodes[0].name).toBe('Step 1');
            expect(graph.nodes[0].type).toBe('DATA SOURCE');

            expect(graph.nodes[1].stepId).toBe(20);
            expect(graph.nodes[1].name).toBe('Step 2');
        });

        it('should convert a flow with connections', () => {
            const domainFlow: DqFlow = {
                id: 1,
                name: 'Test Flow',
                description: 'Test Description',
                status: 'ACTIVE',
                steps: [
                    {
                        id: 10,
                        name: 'Step 1',
                        type: 'DATA SOURCE',
                        description: 'First step',
                        posX: 100,
                        posY: 100,
                        successorIds: [20],
                        predecessorIds: []
                    },
                    {
                        id: 20,
                        name: 'Step 2',
                        type: 'DQ RULE',
                        description: 'Second step',
                        posX: 400,
                        posY: 100,
                        successorIds: [30],
                        predecessorIds: [10]
                    },
                    {
                        id: 30,
                        name: 'Step 3',
                        type: 'DATA SINK',
                        description: 'Third step',
                        posX: 700,
                        posY: 100,
                        successorIds: [],
                        predecessorIds: [20]
                    }
                ]
            };

            const graph = FlowGraphMapper.domainToView(domainFlow);

            console.log('=== TEST: domainToView with connections ===');
            console.log('Input domain flow:', domainFlow);
            console.log('Output graph:', graph);

            expect(graph.flowId).toBe(1);
            expect(graph.nodes.length).toBe(3);
            expect(graph.connections.length).toBe(2);

            // Check connections
            expect(graph.connections[0].sourceNodeId).toBe(0); // Step 1 is at index 0
            expect(graph.connections[0].targetNodeId).toBe(1); // Step 2 is at index 1

            expect(graph.connections[1].sourceNodeId).toBe(1); // Step 2 is at index 1
            expect(graph.connections[1].targetNodeId).toBe(2); // Step 3 is at index 2
        });

        it('should handle flow with new step (no ID)', () => {
            const domainFlow: DqFlow = {
                id: 1,
                name: 'Test Flow',
                description: 'Test Description',
                status: 'ACTIVE',
                steps: [
                    {
                        id: 10,
                        name: 'Existing Step',
                        type: 'DATA SOURCE',
                        description: 'Existing',
                        posX: 100,
                        posY: 100,
                        successorIds: [],
                        predecessorIds: []
                    },
                    {
                        // New step - no ID yet
                        name: 'New Step',
                        type: 'DQ RULE',
                        description: 'New',
                        posX: 400,
                        posY: 100,
                        successorIds: [],
                        predecessorIds: []
                    }
                ]
            };

            const graph = FlowGraphMapper.domainToView(domainFlow);

            console.log('=== TEST: domainToView with new step (no ID) ===');
            console.log('Input domain flow:', domainFlow);
            console.log('Output graph:', graph);

            expect(graph.nodes.length).toBe(2);
            expect(graph.nodes[0].stepId).toBe(10);
            expect(graph.nodes[1].stepId).toBeNull(); // New step has no ID
        });
    });

    describe('viewToDomain', () => {
        it('should convert view model back to domain model', () => {
            const originalFlow: DqFlow = {
                id: 1,
                name: 'Test Flow',
                description: 'Test Description',
                status: 'ACTIVE',
                steps: []
            };

            const graph: DrawflowGraph = {
                flowId: 1,
                nodes: [
                    {
                        nodeId: 1,
                        stepId: 10,
                        name: 'Step 1',
                        type: 'DATA SOURCE',
                        description: 'First step',
                        posX: 100,
                        posY: 100,
                        className: 'node-data-source',
                        inputs: 0,
                        outputs: 1,
                        html: '<div>Step 1</div>'
                    },
                    {
                        nodeId: 2,
                        stepId: 20,
                        name: 'Step 2',
                        type: 'DQ RULE',
                        description: 'Second step',
                        posX: 400,
                        posY: 100,
                        className: 'node-dq-rule',
                        inputs: 1,
                        outputs: 1,
                        html: '<div>Step 2</div>'
                    }
                ],
                connections: [
                    {
                        sourceNodeId: 1,
                        targetNodeId: 2,
                        outputPort: 'output_1',
                        inputPort: 'input_1'
                    }
                ]
            };

            const domainFlow = FlowGraphMapper.viewToDomain(graph, originalFlow);

            console.log('=== TEST: viewToDomain ===');
            console.log('Input graph:', graph);
            console.log('Output domain flow:', domainFlow);

            expect(domainFlow.id).toBe(1);
            expect(domainFlow.name).toBe('Test Flow');
            expect(domainFlow.steps.length).toBe(2);

            // Check steps
            expect(domainFlow.steps[0].id).toBe(10);
            expect(domainFlow.steps[0].name).toBe('Step 1');
            expect(domainFlow.steps[1].id).toBe(20);
            expect(domainFlow.steps[1].name).toBe('Step 2');

            // Check connections
            expect(domainFlow.steps[0].successorIds).toContain(20);
            expect(domainFlow.steps[1].predecessorIds).toContain(10);
        });

        it('should handle new steps without IDs in connections', () => {
            const originalFlow: DqFlow = {
                id: 1,
                name: 'Test Flow',
                description: 'Test Description',
                status: 'ACTIVE',
                steps: []
            };

            const graph: DrawflowGraph = {
                flowId: 1,
                nodes: [
                    {
                        nodeId: 1,
                        stepId: 10, // Existing step
                        name: 'Existing Step',
                        type: 'DATA SOURCE',
                        description: 'Existing',
                        posX: 100,
                        posY: 100,
                        className: 'node-data-source',
                        inputs: 0,
                        outputs: 1,
                        html: '<div>Existing</div>'
                    },
                    {
                        nodeId: 2,
                        stepId: null, // New step - no ID yet
                        name: 'New Step',
                        type: 'DQ RULE',
                        description: 'New',
                        posX: 400,
                        posY: 100,
                        className: 'node-dq-rule',
                        inputs: 1,
                        outputs: 1,
                        html: '<div>New</div>'
                    }
                ],
                connections: [
                    {
                        sourceNodeId: 1,
                        targetNodeId: 2,
                        outputPort: 'output_1',
                        inputPort: 'input_1'
                    }
                ]
            };

            const domainFlow = FlowGraphMapper.viewToDomain(graph, originalFlow);

            console.log('=== TEST: viewToDomain with new step ===');
            console.log('Input graph:', graph);
            console.log('Output domain flow:', domainFlow);
            console.log('Step 0 successorIds:', domainFlow.steps[0].successorIds);
            console.log('Step 1 predecessorIds:', domainFlow.steps[1].predecessorIds);

            expect(domainFlow.steps.length).toBe(2);
            expect(domainFlow.steps[0].id).toBe(10);
            expect(domainFlow.steps[1].id).toBeUndefined(); // New step has no ID

            // THIS IS THE KEY TEST: Connection should NOT be added because new step has no ID
            expect(domainFlow.steps[0].successorIds?.length).toBe(0);
            expect(domainFlow.steps[1].predecessorIds?.length).toBe(0);
        });
    });

    describe('fromDrawflowExport', () => {
        it('should extract graph from Drawflow export data', () => {
            const exportData = {
                drawflow: {
                    Home: {
                        data: {
                            '1': {
                                id: 1,
                                name: 'Step 1',
                                data: { stepId: 10, type: 'DATA SOURCE' },
                                class: 'node-data-source',
                                html: '<div>Step 1</div>',
                                typenode: false,
                                inputs: {},
                                outputs: {
                                    output_1: {
                                        connections: [
                                            { node: '2', output: 'input_1' }
                                        ]
                                    }
                                },
                                pos_x: 100,
                                pos_y: 100
                            },
                            '2': {
                                id: 2,
                                name: 'Step 2',
                                data: { stepId: 20, type: 'DQ RULE' },
                                class: 'node-dq-rule',
                                html: '<div>Step 2</div>',
                                typenode: false,
                                inputs: {
                                    input_1: {
                                        connections: [
                                            { node: '1', input: 'output_1' }
                                        ]
                                    }
                                },
                                outputs: {},
                                pos_x: 400,
                                pos_y: 100
                            }
                        }
                    }
                }
            };

            const graph = FlowGraphMapper.fromDrawflowExport(exportData, 1);

            console.log('=== TEST: fromDrawflowExport ===');
            console.log('Input export data:', exportData);
            console.log('Output graph:', graph);

            expect(graph.flowId).toBe(1);
            expect(graph.nodes.length).toBe(2);
            expect(graph.connections.length).toBe(1);

            expect(graph.nodes[0].nodeId).toBe(1);
            expect(graph.nodes[0].stepId).toBe(10);
            expect(graph.nodes[1].nodeId).toBe(2);
            expect(graph.nodes[1].stepId).toBe(20);

            expect(graph.connections[0].sourceNodeId).toBe(1);
            expect(graph.connections[0].targetNodeId).toBe(2);
        });
    });

    describe('Round-trip conversion', () => {
        it('should preserve data through domain -> view -> domain conversion', () => {
            const originalFlow: DqFlow = {
                id: 1,
                name: 'Test Flow',
                description: 'Test Description',
                status: 'ACTIVE',
                steps: [
                    {
                        id: 10,
                        name: 'Step 1',
                        type: 'DATA SOURCE',
                        description: 'First',
                        posX: 100,
                        posY: 100,
                        successorIds: [20],
                        predecessorIds: []
                    },
                    {
                        id: 20,
                        name: 'Step 2',
                        type: 'DQ RULE',
                        description: 'Second',
                        posX: 400,
                        posY: 100,
                        successorIds: [],
                        predecessorIds: [10]
                    }
                ]
            };

            // Convert to view
            const graph = FlowGraphMapper.domainToView(originalFlow);

            // Convert back to domain
            const resultFlow = FlowGraphMapper.viewToDomain(graph, originalFlow);

            console.log('=== TEST: Round-trip conversion ===');
            console.log('Original flow:', originalFlow);
            console.log('Intermediate graph:', graph);
            console.log('Result flow:', resultFlow);

            // Verify structure is preserved
            expect(resultFlow.steps.length).toBe(2);
            expect(resultFlow.steps[0].id).toBe(10);
            expect(resultFlow.steps[1].id).toBe(20);

            // Verify connections are preserved
            expect(resultFlow.steps[0].successorIds).toContain(20);
            expect(resultFlow.steps[1].predecessorIds).toContain(10);
        });
    });
});
