package cn.cz.aischool.engine;

/**
 * 数据库引擎
 *
 * @Author: Jiu.hu
 * @Date: 2023/7/4 10:49
 * @Description:
 */
public enum DbProductEnum {

    MYSQL("MYSQL"),
    DM_DBMS("DM DBMS");

    private final String name;

    DbProductEnum(String name) {
        this.name = name;
    }

    public static DbProductEnum nameOf(String name) {
        if (name == null || name.isEmpty()) {
            return MYSQL;
        }
        for (DbProductEnum dbProductEnum : values()) {
            if (dbProductEnum.getName().equalsIgnoreCase(name)) {
                return dbProductEnum;
            }
        }
        return MYSQL;
    }

    public String getName() {
        return name;
    }
}
