package me.khudyakov.staticanalyzer.entity.syntaxtree.expression.operator;

public class MultiplicationOperator implements BinaryOperator {

    @Override
    public int applyAsInt(Integer integer, Integer integer2) {
        return integer * integer2;
    }

    @Override
    public String toString() {
        return "*";
    }
}
