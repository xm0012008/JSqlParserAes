package cn.cz.config;

import cn.cz.aischool.config.SecuritySchemeTableConfig;
import cn.cz.aischool.engine.DbProductEnum;
import cn.cz.aischool.plugin.SecurityPlugin;
import cn.cz.inteceptor.SensitiveInterceptor;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.db.MetadataManager;
import org.beetl.sql.core.db.TableDesc;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author xuming
 */
@Slf4j
@Configuration
public class SecurityConfiguration implements InitializingBean {

    /**
     * beetSql的SqlManager
     */
    @Autowired
    private List<SQLManager> sqlManagerList;

    @Autowired
    private SecuritySchemeTableConfig securitySchemeTableConfig;

    @Bean
    @ConfigurationProperties(prefix = "security")
    public SecuritySchemeTableConfig securityConfig() {
        log.info("准备开始初始化等保相关加解密组件");
        return new SecuritySchemeTableConfig();
    }

    public void sensitiveInterceptor(SecuritySchemeTableConfig securityConfig) {
        if (CollectionUtil.isEmpty(securityConfig.getSchemaConfigList())) {
            log.warn("应用未进行敏感信息表配置,不会进行sql转换");
            return;
        }
        for (SQLManager sqlManager : sqlManagerList) {
            MetadataManager metaDataManager = sqlManager.getMetaDataManager();
            //组装sensitiveTableNameAllColumnMap
            for (SecuritySchemeTableConfig.SchemeConfig schemeConfig : securityConfig.getSchemaConfigList()) {
                try {
                    String catalog = sqlManager.getDs().getMaster().getCatalog();
                    if (!StringUtils.equals(catalog, schemeConfig.getScheme())) {
                        continue;
                    }
                } catch (SQLException e) {
                    log.error("获取schema发生异常,跳过当前配置的schema:{}", schemeConfig.getScheme(), e);
                }
                Map<String, List<String>> sensitiveTableNameAllColumnMap = Maps.newHashMap();
                schemeConfig.getSensitiveTableSensitiveColumnMap().keySet().forEach(sensitiveTable -> {
                    //根据配置,获取敏感表元数据
                    TableDesc tableDesc = metaDataManager.getTable(sensitiveTable);
                    Set<String> tableAllColumnSet = tableDesc.getCols();
                    sensitiveTableNameAllColumnMap.put(sensitiveTable, Lists.newArrayList(tableAllColumnSet));
                });
                schemeConfig.setSensitiveTableAllColumnMap(sensitiveTableNameAllColumnMap);
                SensitiveInterceptor sensitiveInterceptor = new SensitiveInterceptor();
                SecurityPlugin securityPlugin = new SecurityPlugin(securityConfig);
                DbProductEnum dbProductEnum = DbProductEnum.MYSQL;
                try {
                    dbProductEnum = DbProductEnum.nameOf(metaDataManager.getDs().getMetaData().getMetaData().getDatabaseProductName());
                } catch (SQLException e) {
                    log.error("加载数据库引擎失败", e);
                }
                securityPlugin.setDbProductEnum(dbProductEnum);
                sensitiveInterceptor.setSecurityPlugin(securityPlugin);
                //注册拦截器
                List<Interceptor> interceptors = Convert.toList(Interceptor.class, sqlManager.getInters());
                interceptors.add(0, sensitiveInterceptor);
                sqlManager.setInters(interceptors.toArray(new Interceptor[]{}));
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sensitiveInterceptor(securitySchemeTableConfig);
    }
}
