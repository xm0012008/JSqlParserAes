package cn.cz.aischool.parameter;

import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.JdbcParameter;

/**
 * 经转换后的参数
 *
 * @author xuming
 */
@RequiredArgsConstructor
public class EncryptJdbcParameter extends JdbcParameter {

    public final String format;

    @Override
    public String toString() {
        return format;
    }
}
