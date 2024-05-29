package cn.cz.config;

import cn.cz.aischool.config.SecuritySchemeTableConfig;
import cn.cz.aischool.engine.DbProductEnum;
import cn.cz.aischool.plugin.SecurityPlugin;
import cn.cz.interceptor.SensitiveInterceptor;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author xuming
 */
@Slf4j
@Configuration
public class SecurityConfiguration implements ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext applicationContext;

    private SecurityPlugin securityPlugin;

    @Bean
    @ConfigurationProperties(prefix = "security")
    public SecuritySchemeTableConfig securityConfig() {
        log.info("准备开始初始化等保相关加解密组件");
        return new SecuritySchemeTableConfig();
    }

    @Bean
    public SensitiveInterceptor sensitiveInterceptor(SecuritySchemeTableConfig securitySchemeTableConfig) {
        securityPlugin = new SecurityPlugin(securitySchemeTableConfig);
        //未进行配置中止处理
        if (CollectionUtil.isEmpty(securitySchemeTableConfig.getSchemaConfigList())) {
            log.warn("应用未进行敏感信息表配置,不会进行sql转换");
        }
        SensitiveInterceptor sensitiveInterceptor = new SensitiveInterceptor();
        sensitiveInterceptor.setSecurityPlugin(securityPlugin);
        return sensitiveInterceptor;
    }

    @Override
    public void afterSingletonsInstantiated() {
        //sqlSessionFactory获取敏感表元数据
        SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {

            List<String> configList = Lists.newArrayList();
            DatabaseMetaData databaseMetaData = sqlSession.getConnection().getMetaData();
            securityPlugin.setDbProductEnum(DbProductEnum.nameOf(databaseMetaData.getDatabaseProductName()));
            for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : Optional.ofNullable(
                    securityPlugin.getSecuritySchemeTableConfig().getSchemaConfigList()).orElse(Collections.emptyList())) {
                Map<String, List<String>> sensitiveTableNameAllColumnMap = Maps.newHashMap();
                String scheme = schemeConfig.getScheme();

                Optional.ofNullable(schemeConfig.getSensitiveTableSensitiveColumnMap()).orElse(Collections.emptyMap())
                        .keySet().forEach(sensitiveTableName -> {
                            try {
                                List<String> tableColumnList = getColumn(databaseMetaData, scheme, sensitiveTableName);
                                log.debug("敏感表:{},的列信息为:[{}]", sensitiveTableName + StrUtil.DOT + sensitiveTableName,
                                        JSONUtil.toJsonStr(tableColumnList));
                                sensitiveTableNameAllColumnMap.put(sensitiveTableName, tableColumnList);
                                schemeConfig.setSensitiveTableAllColumnMap(sensitiveTableNameAllColumnMap);
                                configList.add(schemeConfig.getScheme() + StrUtil.DOT + sensitiveTableName);
                            } catch (Exception e) {
                                log.error("敏感表:{}的列信息解析失败,跳过", sensitiveTableName + StrUtil.DOT + sensitiveTableName);
                                log.error("", e);
                            }
                        });
            }
            if (CollectionUtil.isNotEmpty(configList)) {
                log.info("敏感字段转换插件初始化完毕,成功解析的敏感表为:{}", StringUtils.join(configList));
            } else {
                log.info("敏感字段转换插件初始化完毕,未解析到敏感表配置");
            }
        } catch (Exception e) {
            log.error("敏感字段转换插件初始化失败,异常详情:", e);
        }
    }

    private List<String> getColumn(DatabaseMetaData databaseMetaData, String schemeName, String tableName) throws SQLException {
        List<String> columnResult = getColumn(databaseMetaData, schemeName, tableName,true);
        if(CollectionUtil.isNotEmpty(columnResult)){
            return columnResult;
        }
        columnResult = getColumn(databaseMetaData, schemeName, tableName,false);
        return columnResult;
    }

    private List<String> getColumn(DatabaseMetaData databaseMetaData, String schemeName, String tableName,boolean upperFlag) throws SQLException {
        List<String> columnResult = new ArrayList<>();
        String schemaPattern = StringUtils.isBlank(schemeName) ? null : schemeName.toUpperCase();
        String finallyTableName = tableName.toUpperCase();
        if(!upperFlag){
            schemaPattern = schemaPattern == null ? null : schemaPattern.toLowerCase();
            finallyTableName = finallyTableName.toLowerCase();
        }
        ResultSet columnResultSet = databaseMetaData.getColumns(schemaPattern, schemaPattern,finallyTableName, "%");
        while (columnResultSet.next()) {
            columnResult.add(columnResultSet.getString("COLUMN_NAME"));
        }
        columnResultSet.close();
        return columnResult;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
