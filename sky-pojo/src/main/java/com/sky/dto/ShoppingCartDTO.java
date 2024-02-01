package com.sky.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ShoppingCartDTO implements Serializable {

    private Long setmealId;
    private Long dishId;
    private String dishFlavor;

}
