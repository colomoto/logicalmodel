package org.colomoto.biolqm.tool.simulation.ordering;

import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.NodeInfo;
import org.colomoto.biolqm.tool.simulation.deterministic.BlockSequentialUpdater;
import org.colomoto.biolqm.tool.simulation.deterministic.DeterministicPriorityUpdater;
import org.colomoto.biolqm.tool.simulation.deterministic.DeterministicUpdater;

import java.util.List;

/**
 * Apply a simple ordering on groups of components for deterministic updaters.
 * This ordered list of groups can yield sequential or priority updaters.
 */
public class DeterministicGrouping extends Grouping {

    public DeterministicGrouping(LogicalModel model) {
        super(model);
    }

    public DeterministicGrouping(LogicalModel model, String setup) {
        super(model);
        boolean first = true;
        for (String block: setup.split(";")) {
            if (first) {
                first = false;
                continue;
            }
            Group group = new Group(this);
            add(group);
            for (String component: block.split(",")) {
                String uid = component.trim();
                SplittingType type = SplittingType.MERGED;
                if (uid.endsWith("[+]")) {
                    uid = uid.substring(0, uid.length()-3);
                    type = SplittingType.POSITIVE;
                } else if (uid.endsWith("[-]")) {
                    uid = uid.substring(0, uid.length()-3);
                    type = SplittingType.NEGATIVE;
                }
                NodeInfo ni = model.getComponent(uid);
                if (ni == null) {
                    continue;
                }
                assign(ni, type, group);
            }
        }

    }

    private int[][] getBlocks() {
        // Make sure the grouping is ready to use
        refresh();

        // create groups
        List<NodeInfo> components = model.getComponents();
        int[][] blocks = new int[size()][];
        int idx = 0;
        for (Group group : this) {
            // Start by counting the number of really needed entries
            int len = 0;
            for (GroupMember member : group) {
                if (member.type == SplittingType.NEGATIVE && ! member.membership.isSeparated()) {
                    continue;
                }
                len += 2;
            }

            // Go through entries a second time to fill the block
            int[] content = new int[len];
            blocks[idx++] = content;
            int curnode = 0;
            for (GroupMember member : group) {
                int nodeidx = components.indexOf(member.membership.node);
                int cst = 0;
                if (member.membership.isSeparated()) {
                    if (member.type == SplittingType.POSITIVE) {
                        cst = 1;
                    } else {
                        cst = -1;
                    }
                }
                content[curnode++] = nodeidx;
                content[curnode++] = cst;

            }
        }
        return blocks;
    }

    public DeterministicUpdater getBlockSequentialUpdater() {
        return new BlockSequentialUpdater(model, getBlocks());
    }

    public DeterministicUpdater getPriorityUpdater() {
        return new DeterministicPriorityUpdater(model, getBlocks());
    }
}
