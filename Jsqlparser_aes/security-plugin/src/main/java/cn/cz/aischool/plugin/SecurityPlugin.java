package cn.cz.aischool.plugin;

import cn.cz.aischool.config.SecuritySchemeTableConfig;
import cn.cz.aischool.engine.DbProductEnum;
import cn.cz.aischool.engine.ProductAlgorithmSelector;
import cn.cz.aischool.parameter.EncryptJdbcParameter;
import cn.cz.aischool.visitor.*;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 安全插件
 *
 * @author xuming
 */
@Data
public class SecurityPlugin {

    /**
     * scheme下的表名,敏感列,全部列的配置项
     */
    private SecuritySchemeTableConfig securitySchemeTableConfig;

    private DbProductEnum dbProductEnum;

    /**
     * 使用插件必须提供的参数
     *
     * @param securitySchemeTableConfig scheme下的多个配置项列表
     */
    public SecurityPlugin(SecuritySchemeTableConfig securitySchemeTableConfig) {
        this.securitySchemeTableConfig = securitySchemeTableConfig;
    }

    public String getEncryptFormat(String encryptKey) {
        return StrUtil.format(ProductAlgorithmSelector.getEncryptExp(dbProductEnum), encryptKey);
    }

    public String getDecryptFormat(String column, String decryptKey) {
        return StrUtil.format(ProductAlgorithmSelector.getDecryptExp(dbProductEnum), column, decryptKey);
    }

    private SecurityFromItemVisitor securityFromItemVisitor = new SecurityFromItemVisitor(this);

    private SecurityStatementVisitor securityStatementVisitor = new SecurityStatementVisitor(this);

    private SecuritySelectItemVisitor securitySelectItemVisitor = new SecuritySelectItemVisitor(this);

    private SecurityExpressionVisitor securityExpressionVisitor = new SecurityExpressionVisitor(this);

    private SecuritySelectVisitor securitySelectVisitor = new SecuritySelectVisitor(this);

    private SecurityItemListVisitor securityItemListVisitor = new SecurityItemListVisitor(this, null);

