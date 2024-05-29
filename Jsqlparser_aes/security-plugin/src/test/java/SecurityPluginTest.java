import cn.cz.aischool.config.SecuritySchemeTableConfig;
import cn.cz.aischool.engine.DbProductEnum;
import cn.cz.aischool.plugin.SecurityPlugin;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

import java.util.List;
import java.util.Map;

/**
 * 插件测试
 * 敏感表:tb_teacher
 * 敏感表中的敏感列:certify_number
 *
 * @author xuming
 */
@Slf4j
public class SecurityPluginTest {

    /**
     * 敏感表所在的scheme
     */
    private static final String AI_SCHOOL_CENTER = "ai_school_center";
    /**
     * 敏感表
     */
    private static final String TB_TEACHER = "tb_teacher";
    private static final String TB_STUDENT_INFORMATION = "tb_student_information";

    private static final String TB_BA_STUDENT = "ba_student";
    private static final String CZ_STUDENT = "cz_student";
    private static final String TB_STUDENT_FAMILY = "tb_student_family";
    private static final String TB_STUDENT_RESULT = "tb_student_result";
    /**
     * 敏感列
     */
    private static final String CERTIFY_NUMBER = "certify_number";
    private static final String STUDENT_CERTIFY_NUMBER = "student_certify_number";
    private static final String STUDENT_PHONE = "student_phone";
    private static final String PHONE = "phone";
    private static final String IDCARD = "idcard";
    private static final String GUARDIANS_PHONE = "guardian_phone";
    /**
     * 敏感字段加解密key
     */
    private static final String ENCRYPT_KEY = "encrypt_key";
    /**
     * 敏感表中的全部列信息
     */
    private static final List<String> tbTeachertableAllColumnList =
        Lists.newArrayList("id                        ", "school_id                 ", "user_id                   ",
            "name                      ", "certify_type              ", "certify_number            ",
            "gender_code               ", "phone                     ", "nation_code               ",
            "resident_province         ", "resident_city             ", "resident_district         ",
            "resident_street           ", "resident_community        ", "family_address            ",
            "resident_detail           ", "residence_booklet_type    ", "teacher_certificate_number",
            "birthdate                 ", "nationality               ", "oversea_chinese           ",
            "high_education            ", "high_degree               ", "high_degree_school        ",
            "duty                      ", "professional_title        ", "politics_state_code       ",
            "head_portrait             ", "face_url                  ", "created_by                ",
            "created_at                ", "updated_by                ", "updated_at                ",
            "del_flag                  ");

    private static final List<String> tbStudentInformationAllColumnList = Lists.newArrayList("id",
        "user_id", "school_id", "student_name", "gender_code", "nation", "certify_type", "certify_number",
        "student_number", "birthdate", "overseas_flag", "migrant_worker_flag", "left_behind_flag", "only_child_flag",
        "head_portrait", "lodge", "politics_state_code", "face_url", "nationality", "province", "city", "county",
        "resident_street", "resident_community", "resident_detail", "residence_booklet_type", "parent_bind",
        "created_by", "created_at", "updated_by", "updated_at", "del_flag"

    );

    private static final List<String> tbStudentFamilyAllColumnList = Lists.newArrayList(
        "id", "student_id", "student_certify_number", "parent_name", "phone", "certify_number",
        "guardianship", "home", "created_by", "created_at", "updated_by", "updated_at", "del_flag"

    );

    private static final List<String> tbStudentResultAllColumnList = Lists.newArrayList(
        "id", "student_id", "student_certify_number", "student_phone", "result1", "result2", "result3", "result4",
        "del_flag");


