package com.yawn.entity;

import java.io.Serializable;
import java.util.Map;

/**
 * 动态表单配置
 *
 * @author Create by YLL
 * @date 2020/4/1 14:37
 */

public class DynamicFormConf implements Serializable {
    /**
     * 审批操作类型
     */
    private Map<String, String> approvalTypeFlag;

    /**
     * 是否可以修改表单
     */
    private boolean writable;

    /**
     * 字段是否可编辑(批量控制)
     */
    private boolean fieldWritable;

    /**
     * 是否可以指定下一步办理人
     */
    private boolean nextAssign;

    /**
     * 存放已经执行的节点信息
     */
    private Map<String, String> runNodes;
}
