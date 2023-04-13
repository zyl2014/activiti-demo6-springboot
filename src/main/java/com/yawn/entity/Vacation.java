package com.yawn.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author Created by yawn on 2018-01-08 16:44
 */
@Data
public class Vacation {

    /**
     * 申请人
     */
    private String applyUser;
    private int days;
    private String reason;
    private Date applyTime;
    private Date endTime;
    private String applyStatus;

    /**
     * 审核人
     */
    private String auditor;
    private String taskId;
    private String result;
    private Date auditTime;
    private String taskName;
    private String taskStatus;
    private String flowInstanceId;


}