    public static void main(String[] args) throws Exception {
        Map<String, List<String>> sensitiveTableNameColumnMap = Maps.newHashMap();
        sensitiveTableNameColumnMap.put(TB_TEACHER, Lists.newArrayList(CERTIFY_NUMBER));
        sensitiveTableNameColumnMap.put(TB_STUDENT_FAMILY,
            Lists.newArrayList(STUDENT_CERTIFY_NUMBER, PHONE, CERTIFY_NUMBER));
        sensitiveTableNameColumnMap.put(TB_STUDENT_RESULT, Lists.newArrayList(STUDENT_CERTIFY_NUMBER, STUDENT_PHONE));
        sensitiveTableNameColumnMap.put(TB_STUDENT_INFORMATION, Lists.newArrayList(CERTIFY_NUMBER, PHONE));
        sensitiveTableNameColumnMap.put(TB_BA_STUDENT, Lists.newArrayList(GUARDIANS_PHONE, IDCARD));
        sensitiveTableNameColumnMap.put(CZ_STUDENT, Lists.newArrayList(GUARDIANS_PHONE));

        Map<String, List<String>> sensitiveTableAllColumnMap = Maps.newHashMap();
        sensitiveTableAllColumnMap.put(TB_TEACHER, tbTeachertableAllColumnList);
        sensitiveTableNameColumnMap.put(TB_STUDENT_FAMILY,
            Lists.newArrayList(PHONE, STUDENT_CERTIFY_NUMBER, CERTIFY_NUMBER));
        sensitiveTableAllColumnMap.put(TB_STUDENT_RESULT, tbStudentResultAllColumnList);
        sensitiveTableAllColumnMap.put(TB_STUDENT_INFORMATION,tbStudentInformationAllColumnList);


        SecuritySchemeTableConfig securitySchemeTableConfig = new SecuritySchemeTableConfig();
        securitySchemeTableConfig.setEncryptKey(ENCRYPT_KEY);
        SecuritySchemeTableConfig.SchemeConfig schemeConfig = new SecuritySchemeTableConfig.SchemeConfig();
        schemeConfig.setScheme(AI_SCHOOL_CENTER);
        schemeConfig.setSensitiveTableSensitiveColumnMap(sensitiveTableNameColumnMap);
        schemeConfig.setSensitiveTableAllColumnMap(sensitiveTableAllColumnMap);
        securitySchemeTableConfig.setSchemaConfigList(Lists.newArrayList(schemeConfig));
        SecurityPlugin securityPlugin = new SecurityPlugin(securitySchemeTableConfig);
        securityPlugin.setDbProductEnum(DbProductEnum.MYSQL);
//        String selectSql =
//            "select t.certify_number , t.id_num from   `ai_school_center`.tb_teacher t where t.certify_number = ? and id"
//                + " = ? and name = ? and sex = ? and certify_number in (?,?,?) or name in (? ,?)";
//        log.info("--------------------------------------------带schema的简单select"
//            + "--------------------------------------------");
//        log.info("原始语句:{}", selectSql);
//        log.info("修改后的语句:{}", doConvert(selectSql, securityPlugin));
//
//        log.info(
//            "--------------------------------------------简单select+groupBy--------------------------------------------");
//        String groupBySql = "select length(t.certify_number) '证件号长度',count(t.certify_number) as '证件号个数' from "
//            + "tb_teacher t" + " where length(t.certify_number) <? group by length(t.certify_number)";
//        log.info("原始语句:{}", groupBySql);
//        log.info("修改后的语句:{}", doConvert(groupBySql, securityPlugin));
//
//        log.info(
//            "--------------------------------------------简单select+groupBy"
//                + "+having--------------------------------------------");
//        String groupBySql2 = "SELECT count(1),certify_number as 'cNum' FROM tb_teacher GROUP BY "
//            + "certify_number having certify_number = ? and certify_number like ?";
//        log.info("原始语句:{}", groupBySql2);
//        log.info("修改后的语句:{}", doConvert(groupBySql2, securityPlugin));
//
//        log.info("--------------------------------------------复合select1--------------------------------------------");
//        String complexSelect1 =
//            "select t1.certify_number aS 'certifyNumber' ,t2.certify_number from tb_teacher t1 left "
//                + "join tb_teacher t2 on t1.id = t2.id where t1.certify_number = ? and t2.certify_number = ? and t2.id = ?";
//        log.info("原始语句:{}", complexSelect1);
//        log.info("修改后的语句:{}", doConvert(complexSelect1, securityPlugin));
//
//        log.info("--------------------------------------------复合select2--------------------------------------------");
//        String complexSelect2 = "select t1.certify_number 'cNumber' ,t2.dept_id 'dId' ,t3.dept_name '部门名称' from "
//            + "tb_teacher t1,tb_department_teacher t2 , tb_department t3 where t1.id = t2.teacher_id and t2.dept_id ="
//            + " t3.id and t1.del_flag = 0 and t2.del_flag = 0 and t1.id = ? and t1.certify_number = ? and t2"
//            + ".certify_number like ? and t3.certify_number in (?,?)";
//        log.info("原始语句:{}", complexSelect2);
//        log.info("修改后的语句:{}", doConvert(complexSelect2, securityPlugin));
//
//        log.info("--------------------------------------------复合select3--------------------------------------------");
//        String complexSelect3 =
//            "select * from (select * from (select id,phone,certify_number 'cNumber' from tb_teacher where "
//                + "certify_number = ? and phone = ?) t2 where t2.certify_number = ?) t1";
//        log.info("原始语句:{}", complexSelect3);
//        log.info("修改后的语句:{}", doConvert(complexSelect3, securityPlugin));
//
//        log.info(
//            "--------------------------------------------敏感表的简单select*--------------------------------------------");
//        String selectAll = "select * from tb_teacher t where t.id = ? and certify_number = ? limit ?,?";
//        log.info("原始语句:{}", selectAll);
//        log.info("修改后的语句:{}", doConvert(selectAll, securityPlugin));
//
//        log.info(
//            "--------------------------------------------不敏感表的简单select*--------------------------------------------");
//        String selectAll2 = "select * from tb_user t where t.id = ? and certify_number = ?";
//        log.info("原始语句:{}", selectAll2);
//        log.info("修改后的语句:{}", doConvert(selectAll2, securityPlugin));
//
//        log.info(
//            "--------------------------------------------不带from的select--------------------------------------------");
//        String specialSelect = "select 1";
//        log.info("原始语句:{}", specialSelect);
//        log.info("修改后的语句:{}", doConvert(specialSelect, securityPlugin));
//
//        log.info("--------------------------------------------select+like--------------------------------------------");
//        String selectLike = "select id, certify_number ,phone from tb_teacher t where t.certify_number like ?";
//        log.info("原始语句:{}", selectLike);
//        log.info("修改后的语句:{}", doConvert(selectLike, securityPlugin));
//
//        log.info(
//            "--------------------------------------------单条数据insert1--------------------------------------------");
//        String insertSql1 = "insert into tb_teacher(id,id_num,certify_number) values (?,?,?)";
//        log.info("原始语句:{}", insertSql1);
//        log.info("修改后的语句:{}", doConvert(insertSql1, securityPlugin));
//
//        log.info(
//            "--------------------------------------------单条数据insert2--------------------------------------------");
//        String insertSql2 = "insert into tb_teacher(id,certify_number,id_num) values (?,?,?)";
//        log.info("原始语句:{}", insertSql2);
//        log.info("修改后的语句:{}", doConvert(insertSql2, securityPlugin));
//
//        log.info(
//            "--------------------------------------------多条数据insert--------------------------------------------");
//        String insertSql3 = "insert into tb_teacher (certify_number,id,phone) values (?,?,?),(?,?,?),(?,?,?),(?,?,?)";
//        log.info("原始语句:{}", insertSql3);
//        log.info("修改后的语句:{}", doConvert(insertSql3, securityPlugin));
//
//        log.info("--------------------------------------------update1--------------------------------------------");
//        String updateSql = "update tb_teacher t set t.id_num = ? ,t.certify_number = ? where t.id = ?";
//        log.info("原始语句:{}", updateSql);
//        log.info("修改后的语句:{}", doConvert(updateSql, securityPlugin));
//
//        log.info("--------------------------------------------update2--------------------------------------------");
//        String updateSql2 = "update tb_teacher t set t.id_num = ? ,t.certify_number = ? where t.certify_number = ?";
//        log.info("原始语句:{}", updateSql2);
//        log.info("修改后的语句:{}", doConvert(updateSql2, securityPlugin));
//
//        log.info("--------------------------------------------简单delete1--------------------------------------------");
//        String delSql1 = "delete from tb_teacher t where t.certify_number like ? and t.name like ? and t.phone like ? "
//            + "and id = ? and t.certify_number = ? and t.certify_number like ?";
//        log.info("原始语句:{}", delSql1);
//        log.info("修改后的语句:{}", doConvert(delSql1, securityPlugin));
//
//        log.info("--------------------------------------------两表left "
//            + "join--------------------------------------------");
//        String innerJoinSql = "SELECT t1.certify_number,t2.* FROM tb_class_student t1 LEFT JOIN tb_student_information t2 ON t1"
//            + ".student_id = t2.id AND t2.del_flag = 0 WHERE t1.class_id IN ('1','2') AND t1.del_flag = 0";
//        log.info("原始语句:{}", innerJoinSql);
//        log.info("修改后的语句:{}", doConvert(innerJoinSql, securityPlugin));

//        log.info("--------------------------------------------两表left "
//            + "join2--------------------------------------------");
//        String innerJoinSql2 =
//            " SELECT tti.*,td.* FROM tb_department td INNER JOIN tb_department_teacher tdt ON td.id = tdt.dept_id AND "
//                + "tdt.del_flag = 0 INNER JOIN tb_teacher tti ON tdt.teacher_id = tti.id AND tti.del_flag = 0 WHERE td.del_flag = 0 AND tti.user_id = ?";
//        log.info("原始语句:{}", innerJoinSql2);
//        log.info("修改后的语句:{}", doConvert(innerJoinSql2, securityPlugin));

//
        String sqlqqq = "SELECT\n" + "\ttst.id,\n" + "\tcst.user_id,\n" + "\ttst.province_code,\n"
            + "\ttst.city_code,\n" + "\ttst.area_code,\n" + "\ttst.neighborhood_code,\n" + "\ttst.street_code,\n"
            + "\ttst.address,\n" + "\ttst.other_domicile,\n" + "\ttst.guardian,\n" + "\ttst.guardian_phone,\n"
            + "\tcst.school_id,\n" + "\tcst.class_org_id,\n" + "\tcst.NAME,\n" + "\tcst.sex,\n"
            + "\tcst.has_disability,\n" + "\tcst.hmt_flag,\n" + "\tcst.id_card,\n"
            + "\tcst.province_code AS householdProvinceCode,\n" + "\tcst.city_code AS householdCityCode,\n"
            + "\tcst.area_code AS householdAreaCode,\n" + "\tcst.street_code AS householdStreetCode,\n"
            + "\tcst.neighborhood_code AS householdNeighborhoodCode,\n" + "\tcst.address AS householdAddress,\n"
            + "\tcst.guardian AS czGuardian,\n" + "\tcst.guardian_phone AS czGuardianPhone,\n" + "\tcs.school_name,\n"
            + "\tcc.class_name \n" + "FROM\n" + "\tcz_student cst\n"
            + "\tLEFT JOIN t_student tst ON cst.user_id = tst.user_id \n" + "\tAND tst.del_flag = 0\n"
            + "\tLEFT JOIN cz_school cs ON cs.school_id = cst.school_id\n"
            + "\tLEFT JOIN cz_class cc ON cc.class_org_id = cst.class_org_id \n" + "WHERE\n" + "\t1 = 1 \n"
            + "\tAND cst.school_id = 'd86f0bde12924565a930adf22045ed99'\n" + "\t\n" + "\tAND cst.class_org_id = 600\n"
            + "\t\n" + "\tAND cst.user_id = '1545236927123136513'";

        log.info("原始语句:{}", sqlqqq);
        log.info("修改后的语句:{}", doConvert(sqlqqq, securityPlugin));
    }

    /**
     * sql转换
     *
     * @param oldSql         原始sql
     * @param securityPlugin
     * @return 转换后的sql
     * @throws JSQLParserException
     */
    private static String doConvert(String oldSql, SecurityPlugin securityPlugin) throws JSQLParserException {
        int count = 1;
        String newSql = null;
        while (count <= 1) {
            //1、原始sql
            log.debug("第{}次转换开始,原始sql:{}", count, oldSql);
            long start = System.currentTimeMillis();
            //2、使用解析器解析sql生成sql抽象语法树
            Statement statement = CCJSqlParserUtil.parse(oldSql);
            //3、将自定义访问器传入解析后的sql对象
            statement.accept(securityPlugin.getSecurityStatementVisitor());
            //4、打印转换后的sql语句
            long end = System.currentTimeMillis() - start;
            log.debug("第{}次转换结束,替换后的sql:[{}],sql转换耗时:[{}]毫秒", count, statement, end);
            newSql = statement.toString();
            count++;
        }
        return newSql;
    }
}
