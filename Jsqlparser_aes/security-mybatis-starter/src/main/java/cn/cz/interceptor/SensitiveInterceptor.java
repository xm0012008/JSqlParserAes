package cn.cz.interceptor;

import cn.cz.aischool.config.SecuritySchemeTableConfig;
import cn.cz.aischool.plugin.SecurityPlugin;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.hutool.core.util.StrUtil.C_LF;
import static cn.hutool.core.util.StrUtil.C_SPACE;

/**
 * 敏感字段拦截器,用于处理sql中的敏感字段
 *
 * @author xuming
 */
@Intercepts(value = {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
@Data
@Slf4j
public class SensitiveInterceptor implements Interceptor {
    private SecurityPlugin securityPlugin;

    /**
     * 拦截器中因sql转换出现的异常不会进行抛出,捕捉后记录日志
     *
     * @param invocation
     * @return
     * @throws Exception
     */
    @Override
    public Object intercept(Invocation invocation) throws Exception {
        String originSql = null;
        String convertedSql = null;
        Connection conn = (Connection) invocation.getArgs()[0];
        String database = getDatabase(conn);
        log.debug("database:{}", database);
        //未进行敏感信息配置的结束
        try {
            if (Objects.nonNull(securityPlugin.getSecuritySchemeTableConfig()) && CollectionUtil.isNotEmpty(
                    securityPlugin.getSecuritySchemeTableConfig().getSchemaConfigList())) {
                SecuritySchemeTableConfig securitySchemeTableConfig = securityPlugin.getSecuritySchemeTableConfig();
                List<SecuritySchemeTableConfig.SchemeConfig> schemaConfigList =
                        securitySchemeTableConfig.getSchemaConfigList();
                Set<String> sehemaSet = schemaConfigList.stream().map(SecuritySchemeTableConfig.SchemeConfig::getScheme)
                        .collect(Collectors.toSet());
                if (sehemaSet.contains(database)) {
                    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
                    BoundSql boundSql = statementHandler.getBoundSql();
                    originSql = StrUtil.replace(boundSql.getSql(), StringUtils.LF, StringUtils.SPACE);
                    originSql = StrUtil.replace(originSql, StringUtils.CR, StringUtils.SPACE);
                    Statement statement = CCJSqlParserUtil.parse(originSql);
                    statement.accept(securityPlugin.getSecurityStatementVisitor());
                    Field field = boundSql.getClass().getDeclaredField("sql");
                    field.setAccessible(true);
                    field.set(boundSql, statement.toString());
                    convertedSql = statement.toString();
                    log.debug("敏感sql转换成功,原始sql:{},转换后的sql:{}", originSql, convertedSql);
                }
            }
        } catch (JSQLParserException parserException) {
            log.warn("敏感sql转换失败,放弃转换,原始sql为:{},异常信息为:{}", originSql, parserException.getMessage());
        } catch (Exception e) {
            //sql解析失败,保证业务正常执行,放弃转换
            log.error("敏感sql转换失败,放弃转换:{}", originSql);
            log.error("", e);
        }
        return invocation.proceed();
    }

    private static String getDatabase(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String databaseProductName = metaData.getDatabaseProductName();
        switch (databaseProductName) {
            case "MySQL":
                return (String) SystemMetaObject.forObject(metaData).getValue("database");
            case "DM DBMS":
                return conn.getSchema();
            default:
                String schema = conn.getSchema();
                if (StringUtils.isNotBlank(schema)) {
                    return schema;
                }
                schema = conn.getCatalog();
                if (StringUtils.isNotBlank(schema)) {
                    return schema;
                }
                throw new RuntimeException("数据脱敏初始化连接库名错误!");
        }
    }

}
