import matplotlib.pyplot as plt
import matplotlib.patches as patches

fig, ax = plt.subplots(figsize=(10, 6))

# Define rectangles: [Transaction_Start, Valid_Start, Width, Height]
# 1. Original Record logged at T1 (Jan), valid Jan to June
rect1 = patches.Rectangle((1, 1), 4, 5, linewidth=1.5, edgecolor='blue', facecolor='skyblue', alpha=0.5, label='Original State (Right V)')

# 2. Non-monotonic Amendment logged at T5 (May), retroactively alters Valid Time from T3 (March) onward
rect2 = patches.Rectangle((5, 3), 2, 3, linewidth=1.5, edgecolor='green', facecolor='lightgreen', alpha=0.7, label='Amended State (Right V\')')

ax.add_patch(rect1)
ax.add_patch(rect2)

# Grid setup & timeline labels
time_labels = ['T1 (Jan)', 'T2 (Feb)', 'T3 (Mar)', 'T4 (Apr)', 'T5 (May)', 'T6 (Jun)']
ax.set_xticks(range(1, 7))
ax.set_xticklabels(time_labels)
ax.set_yticks(range(1, 7))
ax.set_yticklabels(time_labels)

# Correctly aligned axes pointing forward in time
ax.set_xlabel('Transaction Time (System Record Date) →', fontsize=11, fontweight='bold')
ax.set_ylabel('Valid Time (Real-World Fact Date) →', fontsize=11, fontweight='bold')
ax.set_title('Bitemporal Matrix: Non-Monotonic Retroactive Amendment', fontsize=13, fontweight='bold')

# Annotations
ax.annotate('Retroactive Override\n(Logged at T5, applies from T3)', xy=(5.5, 4.5), xytext=(3.2, 5.2),
            arrowprops=dict(facecolor='black', shrink=0.05, width=1, headwidth=6),
            fontsize=10, bbox=dict(boxstyle="round,pad=0.3", fc="yellow", ec="black", lw=1))

ax.set_xlim(0.5, 6.5)
ax.set_ylim(0.5, 6.5)
ax.grid(True, linestyle='--', alpha=0.5)
ax.legend(loc='upper left')

plt.tight_layout()
plt.show()