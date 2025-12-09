# Flow Graph Architecture Refactoring Plan

## Overview
Refactor the flow graph dialog to use clean architecture with proper separation between domain models and view models.

## Architecture Layers

### 1. Domain Model (Backend-oriented)
- **Location**: `models/dq-flow.model.ts`
- **Purpose**: Represents the business domain as understood by the backend
- **Models**: `DqFlow`, `DqFlowStep`
- **Characteristics**:
  - Contains `successorIds` and `predecessorIds` for relationships
  - Includes backend-specific fields (id, createdDate, modifiedDate, etc.)
  - Serializable to/from JSON for API communication

### 2. View Model (UI-oriented)
- **Location**: `utils/flow-graph-mapper.ts` (inline definitions)
- **Purpose**: Represents the graph as needed by Drawflow library
- **Models**: `DrawflowGraph`, `DrawflowNode`, `DrawflowConnection`
- **Characteristics**:
  - Contains Drawflow-specific properties (nodeId, posX, posY, html, etc.)
  - Separates nodes and connections explicitly
  - Independent of backend structure

### 3. Mapping Layer
- **Location**: `utils/flow-graph-mapper.ts`
- **Purpose**: Bidirectional conversion between domain and view models
- **Class**: `FlowGraphMapper`
- **Methods**:
  - `domainToView(flow: DqFlow): DrawflowGraph` - Convert backend data to graph
  - `viewToDomain(graph: DrawflowGraph, originalFlow: DqFlow): DqFlow` - Convert graph to backend data
  - `fromDrawflowExport(exportData: DrawflowExportData, flowId: number): DrawflowGraph` - Extract graph from Drawflow

## Implementation Steps

### Phase 1: Setup and Testing (Backend Only)
**Goal**: Ensure backend correctly handles domain model persistence

1. ✅ **Create Mapper** - Already done in `flow-graph-mapper.ts`

2. **Test Backend Persistence**
   - Create a simple test flow with 2-3 steps
   - Add connections (successorIds)
   - Save via API
   - Retrieve and verify connections are persisted
   - **Expected**: Backend correctly saves and retrieves `successorIds`

### Phase 2: Refactor Component (Frontend)
**Goal**: Update flow-graph-dialog.component.ts to use the mapper

#### 2.1 Update `ngAfterViewInit()` and `loadFlow()`
**Current**: Directly uses `dqFlowService.getFlowGraphNodes()`
**New**:
```typescript
ngAfterViewInit(): void {
    if (this.drawflowElement) {
        this.editor = new Drawflow(this.drawflowElement.nativeElement);
        this.editor.reroute = true;
        this.editor.curvature = 0.5;
        this.editor.start();
        
        // Event handlers...
        
        this.loadFlow();
    }
}

loadFlow(): void {
    if (!this.editor) return;
    
    console.log('=== Loading flow ===');
    console.log('Domain model:', this.flow);
    
    // Convert domain model to view model
    const graph = FlowGraphMapper.domainToView(this.flow);
    console.log('View model:', graph);
    
    // Clear existing graph
    this.editor.clear();
    this.selectedNodeId = null;
    
    // Add nodes to Drawflow
    const nodeIdMap = new Map<number, number>(); // viewNodeId -> drawflowNodeId
    
    graph.nodes.forEach((node, index) => {
        const drawflowNodeId = this.editor.addNode(
            node.name,
            node.inputs,
            node.outputs,
            node.posX,
            node.posY,
            node.className,
            { stepId: node.stepId, stepIndex: index },
            node.html,
            false
        );
        nodeIdMap.set(index, drawflowNodeId);
    });
    
    // Add connections
    setTimeout(() => {
        graph.connections.forEach(conn => {
            const sourceDrawflowId = nodeIdMap.get(conn.sourceNodeId);
            const targetDrawflowId = nodeIdMap.get(conn.targetNodeId);
            
            if (sourceDrawflowId !== undefined && targetDrawflowId !== undefined) {
                this.editor.addConnection(
                    sourceDrawflowId,
                    targetDrawflowId,
                    conn.outputPort,
                    conn.inputPort
                );
            }
        });
    }, 100);
}
```

