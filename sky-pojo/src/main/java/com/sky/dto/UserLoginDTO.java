package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户登录
 * 登录小程序时获取的 code，可通过wx.login获取
 */
@Data
public class UserLoginDTO implements Serializable {

    private String code;

}
