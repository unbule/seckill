package com.wx.seckill.entry;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.internal.IgnoreForbiddenApisErrors;

import java.util.Date;

@Getter
@Setter
public class StockOrder {

    private Integer id;

    private Integer sid;

    private String name;

    private Date createTime;
}
