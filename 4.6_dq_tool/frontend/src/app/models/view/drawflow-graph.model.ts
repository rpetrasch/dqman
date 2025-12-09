/**
 * View models for Drawflow graph representation
 * These models are specific to the UI layer and independent of the backend domain model
 */

/**
 * Represents a node in the Drawflow graph
 */
export interface DrawflowNode {
    /** Drawflow's internal node ID (assigned by Drawflow library) */
    nodeId: number;

    /** Reference to the domain model step ID */
    stepId: number | null;

    /** Step name */
    name: string;

    /** Step type (DATA SOURCE, DQ RULE, etc.) */
    type: string;

    /** Step description */
    description?: string;

    /** X position in the graph */
    posX: number;

    /** Y position in the graph */
    posY: number;

    /** CSS class for styling */
    className: string;

    /** Number of input ports */
    inputs: number;

    /** Number of output ports */
    outputs: number;

    /** HTML content for the node */
    html: string;

    /** Additional metadata */
    metadata?: {
        integrationId?: number;
        ruleId?: number;
        transformationId?: number;
        isInitial?: boolean;
        isFinal?: boolean;
    };
}

/**
 * Represents a connection between two nodes in the Drawflow graph
 */
export interface DrawflowConnection {
    /** Source node ID (Drawflow internal ID) */
    sourceNodeId: number;

    /** Target node ID (Drawflow internal ID) */
    targetNodeId: number;

    /** Output port name (e.g., 'output_1') */
    outputPort: string;

    /** Input port name (e.g., 'input_1') */
    inputPort: string;
}

/**
 * Represents the complete Drawflow graph state
 */
export interface DrawflowGraph {
    /** Reference to the domain flow ID */
    flowId: number;

    /** All nodes in the graph */
    nodes: DrawflowNode[];

    /** All connections in the graph */
    connections: DrawflowConnection[];
}

/**
 * Drawflow export format (as returned by editor.export())
 */
export interface DrawflowExportData {
    drawflow: {
        Home: {
            data: {
                [nodeId: string]: {
                    id: number;
                    name: string;
                    data: any;
                    class: string;
                    html: string;
                    typenode: boolean;
                    inputs: {
                        [key: string]: {
                            connections: Array<{
                                node: string;
                                input: string;
                            }>;
                        };
                    };
                    outputs: {
                        [key: string]: {
                            connections: Array<{
                                node: string;
                                output: string;
                            }>;
                        };
                    };
                    pos_x: number;
                    pos_y: number;
                };
            };
        };
    };
}
