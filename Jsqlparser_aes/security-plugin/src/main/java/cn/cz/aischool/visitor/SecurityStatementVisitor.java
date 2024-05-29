package cn.cz.aischool.visitor;

import cn.cz.aischool.parameter.EncryptJdbcParameter;
import cn.cz.aischool.plugin.SecurityPlugin;
import com.google.common.collect.Lists;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 语句访问器:用于处理insert,select,update,delete
 *
 * @author xuming
 */
public class SecurityStatementVisitor extends StatementVisitorAdapter {

    private final SecurityPlugin securityPlugin;

    public SecurityStatementVisitor(SecurityPlugin securityPlugin) {
        this.securityPlugin = securityPlugin;
    }

    @Override
    public void visit(Select select) {
        SelectBody selectBody = select.getSelectBody();
        if (Objects.nonNull(selectBody)) {
            selectBody.accept(securityPlugin.getSecuritySelectVisitor());
        }
    }

    @Override
    public void visit(Insert insert) {
        if (!securityPlugin.isSensitiveTable(insert.getTable().getName())) {
            return;
        }
        List<String> columnNameList =
            insert.getColumns().stream().map(column -> column.getName(false)).collect(Collectors.toList());
        LinkedHashMap<Integer, EncryptJdbcParameter> encryptJdbcParameterLinkedHashMap =
            securityPlugin.fetchIndexEncryptParameterMap(columnNameList,
                    securityPlugin.getEncryptFormat(securityPlugin.getSecuritySchemeTableConfig().getEncryptKey()));
        SecurityItemListVisitor securityItemListVisitor = securityPlugin.getSecurityItemListVisitor();
        securityItemListVisitor.setEncryptParameterConfigMap(encryptJdbcParameterLinkedHashMap);
        insert.getItemsList().accept(securityPlugin.getSecurityExpressionVisitor());
    }

    @Override
    public void visit(Update update) {
        if (!securityPlugin.isSensitiveTable(update.getTable().getName())) {
            return;
        }
        List<UpdateSet> updateSets = update.getUpdateSets();
        for (int i = 0; i < CollectionUtils.size(updateSets); i++) {
            UpdateSet updateSet = updateSets.get(i);
            for (Column column : updateSet.getColumns()) {
                if (securityPlugin.isSensitiveColumn(column.getColumnName())) {
                    updateSet.setExpressions(Lists.newArrayList(new EncryptJdbcParameter(
                            securityPlugin.getEncryptFormat(
                            securityPlugin.getSecuritySchemeTableConfig().getEncryptKey()))));
                }
            }
        }
        if (Objects.nonNull(update.getWhere())) {
            update.getWhere().accept(securityPlugin.getSecurityExpressionVisitor());
        }
    }

    @Override
    public void visit(Delete delete) {
        if (securityPlugin.isSensitiveTable(delete.getTable().getName())) {
            delete.getWhere().accept(securityPlugin.getSecurityExpressionVisitor());
        }
    }

}
