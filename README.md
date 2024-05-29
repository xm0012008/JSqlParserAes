# 数据库敏感字段sql加解密小插件
###
目前支持Mybatis,beetSql,理论上来说,只要提供了拦截器机制的ORM均可以支持

### 设计思路概述
#### 使用mysql函数hex(),unhex(),aes_decrypt(),aes_encrypt()对数据进行加解密:支持模糊查询+分页,对现有业务零侵入;
#### 设置/修改密码接口新增加密逻辑hex(aes_encrypt(?));
#### 详情/列表接口新增解密逻辑aes_decrypt(unhex(?));
#### mybatis,beetSql均提供Interceptor,各个对接应用在sql执行前捕捉到目标sql调用插件加解密sql替换,最后执行替换后的sql以调用数据库层加解密函数;
#### 针对特殊语句:select * from table的处理.应用启动阶段扫描并获取scheme中的敏感表结构等信息(Metadata),在Interceptor中建立敏感表table-[column,column,column...]的映射,用于替换*
#### 使用JSqlParser将原始sql语句解析为抽象语法树,逐层处理;
#### 插件提供一个测试类用于观察sql替换效果:
```java 
SecurityPluginTest
```

### 整体架构

[img]https://github.com/xm0012008/JSqlParserAes/blob/main/Jsqlparser_aes/security-plugin/img.png[img]



