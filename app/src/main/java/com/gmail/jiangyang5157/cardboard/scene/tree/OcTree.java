package com.gmail.jiangyang5157.cardboard.scene.tree;

/**
 * @author Yang
 * @since 7/10/2016
 */
public class OcTree implements TreePhase {
    private static final String TAG = "[OcTree]";

    protected static final int MAX_DEPTH = 4; // TODO: 7/10/2016 up to 16

    private OcTreeNode node;

    public OcTree(float[] center, float step) {
        node = new OcTreeNode(center, step, 0);
    }

    @Override
    public void clean() {
        node.clean();
    }

    @Override
    public void insertObject(TreeObject obj) {
        node.insertObject(obj);
    }

    @Override
    public String toString() {
        return TAG + "Node Size: " + node.getNodeSize() + ", Leaf Size: " + node.getLeafSize();
    }
}
