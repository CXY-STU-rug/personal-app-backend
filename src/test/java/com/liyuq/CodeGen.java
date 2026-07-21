package com.liyuq;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器（修复版）
 *
 * 相比上一版修复的两个 bug：
 *   1. 数据库密码之前是占位符"你的MySQL密码"，会报 Access denied，已改成真实密码
 *   2. 之前没指定模板引擎，FastAutoGenerator 默认走 Velocity，
 *      而 pom 里只引了 Freemarker（generator 的 pom 里两个引擎都是 optional，不会自动传递进来），
 *      运行会报 NoClassDefFoundError: org/apache/velocity/...，
 *      修复方式是在 execute() 前显式 .templateEngine(new FreemarkerTemplateEngine())
 *
 * 本版的关键设计：输出到项目根目录的 generated-code/ 隔离文件夹，不直接写进 src/main/java。
 * 原因：src/main/java 里已有手写的 entity/mapper（如 Users.java），
 * 和生成器的命名规则（users 表 → Users.java）对不上，直接生成会出现两套重名类并存。
 * 生成后自己去 generated-code/ 里挑需要的文件拷进主工程。
 *
 * 运行方式：IDEA 里右键本类 → Run 'CodeGen.main()'（它在 test 目录但有 main 方法，可直接运行）
 */
public class CodeGen {

    /** 生成结果的隔离输出目录：项目根目录/generated-code，检查后手动拷贝需要的文件 */
    private static final String OUTPUT_DIR = System.getProperty("user.dir") + "/generated-code";

    public static void main(String[] args) {
        // 1. 数据库连接三要素：url / 用户名 / 密码（密码修复点：之前是占位符，现在是真实密码）
        FastAutoGenerator.create(
                        "jdbc:mysql://127.0.0.1:3306/personal_app?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                        "root",
                        "123456")

                // 2. 全局配置：作者名（写进类注释）、输出根目录、生成完不自动弹资源管理器窗口
                //    注意：dateType 在 3.5.7 里属于全局配置（老教程写在 entityBuilder 里，已失效）
                .globalConfig(builder -> {
                    builder.author("liyuq")
                            .outputDir(OUTPUT_DIR + "/java")   // Java 文件统一输出到隔离目录下
                            .dateType(DateType.TIME_PACK)      // 时间字段用 java.time 包（LocalDateTime），别用老的 Date
                            .disableOpenDir();
                })

                // 3. 包配置：生成代码的 package 结构，和主工程保持一致（com.liyuq.entity 等），
                //    这样拷贝文件进主工程时不用改 package 声明
                .packageConfig(builder -> {
                    builder.parent("com.liyuq")
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            // mapper.xml 文件不走 java 目录，单独指定到隔离目录的 resources/mapper 下
                            .pathInfo(Collections.singletonMap(OutputFile.xml, OUTPUT_DIR + "/resources/mapper"));
                })

                // 4. 策略配置：生成哪些表、每一层的生成细节
                .strategyConfig(builder -> {
                    // 要生成的 7 张表，和建表.sql 一一对应
                    builder.addInclude("users", "todos", "finance_categories",
                            "finance_records", "notes", "note_tags", "note_tag_relations");

                    // 实体层：开启 Lombok（生成 @Getter/@Setter，省掉手写 getter/setter）
                    // 命名策略在 3.5.7 里也归实体层管（老教程写在 strategyConfig 顶层，已失效）：
                    // naming = 表名转类名（finance_records → FinanceRecords）
                    // columnNaming = 列名转字段名（user_id → userId）
                    builder.entityBuilder()
                            .enableLombok()
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel);

                    // service 层：默认接口名是 IUsersService（带 I 前缀），改成 UsersService 更符合主工程习惯
                    builder.serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl");

                    // controller 层：生成 @RestController 风格（返回 JSON），而不是 @Controller（返回页面）
                    builder.controllerBuilder()
                            .enableRestStyle();
                })

                // 5. 模板引擎修复点：显式指定 Freemarker，不指定则默认 Velocity（pom 里没有，会运行时报错）
                .templateEngine(new FreemarkerTemplateEngine())

                // 6. 执行生成
                .execute();

        System.out.println("===== 代码生成完成，请到 " + OUTPUT_DIR + " 目录检查生成结果 =====");
    }
}
