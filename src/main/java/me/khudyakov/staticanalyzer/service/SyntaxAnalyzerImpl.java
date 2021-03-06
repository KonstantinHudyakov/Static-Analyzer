package me.khudyakov.staticanalyzer.service;

import me.khudyakov.staticanalyzer.entity.Token;
import me.khudyakov.staticanalyzer.entity.TokenType;
import me.khudyakov.staticanalyzer.entity.syntaxtree.SyntaxTree;
import me.khudyakov.staticanalyzer.entity.syntaxtree.expression.BinaryOperation;
import me.khudyakov.staticanalyzer.entity.syntaxtree.expression.Constant;
import me.khudyakov.staticanalyzer.entity.syntaxtree.expression.Expression;
import me.khudyakov.staticanalyzer.entity.syntaxtree.expression.Variable;
import me.khudyakov.staticanalyzer.entity.syntaxtree.expression.operator.*;
import me.khudyakov.staticanalyzer.entity.syntaxtree.statement.*;
import me.khudyakov.staticanalyzer.entity.ProgramCode;
import me.khudyakov.staticanalyzer.util.SyntaxAnalyzerException;
import me.khudyakov.staticanalyzer.util.TokenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static me.khudyakov.staticanalyzer.entity.TokenType.*;
import static me.khudyakov.staticanalyzer.util.TokenUtils.isExprStart;
import static me.khudyakov.staticanalyzer.util.TokenUtils.isTokenOfType;

public class SyntaxAnalyzerImpl implements SyntaxAnalyzer {

    @Override
    public SyntaxTree createSyntaxTree(ProgramCode programCode) throws SyntaxAnalyzerException {
        SyntaxTreeBuilder builder = new SyntaxTreeBuilder(programCode);
        return builder.buildTree();
    }

    private static class SyntaxTreeBuilder {

        private static final String ERROR = "Unexpected token \"%s\", ind = %d";
        private static final String PREMATURE_END = "Error! Premature end of program";

        private final Map<String, Variable> varNameToVariable = new HashMap<>();

        private final ProgramCode code;
        private int curInd = 0;

        SyntaxTreeBuilder(ProgramCode code) {
            this.code = code;
        }

        SyntaxTree buildTree() throws SyntaxAnalyzerException {
            List<Statement> statementList = statementList();
            BlockStatement blockStatement = new BlockStatement(statementList);
            return new SyntaxTree(blockStatement);
        }

        /**
         * Program -> StatementList
         * StatementList -> empty | StatementList Statement
         */
        private List<Statement> statementList() throws SyntaxAnalyzerException {
            List<Statement> statements = new ArrayList<>();
            while (curInd < code.size()) {
                statements.add(statement());
            }
            return statements;
        }

        /**
         * Statement -> ExpressionStatement | IfStatement | AssignStatement | BlockStatement
         */
        private Statement statement() throws SyntaxAnalyzerException {
            Token token = getCurOrThrow();
            Statement statement = null;
            if (isTokenOfType(token, IF)) {
                statement = ifStatement();
            } else if (isTokenOfType(token, ASSIGN_IDENTIFIER)) {
                statement = assignStatement();
            } else if (isTokenOfType(token, OPEN_BRACE)) {
                statement = blockStatement();
            } else if (isExprStart(token)) {
                statement = expressionStatement();
            } else {
                throwError(token);
            }
            return statement;
        }

        /**
         * BlockStatement -> { StatementList }
         */
        private BlockStatement blockStatement() throws SyntaxAnalyzerException {
            checkTypeOfCurOrThrow(OPEN_BRACE);
            List<Statement> statementList = new ArrayList<>();
            while (checkTypeOfCur(TokenUtils::isStatementStart)) {
                statementList.add(statement());
            }
            checkTypeOfCurOrThrow(CLOSE_BRACE);

            return new BlockStatement(statementList);
        }

        /**
         * IfStatement -> if ( Expression ) Statement
         */
        private IfStatement ifStatement() throws SyntaxAnalyzerException {
            checkTypeOfCurOrThrow(IF);
            checkTypeOfCurOrThrow(OPEN_PARENTHESIS);
            Expression condition = checkTypeOfCur(CLOSE_PARENTHESIS) ? Expression.EMPTY_EXPRESSION : expression();
            checkTypeOfCurOrThrow(CLOSE_PARENTHESIS);
            Statement body = statement();

            return new IfStatement(condition, body);
        }

        /**
         * AssignStatement -> @ Identifier = Expression ;
         */
        private AssignStatement assignStatement() throws SyntaxAnalyzerException {
            checkTypeOfCurOrThrow(ASSIGN_IDENTIFIER);
            Variable variable = variable();
            checkTypeOfCurOrThrow(ASSIGN);
            Expression expr = expression();
            checkTypeOfCurOrThrow(SEMICOLON);

            return new AssignStatement(variable, expr);
        }

        /**
         * ExpressionStatement -> Expression ;
         */
        private ExpressionStatement expressionStatement() throws SyntaxAnalyzerException {
            Expression expr = expression();
            checkTypeOfCurOrThrow(SEMICOLON);

            return new ExpressionStatement(expr);
        }

