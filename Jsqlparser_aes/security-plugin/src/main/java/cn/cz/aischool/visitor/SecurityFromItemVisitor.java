package cn.cz.aischool.visitor;

import cn.cz.aischool.plugin.SecurityPlugin;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * 处理from
 *
 * @author xuming
 */
public class SecurityFromItemVisitor extends FromItemVisitorAdapter {

    private final SecurityPlugin securityPlugin;

    public SecurityFromItemVisitor(SecurityPlugin securityPlugin) {
        this.securityPlugin = securityPlugin;
    }

    @Override
    public void visit(SubSelect subSelect) {
        subSelect.getSelectBody().accept(securityPlugin.getSecuritySelectVisitor());
    }
}
