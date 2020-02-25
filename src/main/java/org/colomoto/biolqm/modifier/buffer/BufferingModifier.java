package org.colomoto.biolqm.modifier.buffer;

import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.LogicalModelImpl;
import org.colomoto.biolqm.NodeInfo;
import org.colomoto.biolqm.modifier.BaseModifier;
import org.colomoto.mddlib.IndexMapper;
import org.colomoto.mddlib.MDDManager;
import org.colomoto.mddlib.MDDMapper;
import org.colomoto.mddlib.MDDVariable;
import org.colomoto.mddlib.internal.MDDStoreImpl;

import java.util.*;

/**
 *
 * @author Aurelien Naldi
 */
public class BufferingModifier extends BaseModifier implements IndexMapper {

    private final LogicalModel model;
    private MDDManager ddm, newDDM;
    private List<NodeInfo> core, extra, newCore, newExtra;
    private int[] coreFunctions, extraFunctions, newCoreFunctions, newExtraFunctions;


    private Map<Integer, BufferingRule> rules = new HashMap<>();
    private MDDMapper mapper = null;
    private int curTarget = 0;
    private int nbBuffers = 0;

    public BufferingModifier(LogicalModel model) {
        this.model = model;
    }

    @Override
    public LogicalModel performTask() {
        if (model.isBoolean()) {
            return model;
        }

        // Lock the configuration to get final positions
        int size = core.size();
        int curPosition = size;
        for (int i=0 ; i<size ; i++) {
            BufferingRule rule = this.rules.get(i);
            if (rule != null) {
                rule.lock(curPosition);
                curPosition += rule.count();
            }
        }

        this.ddm = model.getMDDManager();
        this.core = model.getComponents();
        this.extra = model.getExtraComponents();
        this.coreFunctions = model.getLogicalFunctions();
        this.extraFunctions = model.getExtraLogicalFunctions();

        this.newCore = getComponents(core, true);
        this.newExtra = getComponents(extra, false);

        this.newDDM = new MDDStoreImpl(newCore, ddm.getLeafCount());
        this.mapper = new MDDMapper(ddm, newDDM, this);

        this.newCoreFunctions = transferFunctions(coreFunctions, 0);
        this.newExtraFunctions = transferFunctions(extraFunctions, coreFunctions.length);

        return new LogicalModelImpl( newDDM, newCore, newCoreFunctions, newExtra, newExtraFunctions);
    }

    private BufferingRule ensureRule(int source) {
        BufferingRule rule = rules.get(source);
        if (rule == null) {
            rule = new BufferingRule(source);
            rules.put(source, rule);
        }
        return rule;
    }

    private List<NodeInfo> getComponents(List<NodeInfo> components, boolean isCore) {
        int size = components.size();
        int newSize = size;
        if (isCore) {
            newSize += this.nbBuffers;
        }
        List<NodeInfo> result = new ArrayList<>(newSize);

        for (NodeInfo ni: components) {
            result.add(ni.clone());
        }

        if (isCore) {
            int i = 0;
            for (NodeInfo ni: components) {
                int c = countBuffersFrom(i);
                for (int j=0 ; j<countBuffersFrom(i) ; j++) {
                    result.add( new NodeInfo("_bf_"+ni.getNodeID()+"_"+j, ni.getMax()) );
                }
                i++;
            }
        }

        return result;
    }

    private int[] transferFunctions(int[] functions, int shift) {
        int size = functions.length;
        int newSize = size;

        if (shift == 0) {
            newSize += this.nbBuffers;
        }

        int[] newFunctions = new int[newSize];

        this.curTarget = shift;
        for (int i=0 ; i<functions.length ; i++) {
            this.curTarget++;
            newFunctions[i] = mapper.mapMDD(functions[i]);
        }

        if (shift == 0) {
            int idx = size;
            for (int i=0 ; i<functions.length ; i++) {
                int c = countBuffersFrom(i);
                if (c > 0) {

                    // Create the mirror function for the buffer components
                    int node = 0;
                    MDDVariable var = newDDM.getVariableForKey( this.core.get(i));
                    if (var.nbval == 2) {
                        node = var.getNode(0,1);
                    } else {
                        int[] children = new int[var.nbval];
                        for (int k=0 ; k<children.length ; k++) {
                            children[k] = k;
                        }
                        node = var.getNode(children);
                    }

                    // Assign the mirror function to all buffers
                    for (int j=0 ; j<countBuffersFrom(i) ; j++) {
                        newFunctions[idx++] = node;
                    }
                }
            }
        }

        return newFunctions;
    }

    private int get(int source, int target) {
        BufferingRule rules = this.rules.get(source);
        if (rules != null) {
            Integer result = rules.get(target);
            if (result != null) {
                return result.intValue();
            }
        }
        return source;
    }

    private int countBuffersFrom(int source) {
        BufferingRule rules = this.rules.get(source);
        if (rules != null) {
            return rules.count();
        }
        return 0;
    }

    @Override
    public int get(int i) {
        return this.get(i, curTarget);
    }

    public void addSingleBuffer(int source, int target) {
        ensureRule(source).add(target);
    }

    public void addMultipleBuffer(int source, List<Integer> targets) {
        ensureRule(source).add(targets);
    }

}


class BufferingRule {

    private int count = 0;
    private int startPosition = -1;
    private final int sourceIndex;
    private Map<Integer,Integer> data = new HashMap<>();

    BufferingRule(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public void add(List<Integer> targets) {
        if (startPosition >= 0) {
            throw new RuntimeException("Buffering setup is locked");
        }
        for (int t: targets) {
            data.put(t, count);
        }
        count++;
    }

    public void add(int target) {
        if (startPosition >= 0) {
            throw new RuntimeException("Buffering setup is locked");
        }
        data.put(target, count);
        count++;
    }

    public int get(int target) {
        if (startPosition < 0) {
            throw new RuntimeException("Buffering setup must be locked first");
        }
        Integer result = data.get(target);
        if (result != null) {
            return startPosition + result.intValue();
        }
        return sourceIndex;
    }

    public int count() {
        return this.count;
    }

    public void lock(int startPosition) {
        this.startPosition = startPosition;
    }
}
