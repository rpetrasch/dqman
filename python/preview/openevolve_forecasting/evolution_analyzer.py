
import re
import sys
import os
import json
import glob
import matplotlib.pyplot as plt

class SimpleNode:
    def __init__(self, uuid, dataset_node):
        self.uuid = uuid
        self.dataset_node = dataset_node
        self.children = []
        self.x = 0.0
        self.y = 0.0
        self.width = 1.0 

def build_strict_forest(programs, island_id):
    island_uuids = [uid for uid, n in programs.items() if n.island == island_id]
    draw_set = set(island_uuids)
    
    # Add external parents for context
    # External parents are ONLY roots (inputs to the island).
    for uid in island_uuids:
        p = programs[uid].parent
        if p and p in programs:
            draw_set.add(p)
    
    snodes = {uid: SimpleNode(uid, programs[uid]) for uid in draw_set}
    
    has_parent = set()
    for uid, snode in snodes.items():
        original = snode.dataset_node
        pid = original.parent
        
        if pid and pid in snodes:
            # Check if this link is valid for our view
            # Link only if parent is meant to be in this view
            if uid not in has_parent:
                snodes[pid].children.append(snode)
                has_parent.add(uid)

    for snode in snodes.values():
        snode.children.sort(key=lambda c: c.dataset_node.iteration)

    # Roots: Nodes with NO parent in this specific view selection
    roots = [n for uid, n in snodes.items() if uid not in has_parent]
    
    # Sort roots
    roots.sort(key=lambda c: c.dataset_node.iteration)
    return roots, snodes

def layout_node_leaf_based(node, current_leaf_x, depth):
    node.y = depth
    if not node.children:
        node.x = float(current_leaf_x[0])
        current_leaf_x[0] += 1
        return
    child_xs = []
    for child in node.children:
        layout_node_leaf_based(child, current_leaf_x, depth + 1)
        child_xs.append(child.x)
    node.x = sum(child_xs) / len(child_xs)

def visualize_island(island_id, checkpoint_dir):
    programs = parse_checkpoints(checkpoint_dir)
    roots, all_snodes = build_strict_forest(programs, island_id)
    
    if not all_snodes:
        return

    # Deterministic Layout
    leaf_x_tracker = [0]
    for root in roots:
        layout_node_leaf_based(root, leaf_x_tracker, 0)
        leaf_x_tracker[0] += 1 

    # Determine Bests
    
    # 1. Global Best in this Island View (Gold)
    view_best_id = None
    if all_snodes:
         view_best_node = max(all_snodes.values(), key=lambda sn: sn.dataset_node.score)
         view_best_id = view_best_node.uuid

    # 2. Best per Branch (Subtree of Root's children) (Green)
    # Correct logic: A "Branch" is each main lineage starting from a Root's child.
    branch_bests = set()
    
    for root in roots:
        if root.children:
            for branch_start_node in root.children:
                # Traverse entire subtree from here down
                branch_nodes = []
                stack = [branch_start_node]
                while stack:
                    n = stack.pop()
                    branch_nodes.append(n)
                    stack.extend(n.children)
                
                if branch_nodes:
                    best_n = max(branch_nodes, key=lambda sn: sn.dataset_node.score)
                    branch_bests.add(best_n.uuid)
        else:
            # Solitary root?
            pass

    # Check bounds
    xs = [n.x for n in all_snodes.values()]
    ys = [n.y for n in all_snodes.values()]
    if not xs: return
    
    width_span = max(xs) - min(xs) + 2
    height_span = max(ys) - min(ys) + 2
    
    fig_w = max(12, width_span * 0.7)
    fig_h = max(10, height_span * 1.0)
    
    fig, ax = plt.subplots(figsize=(fig_w, fig_h))
    
    # Edges
    for snode in all_snodes.values():
        for child in snode.children:
            ax.plot([snode.x, child.x], [-snode.y, -child.y], color='#BBBBBB', linewidth=1, alpha=0.7, zorder=1)
            
    # Nodes
    for snode in all_snodes.values():
        real_node = snode.dataset_node
        is_ext = (real_node.island != island_id)
        
        # Rule: Score < 0.1 => Red
        if real_node.score < 0.1:
            color = '#FF4444' # Red
            is_low_score = True
        else:
            is_low_score = False
            # Standard island color
            color = '#FFFFFF' if is_ext else '#1f77b4'
            if island_id == 1: color = '#FFFFFF' if is_ext else '#ff7f0e'
            if island_id == 2: color = '#FFFFFF' if is_ext else '#2ca02c'
            if island_id == 3: color = '#FFFFFF' if is_ext else '#d62728'
            if island_id == 4: color = '#FFFFFF' if is_ext else '#9467bd'
        
        # Best status?
        is_view_best = (snode.uuid == view_best_id)
        is_branch_best = (snode.uuid in branch_bests)
        
        # Priority: View Best (Gold) > Branch Best (Green) > Standard
        if is_view_best:
            ec = '#FFD700' # Gold
            lw = 4
        elif is_branch_best:
            ec = '#32CD32' # Green
            lw = 3
        else:
            ec = '#888888' if is_ext else color
            if is_low_score: ec = '#AA0000'
            lw = 1 if is_ext else 0
            
        ax.scatter(snode.x, -snode.y, s=300, c=color, edgecolors=ec, linewidths=lw, zorder=2)
        
        # Label
        lbl = f"{snode.uuid[:6]}\n{real_node.score:.3f}"
        if is_view_best: 
            lbl += "\n(Global)"
        elif is_branch_best: 
            lbl += "\n(Branch)"
        
        ax.text(snode.x + 0.15, -snode.y, lbl, fontsize=8, ha='left', va='center', color='black', zorder=3)

    ax.axis('off')
    ax.set_title(f"Island {island_id} (Strict Layout)", fontsize=16)
    
    outfile = f"evolution_tree_island_{island_id}.png"
    plt.tight_layout()
    plt.savefig(outfile, dpi=100)
    plt.close()
    print(f"Saved {outfile}")

# --- Parsing ---

class Node:
    def __init__(self, uuid):
        self.uuid = uuid
        self.parent = None 
        self.island = None 
        self.score = 0.0
        self.iteration = 0
        self.generation = 0
        self.is_best = False

def parse_checkpoints(checkpoint_dir):
    programs = {}
    search_pattern = os.path.join(checkpoint_dir, 'checkpoint_*', 'programs', '*.json')
    files = glob.glob(search_pattern)

    for filepath in files:
        try:
            with open(filepath, 'r') as f:
                data = json.load(f)
                uuid = data.get('id')
                if not uuid: continue
                
                if uuid not in programs: programs[uuid] = Node(uuid)
                node = programs[uuid]
                
                node.score = data.get('metrics', {}).get('combined_score', 0.0)
                node.parent = data.get('parent_id')
                
                meta = data.get('metadata', {})
                isl = meta.get('island', data.get('island'))
                if isl is not None:
                     node.island = int(isl)
                    
                node.iteration = data.get('iteration_found', 0)
                node.generation = data.get('generation', 0)
        except: pass
    return programs

if __name__ == "__main__":
    checkpoint_dir = 'openevolve_output/checkpoints'
    if len(sys.argv) > 1 and os.path.isdir(sys.argv[1]):
        checkpoint_dir = sys.argv[1]
        
    print(f"Analyzing {checkpoint_dir}...")
    all_p = parse_checkpoints(checkpoint_dir)
    active = sorted(list(set(p.island for p in all_p.values() if p.island is not None)))
    
    for i in active:
        visualize_island(i, checkpoint_dir)