        /**
         * Expression -> PlusMinusExpr | PlusMinusExpr > PlusMinusExpr | PlusMinusExpr < PlusMinusExpr
         */
        private Expression expression() throws SyntaxAnalyzerException {
            Expression leftExpr = plusMinusExpression();
            if (checkTypeOfCur(TokenUtils::isCompareOperation)) {
                Token cur = getCurOrThrow();
                BinaryOperator operator = null;
                if(isTokenOfType(cur, GREATER)) {
                    operator = new GreaterOperator();
                } else if(isTokenOfType(cur, LESS)) {
                    operator = new LessOperator();
                } else {
                    throwError(cur);
                }
                curInd++;
                Expression rightExpr = plusMinusExpression();
                return new BinaryOperation(operator, leftExpr, rightExpr);
            }
            return leftExpr;
        }

        /**
         * PlusMinusExpr -> MultDivExpr | PlusMinusExpr + MultDivExpr | PlusMinusExpr - MultDivExpr
         */
        private Expression plusMinusExpression() throws SyntaxAnalyzerException {
            Expression expr = multiplyDivisionExpression();
            while (checkTypeOfCur(TokenUtils::isPlusMinus)) {
                Token cur = getCurOrThrow();
                BinaryOperator operator = null;
                if(isTokenOfType(cur, ADDITION)) {
                    operator = new AdditionOperator();
                } else if(isTokenOfType(cur, SUBTRACTION)) {
                    operator = new SubtractionOperator();
                } else {
                    throwError(cur);
                }
                curInd++;
                Expression rightExpr = multiplyDivisionExpression();
                expr = new BinaryOperation(operator, expr, rightExpr);
            }
            return expr;
        }

        /**
         * MultDivExpr -> SimpleExpr | MultDivExpr * SimpleExpr | MultDivExpr / SimpleExpr
         */
        private Expression multiplyDivisionExpression() throws SyntaxAnalyzerException {
            Expression expr = simpleExpression();
            while (checkTypeOfCur(TokenUtils::isMultiplyDivision)) {
                Token cur = getCurOrThrow();
                BinaryOperator operator = null;
                if(isTokenOfType(cur, MULTIPLICATION)) {
                    operator = new MultiplicationOperator();
                } else if(isTokenOfType(cur, DIVISION)) {
                    operator = new DivisionOperator();
                } else {
                    throwError(cur);
                }
                curInd++;
                Expression rightExpr = simpleExpression();
                expr = new BinaryOperation(operator, expr, rightExpr);
            }
            return expr;
        }

        /**
         * SimpleExpression -> Identifier | Integer | ( Expression )
         */
        private Expression simpleExpression() throws SyntaxAnalyzerException {
            Token cur = getCurOrThrow();
            Expression expr = null;
            if (isTokenOfType(cur, OPEN_PARENTHESIS)) {
                curInd++;
                expr = expression();
                checkTypeOfCurOrThrow(CLOSE_PARENTHESIS);
            } else if (isTokenOfType(cur, INTEGER)) {
                int value = Integer.parseInt(cur.getValue());
                expr = new Constant(value);
                curInd++;
            } else if (isTokenOfType(cur, IDENTIFIER)) {
                expr = variable();
            } else {
                throwError(cur);
            }

            return expr;
        }

        private Variable variable() throws SyntaxAnalyzerException {
            Token cur = getCurOrThrow();
            if (!isTokenOfType(cur, IDENTIFIER)) {
                throwError(cur);
            }
            String varName = cur.getValue();
            Variable variable;
            if (varNameToVariable.containsKey(varName)) {
                variable = varNameToVariable.get(varName);
            } else {
                variable = new Variable(varName);
                varNameToVariable.put(varName, variable);
            }
            curInd++;
            return variable;
        }

        // this method may be used with another argument in future modifications
        private boolean checkTypeOfCur(TokenType type) throws SyntaxAnalyzerException {
            return checkTypeOfCur(token -> isTokenOfType(token, type));
        }

        private boolean checkTypeOfCur(Function<Token, Boolean> condition) throws SyntaxAnalyzerException {
            Token cur = getCurOrThrow();
            return condition.apply(cur);
        }

        private void checkTypeOfCurOrThrow(TokenType type) throws SyntaxAnalyzerException {
            checkTypeOfCurOrThrow(token -> isTokenOfType(token, type));
        }

        private void checkTypeOfCurOrThrow(Function<Token, Boolean> condition) throws SyntaxAnalyzerException {
            Token cur = getCurOrThrow();
            throwIfNotTypeOf(cur, condition);
            curInd++;
        }

        private void throwIfNotTypeOf(Token token, Function<Token, Boolean> condition) throws SyntaxAnalyzerException {
            if (!condition.apply(token)) {
                throwError(token);
            }
        }

        private void throwIfNotTypeOf(Token token, TokenType type) throws SyntaxAnalyzerException {
            if (!isTokenOfType(token, type)) {
                throwError(token);
            }
        }

        private Token getCurOrThrow() throws SyntaxAnalyzerException {
            if (curInd >= code.size()) {
                throw new SyntaxAnalyzerException(PREMATURE_END);
            }
            return code.get(curInd);
        }

        private void throwError(Token token) throws SyntaxAnalyzerException {
            throw new SyntaxAnalyzerException(String.format(ERROR, token.getValue(), curInd));
        }
    }
}
