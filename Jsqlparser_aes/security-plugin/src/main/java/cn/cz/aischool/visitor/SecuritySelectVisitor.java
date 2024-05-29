package cn.cz.aischool.visitor;

import cn.cz.aischool.config.SecuritySchemeTableConfig;
import cn.cz.aischool.plugin.SecurityPlugin;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.values.ValuesStatement;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * select访问器
 *
 * @author xuming
 */
public class SecuritySelectVisitor extends SelectVisitorAdapter {

    private final SecurityPlugin securityPlugin;

    public SecuritySelectVisitor(SecurityPlugin securityPlugin) {
        this.securityPlugin = securityPlugin;
    }

    /**
     * 处理表名或子查询
     */
    @Override
    public void visit(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        if (Objects.isNull(fromItem) && CollectionUtil.isEmpty(selectItems)) {
            return;
        }
        if (fromItem instanceof SubSelect) {
            //from后跟子查询,不替换select *
            SubSelect subSelect = (SubSelect)fromItem;
            SelectBody selectBody = subSelect.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                selectBody.accept(this);
            }
            subSelect.accept(securityPlugin.getSecurityFromItemVisitor());
        } else if (fromItem instanceof Table) {
            //from后跟表,替换 select *
            Table table = (Table)fromItem;
            List<SelectItem> newSelectItemList = replaceAllColumns(selectItems, table);
            if (CollectionUtil.isNotEmpty(newSelectItemList)) {
                plainSelect.setSelectItems(newSelectItemList);
            }
            for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : securityPlugin.getSecuritySchemeTableConfig()
                .getSchemaConfigList()) {
                for (Map.Entry<String, List<String>> entry : Optional.ofNullable(
                    schemeConfig.getSensitiveTableSensitiveColumnMap()).orElse(Collections.emptyMap()).entrySet()) {
                    String tableName = entry.getKey();
                    String schemaName = table.getSchemaName();
                    if (StrUtil.isNotBlank(schemaName)) {
                        if (!securityPlugin.isSameSchema(schemaName, schemeConfig.getScheme())) {
                            continue;
                        }
                    }
                    if (!securityPlugin.isSensitiveTable(table.getName())) {
                        continue;
                    }
                    // 处理select字段
                    if (CollectionUtil.isNotEmpty(plainSelect.getSelectItems())) {
                        Alias alias = table.getAlias();
                        plainSelect.getSelectItems().forEach(selectItem -> {
                            String image = selectItem.getASTNode().jjtGetFirstToken().image;
                            if (Objects.nonNull(alias)) {
                                if (StrUtil.equals(alias.getName(), image)) {
                                    selectItem.accept(securityPlugin.getSecuritySelectItemVisitor());
                                }
                            } else {
                                selectItem.accept(securityPlugin.getSecuritySelectItemVisitor());
                            }
                        });
                    }
                }
            }
        }
        //where子句
        convertExpression(plainSelect.getWhere());
        //having子句
        convertExpression(plainSelect.getHaving());
        //joins
        convertJoins(plainSelect);
    }

    /**
     * 替换 select *
     * @param selectItems
     * @param table
     * @return
     */
    private List<SelectItem> replaceAllColumns(List<SelectItem> selectItems, Table table) {
        List<SelectItem> newSelectItemList = Lists.newArrayList();
        for (int i = 0; i < CollectionUtils.size(selectItems); i++) {
            SelectItem selectItem = selectItems.get(i);
            if (selectItem instanceof AllColumns) {
                List<SelectItem> convertedSelectItemList =
                    securityPlugin.convertAllColumnsToSelectItem(table, (AllColumns)selectItem);
                newSelectItemList.addAll(convertedSelectItemList);
                continue;
            }
            if (selectItem instanceof AllTableColumns) {
                AllTableColumns allTableColumns = (AllTableColumns)selectItem;
                if (StrUtil.equals(allTableColumns.getTable().getName(), table.getName()) || StrUtil.equals(
                    allTableColumns.getTable().getName(), table.getAlias().getName())) {

                    List<SelectItem> convertedSelectItemList =
                        securityPlugin.convertAllTableColumnsToSelectItem(table, (AllTableColumns)selectItem);
                    newSelectItemList.addAll(convertedSelectItemList);
                } else {
                    newSelectItemList.add(selectItem);
                }
                continue;
            }
            newSelectItemList.add(selectItem);
        }
        return newSelectItemList;
    }

    @Override
    public void visit(SetOperationList setOpList) {
        setOpList.getSelects().forEach(e -> e.accept(this));
    }

    private void convertExpression(Expression expression) {
        if (Objects.nonNull(expression)) {
            expression.accept(securityPlugin.getSecurityExpressionVisitor());

        }
    }

    /**
     * 若plainSelect中存在joins,且为敏感表,将每个join都按规则进行转换,并转换selectItems
     * @param plainSelect
     */
    private void convertJoins(PlainSelect plainSelect) {
        List<Join> joins = plainSelect.getJoins();
        if (CollectionUtils.isNotEmpty(joins)) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table) {
                    Table rightTable = (Table)rightItem;
                    if (securityPlugin.isSensitiveTable(rightTable.getName())) {
                        Alias sensitiveTableAlias = rightTable.getAlias();
                        join.getOnExpressions().forEach(
                            onExpression -> onExpression.accept(securityPlugin.getSecurityExpressionVisitor()));
                        List<SelectItem> selectItems = replaceAllColumns(plainSelect.getSelectItems(), rightTable);
                        selectItems.forEach(selectItem -> {
                            String image = selectItem.getASTNode().jjtGetFirstToken().image;
                            if (StrUtil.equals(sensitiveTableAlias.getName(), image)) {
                                selectItem.accept(securityPlugin.getSecuritySelectItemVisitor());
                            }
                        });
                        plainSelect.setSelectItems(selectItems);
                    }
                }
            }
        }
    }

}
