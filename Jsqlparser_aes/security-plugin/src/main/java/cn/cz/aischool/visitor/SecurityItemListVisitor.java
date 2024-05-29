package cn.cz.aischool.visitor;

import cn.cz.aischool.parameter.EncryptJdbcParameter;
import cn.cz.aischool.plugin.SecurityPlugin;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * 参数访问器，处理占位符嵌套加密函数
 *
 * @author xuming
 */
public class SecurityItemListVisitor extends ItemsListVisitorAdapter {

    private final SecurityPlugin securityPlugin;

    @Getter
    @Setter
    private LinkedHashMap<Integer, EncryptJdbcParameter> encryptParameterConfigMap;

    public SecurityItemListVisitor(SecurityPlugin securityPlugin,
        LinkedHashMap<Integer, EncryptJdbcParameter> encryptParameterConfigMap) {
        this.encryptParameterConfigMap = encryptParameterConfigMap;
        this.securityPlugin = securityPlugin;
    }

    /**
     * 嵌套加密函数
     *
     * @param expressionList
     */
    @Override
    public void visit(ExpressionList expressionList) {
        expressionList.getExpressions().forEach(expression -> {
            expression.accept(securityPlugin.getSecurityExpressionVisitor());
        });
    }

}
