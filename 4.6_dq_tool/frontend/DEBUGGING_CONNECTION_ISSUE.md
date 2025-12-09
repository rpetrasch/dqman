# Debugging Guide: Connection Persistence Issue

## Problem
Connections to new nodes are not visible after reopening the flow.

## Debugging Steps

### 1. Check Console Logs When Saving

When you click "Save" after adding a new step and connecting it, check the browser console for these logs:

#### Expected Log Sequence:

```
=== Saving flow ===
Drawflow export: { ... }
Extracted graph from Drawflow: { 
  flowId: X,
  nodes: [...],
  connections: [...]  // <-- Should show your new connection
}
Domain model to save: {
  name: "...",
  steps: [
    { name: "...", id: 10, successorIds: [...], predecessorIds: [...] },
    { name: "...", id: undefined, successorIds: [...], predecessorIds: [...] }  // <-- New step
  ]
}
Flow saved successfully
```

### 2. Key Questions to Answer:

**A. Does Drawflow export include the connection?**
- Look at "Drawflow export" log
- Check if the connection exists in the export data
- If NO → Problem is in Drawflow itself (connection wasn't created)
- If YES → Continue to B

**B. Does the extracted graph include the connection?**
- Look at "Extracted graph from Drawflow" log
- Check `connections` array
- If NO → Problem is in `FlowGraphMapper.fromDrawflowExport()`
- If YES → Continue to C

**C. Does the domain model include the connection?**
- Look at "Domain model to save" log
- Check `successorIds` and `predecessorIds` arrays
- If NO → Problem is in `FlowGraphMapper.viewToDomain()`
- If YES → Continue to D

**D. Does the backend save the connection?**
- Check backend logs
- Look for the controller's save operation
- If NO → Problem is in backend persistence
- If YES → Continue to E

**E. Does the backend return the connection when loading?**
- Close and reopen the flow
- Look at "=== Loading flow ===" log
- Check if `successorIds` are present in the domain model
- If NO → Problem is in backend retrieval
- If YES → Problem is in rendering

### 3. Most Likely Issue

Based on the mapper code, the most likely issue is in **Step C**:

```typescript
// In FlowGraphMapper.viewToDomain()
if (sourceStep && targetStep && sourceStep.id && targetStep.id) {
    // Add connection
}
```

**The problem**: If the new step doesn't have an ID yet, this condition fails!

### 4. The Root Cause

When you:
1. Add a new step (no ID)
2. Connect it to an existing step
3. Save

The mapper extracts the connection from Drawflow, BUT when converting to domain model:
- The new step has `stepId: null`
- The condition `sourceStep.id && targetStep.id` fails
- The connection is NOT added to `successorIds`/`predecessorIds`
- The backend saves the steps but WITHOUT connections
- When you reopen, there are no connections to render

### 5. The Solution

We need a **two-phase save**:

**Phase 1**: Save steps to get IDs for new steps
```
1. Extract steps from Drawflow (ignore connections for now)
2. Save to backend
3. Backend assigns IDs to new steps
4. Backend returns updated flow with all IDs
```

**Phase 2**: Save connections
```
5. Update the Drawflow graph with new step IDs
6. Extract connections from Drawflow
7. Convert to domain model (now all steps have IDs)
8. Save to backend again
9. Backend saves connections
```

### 6. Testing the Theory

Add this temporary logging to `saveFlow()`:

```typescript
saveFlow(): void {
    // ... existing code ...
    
    // Convert view model back to domain model
    const updatedFlow = FlowGraphMapper.viewToDomain(graph, this.flow);
    
    // ADD THIS LOGGING:
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
    
    graph.connections.forEach((conn, index) => {
        const sourceNode = graph.nodes.find(n => n.nodeId === conn.sourceNodeId);
        const targetNode = graph.nodes.find(n => n.nodeId === conn.targetNodeId);
        console.log(`Connection ${index}:`, {
            source: sourceNode?.name,
            sourceHasId: !!sourceNode?.stepId,
            target: targetNode?.name,
            targetHasId: !!targetNode?.stepId,
            willBeSaved: !!(sourceNode?.stepId && targetNode?.stepId)
        });
    });
    console.log('=== END ANALYSIS ===');
    
    // ... rest of code ...
}
```

### 7. What to Look For

When you save with a new step connected:

```
=== DETAILED STEP ANALYSIS ===
Step 0: { name: "Existing Step", id: 10, hasId: true, successorIds: [???], ... }
Step 1: { name: "New Step", id: undefined, hasId: false, successorIds: [], ... }

Connection 0: {
  source: "Existing Step",
  sourceHasId: true,
  target: "New Step",
  targetHasId: false,  // <-- THIS IS THE PROBLEM!
  willBeSaved: false   // <-- Connection will NOT be saved!
}
```

## Next Steps

1. Add the detailed logging above
2. Test: Add new step, connect it, save
3. Share the console output
4. Based on the output, we'll implement the two-phase save solution
