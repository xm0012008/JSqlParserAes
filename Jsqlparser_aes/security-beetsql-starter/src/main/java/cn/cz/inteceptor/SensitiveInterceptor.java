package cn.cz.inteceptor;

import cn.cz.aischool.plugin.SecurityPlugin;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.lang3.StringUtils;
import org.beetl.sql.core.*;

import java.util.Objects;

/**
 * 敏感字段拦截器,用于处理sql中的敏感字段
 * @author xuming
 */
@Slf4j
@Data
public class SensitiveInterceptor implements Interceptor {

    private SecurityPlugin securityPlugin;

    @Override
    public void before(InterceptorContext ctx) {
        String sql = ctx.getSql();
        try {
            sql = StrUtil.replace(sql, "\n", StrUtil.SPACE);
            sql = StrUtil.replace(sql, "\t", StrUtil.SPACE);
            sql = StrUtil.removeAllLineBreaks(sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            statement.accept(securityPlugin.getSecurityStatementVisitor());
            ctx.setSql(statement.toString());
            log.debug("敏感sql转换成功,原始sql:{},转换后的sql:{}", sql, statement);
        } catch (JSQLParserException parserException) {
            log.warn("敏感sql转换失败,放弃转换,原始sql为:{},异常信息:{}", ctx.getSql(), parserException.getMessage());
        } catch (Exception e) {
            //sql解析失败,放弃处理
            log.warn("敏感sql转换失败,跳过处理:{}", sql);
            log.warn("", e);
        }
    }

    @Override
    public void after(InterceptorContext ctx) {

    }

    @Override
    public void exception(InterceptorContext ctx, Exception ex) {

    }
}
