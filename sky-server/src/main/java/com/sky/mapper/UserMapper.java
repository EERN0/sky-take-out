package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     *
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenID(String openid);

    /**
     * 插入数据
     * 插入user之后，要返回主键值（在xml里面设置 配置文件）
     *
     * @param user
     */
    void insert(User user);
}
