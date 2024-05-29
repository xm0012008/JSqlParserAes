package cn.cz.aischool.visitor;

import cn.cz.aischool.parameter.EncryptJdbcParameter;
import cn.cz.aischool.plugin.SecurityPlugin;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * 表达式访问器
 *
 * @author xuming
 */
public class SecurityExpressionVisitor extends ExpressionVisitorAdapter {

    private final SecurityPlugin securityPlugin;

    public SecurityExpressionVisitor(SecurityPlugin securityPlugin) {
        this.securityPlugin = securityPlugin;
    }

    @Override
    public void visit(Column column) {
        if (securityPlugin.isSensitiveColumn(column.getColumnName())) {
            String columnWithNotTableAlias = column.getColumnName();
            String prefix = null;
            if (Objects.nonNull(column.getTable())) {
                if (Objects.nonNull(column.getTable().getAlias())) {
                    prefix = column.getTable().getAlias() + StrUtil.DOT;
                } else {
                    prefix = column.getTable().getName() + StrUtil.DOT;
                }
            }
            column.setTable(null);
            String decryptColumn = securityPlugin.getDecryptFormat(
                Optional.ofNullable(prefix).orElse(StrUtil.EMPTY) + columnWithNotTableAlias,
                securityPlugin.getSecuritySchemeTableConfig().getEncryptKey());
            column.setColumnName(decryptColumn);
        }
    }

    @Override
    public void visit(Function function) {
        if (Objects.isNull(function.getParameters())) {
            return;
        }
        function.getParameters().getExpressions().forEach(expression -> {
            if (expression instanceof Column) {
                visit((Column)expression);
            } else if (expression instanceof Function) {
                visit((Function)expression);
            }
        });
    }

    /**
     * 表达式使用"="时,将加解密函数放在等式右边加速查询
     *
     * @param equalsTo
     */
    @Override
    public void visit(EqualsTo equalsTo) {
        Expression leftExpression = equalsTo.getLeftExpression();
        Expression rightExpression = equalsTo.getRightExpression();
        if (leftExpression instanceof Column && rightExpression instanceof JdbcParameter) {
            Column column = (Column)leftExpression;
            if (securityPlugin.isSensitiveColumn(column.getColumnName())) {
                equalsTo.setRightExpression(new EncryptJdbcParameter(
                        securityPlugin.getEncryptFormat(securityPlugin.getSecuritySchemeTableConfig().getEncryptKey())));
            }
        } else if (rightExpression instanceof Column && leftExpression instanceof JdbcParameter) {
            Column column = (Column)rightExpression;
            if (securityPlugin.isSensitiveColumn(column.getColumnName())) {
                equalsTo.setLeftExpression(new EncryptJdbcParameter(
                        securityPlugin.getEncryptFormat(securityPlugin.getSecuritySchemeTableConfig().getEncryptKey())));
            }
        } else if (leftExpression instanceof Column && rightExpression instanceof Column) {
            Column leftColumn = (Column)leftExpression;
            Column rightColumn = (Column)rightExpression;
            if (Objects.nonNull(leftColumn.getTable()) && securityPlugin.isSensitiveTable(
                leftColumn.getTable().getName())) {
                leftColumn.accept(this);
            }
            if (Objects.nonNull(rightColumn.getTable()) && securityPlugin.isSensitiveTable(
                rightColumn.getTable().getName())) {
                rightColumn.accept(this);
            }
        }
    }

    @Override
    public void visit(InExpression expr) {
        if (Objects.nonNull(expr.getLeftExpression())) {
            if (expr.getLeftExpression() instanceof Column) {
                Column column = (Column)expr.getLeftExpression();
                if (securityPlugin.isSensitiveColumn(column.getColumnName())) {
                    ItemsList rightItemsList = expr.getRightItemsList();
                    if (rightItemsList instanceof ExpressionList) {
                        ExpressionList expressionList = (ExpressionList)rightItemsList;
                        List<Expression> newExpressionList = Lists.newArrayList();
                        for (Expression expression : expressionList.getExpressions()) {
                            if (expression instanceof JdbcParameter) {
                                newExpressionList.add(new EncryptJdbcParameter(securityPlugin.getEncryptFormat(
                                    securityPlugin.getSecuritySchemeTableConfig().getEncryptKey())));
                            }
                        }
                        if (CollectionUtils.isNotEmpty(newExpressionList)) {
                            expressionList.setExpressions(newExpressionList);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(AndExpression expr) {
        Expression leftExpression = expr.getLeftExpression();
        leftExpression.accept(this);
        Expression rightExpression = expr.getRightExpression();
        rightExpression.accept(this);
    }

    @Override
    public void visit(ExpressionList expressionList) {
        LinkedHashMap<Integer, EncryptJdbcParameter> encryptParameterConfigMap =
            securityPlugin.getSecurityItemListVisitor().getEncryptParameterConfigMap();
        for (int index = 0; index < CollectionUtils.size(expressionList.getExpressions()); index++) {
            Expression expr = expressionList.getExpressions().get(index);
            if (expr instanceof JdbcParameter) {
                for (Map.Entry<Integer, EncryptJdbcParameter> entry : Optional.ofNullable(encryptParameterConfigMap)
                    .orElse(Maps.newLinkedHashMap()).entrySet()) {
                    if (Objects.equals(entry.getKey(), index)) {
                        expressionList.getExpressions().set(index, entry.getValue());
                    }
                }
            } else if (expr instanceof RowConstructor) {
                RowConstructor rowConstructor = (RowConstructor)expr;
                Optional.ofNullable(encryptParameterConfigMap).orElse(Maps.newLinkedHashMap()).entrySet().forEach(
                    (entry -> rowConstructor.getExprList().getExpressions().set(entry.getKey(), entry.getValue())));
            } else {
                expr.accept(this);
            }
        }
    }

}