#### 2.2 Update `saveFlow()`
**Current**: Complex multi-step save with connection restoration
**New**:
```typescript
saveFlow(): void {
    if (!this.flow.id) {
        console.error('Cannot save flow without ID');
        return;
    }
    
    const flowId = this.flow.id;
    
    console.log('=== Saving flow ===');
    
    // Export current Drawflow state
    const exportData = this.editor.export();
    
    // Convert Drawflow export to view model
    const graph = FlowGraphMapper.fromDrawflowExport(exportData, flowId);
    console.log('Extracted graph:', graph);
    
    // Convert view model back to domain model
    const updatedFlow = FlowGraphMapper.viewToDomain(graph, this.flow);
    console.log('Domain model to save:', updatedFlow);
    
    // Save to backend
    this.dqFlowService.updateFlow(flowId, updatedFlow).subscribe({
        next: (savedFlow) => {
            this.ngZone.run(() => {
                this.flow = savedFlow;
                console.log('Flow saved successfully');
                
                // Reload to show saved state
                this.loadFlow();
                this.cdr.detectChanges();
            });
        },
        error: (error) => {
            this.ngZone.run(() => {
                console.error('Error saving flow:', error);
                this.cdr.detectChanges();
            });
        }
    });
}
```

#### 2.3 Update `onAddStep()`
**Current**: Directly adds to `this.flow.steps`
**New**:
```typescript
onAddStep(): void {
    const dialogRef = this.dialog.open(FlowStepDialogComponent, {
        width: '600px',
        data: null
    });

    dialogRef.afterClosed().subscribe((result: DqFlowStep) => {
        if (result) {
            // Add step to domain model
            if (!this.flow.steps) {
                this.flow.steps = [];
            }
            this.flow.steps.push(result);
            
            // Reload graph (will convert domain to view automatically)
            this.loadFlow();
        }
    });
}
```

#### 2.4 Update `onEditStep()`
**Current**: Uses nodeData.data.stepIndex
**New**:
```typescript
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
                // Update domain model
                this.flow.steps[stepIndex] = result;
                
                // Reload graph
                this.loadFlow();
            }
        });
    }
}
```

#### 2.5 Remove Obsolete Methods
Delete these methods as they're no longer needed:
- `restoreConnectionsFromExport()`
- `updateFlowStepsFromGraph()`
- `saveNodePositions()` (now handled by mapper)

### Phase 3: Testing
**Goal**: Verify the refactored implementation works correctly

1. **Test: Open Existing Flow**
   - Open a flow with existing steps and connections
   - Verify all nodes and connections render correctly
   - Check console logs for proper conversion

2. **Test: Add New Step**
   - Add a new step
   - Connect it to existing steps
   - Save
   - Close and reopen - verify connections persist

3. **Test: Edit Step**
   - Double-click a step
   - Modify its properties
   - Save
   - Verify changes persist

4. **Test: Delete Step**
   - Delete a step
   - Save
   - Verify step and its connections are removed

5. **Test: Move Nodes**
   - Move nodes around
   - Save
   - Reopen - verify positions are preserved

## Benefits of This Architecture

1. **Separation of Concerns**
   - Domain logic separate from view logic
   - Each layer has single responsibility

2. **Testability**
   - Mapper can be unit tested independently
   - No Angular dependencies in mapper
   - Easy to mock for component tests

3. **Maintainability**
   - Changes to backend model don't affect view
   - Changes to Drawflow don't affect domain
   - Clear data flow: Domain → View → Drawflow → View → Domain

4. **Debuggability**
   - Clear conversion points with logging
   - Easy to see what data is at each layer
   - Simpler troubleshooting

5. **Future-Proof**
   - Easy to swap Drawflow for another library
   - Easy to add new features to either layer
   - Clear extension points

## Migration Strategy

1. **Keep old code temporarily** - Comment out but don't delete
2. **Implement new code alongside** - Add mapper-based methods
3. **Test thoroughly** - Ensure feature parity
4. **Remove old code** - Once confident in new implementation
5. **Add unit tests** - For mapper and component

## Next Steps

1. Test backend persistence (verify it works correctly)
2. Implement Phase 2.1 (loadFlow refactor)
3. Test loading existing flows
4. Implement Phase 2.2 (saveFlow refactor)
5. Test saving flows
6. Implement remaining methods
7. Remove obsolete code
8. Add unit tests
