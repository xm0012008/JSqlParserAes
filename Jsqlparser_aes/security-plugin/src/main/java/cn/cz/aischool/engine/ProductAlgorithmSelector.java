package cn.cz.aischool.engine;

/**
 * 数据库算法选择器
 *
 * @Author: Jiu.hu
 * @Date: 2023/7/4 10:56
 * @Description:
 */
public class ProductAlgorithmSelector {

    /**
     * MYSQL加密函数format
     */
    private static final String MYSQL_ENCRYPT_FUNCTION_FORMAT = "HEX(AES_ENCRYPT(?, '{}'))";
    /**
     * MYSQL解密函数format
     */
    private static final String MYSQL_DECRYPT_FUNCTION_FORMAT = "AES_DECRYPT(UNHEX({}),'{}')";

    /**
     * DM_DBMS 加密函数format
     */
    private static final String DM_ENCRYPT_FUNCTION_FORMAT = "CFALGORITHMSENCRYPT(?, 514, '{}')";
    /**
     * DM_DBMS 解密函数format
     */
    private static final String DM_DECRYPT_FUNCTION_FORMAT = "CFALGORITHMSDECRYPT({}, 514, '{}')";


    public static String getEncryptExp(DbProductEnum dbProductEnum) {
        switch (dbProductEnum) {
            case DM_DBMS:
                return DM_ENCRYPT_FUNCTION_FORMAT;
            default:
                return MYSQL_ENCRYPT_FUNCTION_FORMAT;
        }
    }

    public static String getDecryptExp(DbProductEnum dbProductEnum) {
        switch (dbProductEnum) {
            case DM_DBMS:
                return DM_DECRYPT_FUNCTION_FORMAT;
            default:
                return MYSQL_DECRYPT_FUNCTION_FORMAT;
        }

    }

}
