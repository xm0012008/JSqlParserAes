package cn.cz.aischool.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 单个scheme下的表名,敏感列,全部列的配置项
 *
 * @author xuming
 */
@Data
public class SecuritySchemeTableConfig {

    /**
     * 加解密key
     */
    private String encryptKey;

    /**
     * scheme名
     */
    private List<SchemeConfig> schemaConfigList;

    @Data
    public static class SchemeConfig {
        /**
         * scheme名
         */
        private String scheme;

        /**
         * 读取应用配置后传入
         * key:敏感表名
         * val:敏感表中的敏感列
         */
        private Map<String, List<String>> sensitiveTableSensitiveColumnMap;

        /**
         * 从元数据中动态获取后构造并传入
         * key:敏感表名
         * val:敏感表中的全部列信息
         */
        private Map<String, List<String>> sensitiveTableAllColumnMap;
    }

}
