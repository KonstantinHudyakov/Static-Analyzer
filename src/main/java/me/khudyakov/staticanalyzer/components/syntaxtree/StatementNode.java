package me.khudyakov.staticanalyzer.components.syntaxtree;

import me.khudyakov.staticanalyzer.program.Expression;

public abstract class StatementNode extends TreeNode {

    protected Expression expression;

    public StatementNode(Expression expression) {
        this.expression = expression;
    }

    public StatementNode(Expression expression, int startInd, int endInd) {
        super(startInd, endInd);
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}