    /**
     * 判断columnName是否为sensitiveTableNameColumnMap中的任一value值
     *
     * @param columnName 列名
     * @return
     */
    public Boolean isSensitiveColumn(String columnName) {
        for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : Optional.ofNullable(
                securitySchemeTableConfig.getSchemaConfigList()).orElse(Collections.emptyList())) {
            for (List<String> tableSensitiveColumnList : Optional.ofNullable(
                    schemeConfig.getSensitiveTableSensitiveColumnMap()).orElse(Maps.newHashMap()).values()) {
                for (String sensitiveTableColumnName : Optional.ofNullable(tableSensitiveColumnList)
                        .orElse(Collections.emptyList())) {
                    if (isSame(sensitiveTableColumnName, columnName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断tableName是否为敏感表
     *
     * @param tableName
     * @return
     */
    public Boolean isSensitiveTable(String tableName) {
        for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : Optional.ofNullable(
                securitySchemeTableConfig.getSchemaConfigList()).orElse(Collections.emptyList())) {
            for (String sensitiveTableName : Optional.ofNullable(schemeConfig.getSensitiveTableSensitiveColumnMap())
                    .orElse(Collections.emptyMap()).keySet()) {
                if (isSame(tableName, sensitiveTableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean isSameSchema(String schemaName, String anotherSchemaName) {
        return isSame(schemaName, anotherSchemaName);
    }

    public Boolean isSameTable(String tableName, String anotherTableName) {
        return isSame(tableName, anotherTableName);
    }

    public Boolean isSameColumn(String columnName, String anotherColumnName) {
        return isSame(columnName, anotherColumnName);
    }

    private Boolean isSame(String name, String anotherName) {
        return StrUtil.equalsIgnoreCase(name, anotherName) || StrUtil.equalsIgnoreCase("`" + name + "`", anotherName)
                || StrUtil.equalsIgnoreCase("`" + anotherName + "`", name);
    }

    /**
     * 在columnNameList查找targetColumnNameList中的每一个元素,然后组装indexEncryptParameterMap
     * 用于计算sql参数中 ? 所处的位置
     *
     * @param columnNameList
     * @param format
     * @return
     */
    public LinkedHashMap<Integer, EncryptJdbcParameter> fetchIndexEncryptParameterMap(List<String> columnNameList,
                                                                                      String format) {
        LinkedHashMap<Integer, EncryptJdbcParameter> indexEncryptParameterMap = Maps.newLinkedHashMap();
        List<String> targetColumnNameList = Lists.newArrayList();
        for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : securitySchemeTableConfig.getSchemaConfigList()) {
            for (Map.Entry<String, List<String>> entry : schemeConfig.getSensitiveTableSensitiveColumnMap()
                    .entrySet()) {
                CollectionUtil.addAll(targetColumnNameList, entry.getValue());
            }
        }
        if (CollectionUtil.isEmpty(columnNameList) && CollectionUtil.isEmpty(targetColumnNameList)) {
            return indexEncryptParameterMap;
        }
        for (int i = 0; i < columnNameList.size(); i++) {
            String columnNameWithOutAlias = columnNameList.get(i);
            for (String targetColumnName : targetColumnNameList) {
                if (isSameColumn(columnNameWithOutAlias, targetColumnName)) {
                    indexEncryptParameterMap.put(i, new EncryptJdbcParameter(format));
                }
            }
        }
        return indexEncryptParameterMap;
    }

    /**
     * 将table的所有列转换为List<SelectItem> 用于替换 select *
     *
     * @param table      敏感表
     * @param allColumns 敏感表的全部列
     * @return List<SelectItem>
     */
    public List<SelectItem> convertAllColumnsToSelectItem(Table table, AllColumns allColumns) {
        for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : Optional.ofNullable(
                securitySchemeTableConfig.getSchemaConfigList()).orElse(Collections.emptyList())) {
            if (MapUtil.isNotEmpty(schemeConfig.getSensitiveTableAllColumnMap())) {
                for (Map.Entry<String, List<String>> entry : schemeConfig.getSensitiveTableAllColumnMap().entrySet()) {
                    String sensitiveTableName = entry.getKey();
                    List<String> tableColumnList = entry.getValue();
                    if (isSensitiveTable(table.getName()) && isSame(sensitiveTableName, table.getName())) {
                        return tableColumnList.stream().map(tableColumnName -> {
                            SelectExpressionItem selectItem = new SelectExpressionItem();
                            Column column = new Column();
                            column.setColumnName(StrUtil.trim(tableColumnName));
                            column.setTable(table);
                            selectItem.setExpression(column);
                            selectItem.setASTNode(allColumns.getASTNode());
                            return selectItem;
                        }).collect(Collectors.toList());
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 将table的所有列转换为List<SelectItem> 用于替换 select *
     *
     * @param table           敏感表
     * @param allTableColumns 敏感表的全部列
     * @return List<SelectItem>
     */
    public List<SelectItem> convertAllTableColumnsToSelectItem(Table table, AllTableColumns allTableColumns) {
        for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : Optional.ofNullable(
                securitySchemeTableConfig.getSchemaConfigList()).orElse(Collections.emptyList())) {
            if (MapUtil.isNotEmpty(schemeConfig.getSensitiveTableAllColumnMap())) {
                for (Map.Entry<String, List<String>> entry : schemeConfig.getSensitiveTableAllColumnMap().entrySet()) {
                    String sensitiveTableName = entry.getKey();
                    List<String> tableColumnList = entry.getValue();
                    if (isSensitiveTable(table.getName()) && isSame(sensitiveTableName, table.getName())) {
                        return tableColumnList.stream().map(tableColumnName -> {
                            SelectExpressionItem selectItem = new SelectExpressionItem();
                            Column column = new Column();
                            column.setColumnName(StrUtil.trim(tableColumnName));
                            column.setTable(table);
                            selectItem.setExpression(column);
                            selectItem.setASTNode(allTableColumns.getASTNode());
                            return selectItem;
                        }).collect(Collectors.toList());
                    }
                }
            }
        }
        return Lists.newArrayList(allTableColumns);
    }

}
