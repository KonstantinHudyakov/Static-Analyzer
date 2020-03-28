package me.khudyakov.semanticanalyzer.components.semantictree;

import me.khudyakov.semanticanalyzer.util.ExpressionExecutionException;

public abstract class TreeNode {

    private int startInd;
    private int endInd;

    public TreeNode() {
    }

    public TreeNode(int startInd, int endInd) {
        this.startInd = startInd;
        this.endInd = endInd;
    }

    public abstract void executeSubtree() throws ExpressionExecutionException;

    public int getStartInd() {
        return startInd;
    }

    public void setStartInd(int startInd) {
        this.startInd = startInd;
    }

    public int getEndInd() {
        return endInd;
    }

    public void setEndInd(int endInd) {
        this.endInd = endInd;
    }
}