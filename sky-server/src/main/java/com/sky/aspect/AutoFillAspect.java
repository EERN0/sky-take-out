package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /*
     * 定义切入点: 拦截具体类的方法，决定哪些方法需要加入通知 --> com.sky.mapper包下所有类的所有方法 && 方法上加了注解@AutoFill
     * */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCt() {
    }

    /*
     * 前置通知方法，在通知中进行公共字段的赋值
     * (获取拦截到的方法的具体信息)
     * */
    @Before("autoFillPointCt()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段填充...");

        // 获取当前被拦截方法上的数据库操作类型，insert or update
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();  // 方法签名对象
        AutoFill autoFil = signature.getMethod().getAnnotation(AutoFill.class); // 获得方法上的注解对象
        OperationType operationType = autoFil.value(); // 获得数据库操作类型

        // 获取当前被拦截方法的参数--实体对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;

        Object entity = args[0];    // 定义方法时，实体对象统一放到第1位

        // 准备赋值的数据：当前时间、登录用户的id
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 使用反射，根据当前不同的数据库操作，为对应的属性赋值
        if (operationType == OperationType.INSERT) {  // insert操作，为createTime updateTime createUser updateUser四个字段赋值
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射为对象属性赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (operationType == OperationType.UPDATE) { // update操作，为updateTime updateUser两个字段赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
