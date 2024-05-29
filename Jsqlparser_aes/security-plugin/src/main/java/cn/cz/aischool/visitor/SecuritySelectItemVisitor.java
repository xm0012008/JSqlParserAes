package cn.cz.aischool.visitor;

import cn.cz.aischool.plugin.SecurityPlugin;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;

import java.util.Objects;

/**
 * selectItem访问器:用于处理select后的字段
 *
 * @author xuming
 */
public class SecuritySelectItemVisitor extends SelectItemVisitorAdapter {

    private final SecurityPlugin securityPlugin;

    public SecuritySelectItemVisitor(SecurityPlugin securityPlugin) {
        this.securityPlugin = securityPlugin;
    }

    @Override
    public void visit(SelectExpressionItem selectExpressionItem) {
        if (selectExpressionItem.getExpression() instanceof Column) {
            Column column = (Column)selectExpressionItem.getExpression();
            if (securityPlugin.isSensitiveColumn(column.getColumnName())) {
                if (Objects.isNull(selectExpressionItem.getAlias())) {
                    selectExpressionItem.setAlias(new Alias(column.getColumnName()));
                }
                selectExpressionItem.accept(securityPlugin.getSecurityExpressionVisitor());
            }
        }
        if (selectExpressionItem.getExpression() instanceof Function) {
            Function function = (Function)selectExpressionItem.getExpression();
            function.accept(securityPlugin.getSecurityExpressionVisitor());
        }
    }

}
