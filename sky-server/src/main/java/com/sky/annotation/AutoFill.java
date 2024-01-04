package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于表示某个方法需要进行功能字段自动填充处理
 * <p>
 * 在方法上使用这个注解：
 *
 * @AutoFill(value = OperationType.INSERT)  // value= 可以省略掉
 * public void someMethod() {
 * // 方法实现
 * }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    // 数据库操作为update insert时，才执行公共字段的自动填充操作（填充创建、更新的时间和用户）
    OperationType value();  // 定义成员变量 value 类型是 OperationType 枚举
}
