package cn.cz.aischool.visitor;

import cn.cz.aischool.parameter.EncryptJdbcParameter;
import cn.cz.aischool.plugin.SecurityPlugin;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;

import java.util.LinkedHashMap;

/**
 * 处理groupby子句
 * 暂时未使用
 *
 * @author xuming
 */
public class SecurityGroupByVisitor implements GroupByVisitor {

    private LinkedHashMap<Integer, EncryptJdbcParameter> map;

    private SecurityPlugin securityPlugin;

    public SecurityGroupByVisitor(SecurityPlugin securityPlugin) {
        this.securityPlugin = securityPlugin;
    }

    @Override
    public void visit(GroupByElement groupBy) {
        map.forEach((index, encryptJdbcParameter) -> {
            groupBy.getGroupByExpressions().set(index, encryptJdbcParameter);
        });
    }
}
