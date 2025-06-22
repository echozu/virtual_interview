package com.echo.virtual_interview.constant;

/**
 * 模块类型常量类，替代原有的 ModuleType 枚举
 */
public final class ModuleTypeConstants {
    // 私有构造函数，防止被实例化
    private ModuleTypeConstants() {}

    public static final String BASIC_INFO = "BASIC_INFO";   // 基础信息键值对
    public static final String EDUCATION = "EDUCATION";    // 教育经历
    public static final String SKILLS = "SKILLS";       // 相关技能
    public static final String PROJECT = "PROJECT";      // 项目经历
    public static final String HONOR = "HONOR";        // 荣誉证书
    public static final String EVALUATION = "EVALUATION";   // 个人评价
}