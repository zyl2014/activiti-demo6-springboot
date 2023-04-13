package com.yawn.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yawn.entity.UserDTO;
import org.activiti.engine.impl.persistence.entity.AttachmentEntityImpl;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 申请流程 审批信息
 *
 * @author LEN
 */

public class ApprovalOpinionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批人id
     */
    private String opId;
    /**
     * 审批人姓名
     */
    private String opName;
    /**
     * 审批意见
     */
    private String opinion;
    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    /**
     * DIctEnum  流程审批操作类型:
     * 是否通过 0拒绝,1同意,2驳回。。。
     */
    private String flag;

    /**
     * 审批结果对应的字典值
     */
    private String flagStr;
    /**
     * 流程id
     */
    private String taskId;
    /**
     * 当前节点名称
     */
    private String taskNodeName;

    /**
     * 附件
     */
    private List<AttachmentEntityImpl> attachments;

    /**
     * 下一步审核人是否默认 0 默认,1 审批人指定
     */
    private String defNextAssignee;

    /**
     * 指定的下一步审核人
     */
    private UserDTO nextAssignee;

    /**
     * 驳回到指定节点id
     */
    private String runNodeId;

    /**
     * 节点样式
     */
    private String style;

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }

    public String getOpName() {
        return opName;
    }

    public void setOpName(String opName) {
        this.opName = opName;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getFlagStr() {
        return flagStr;
    }

    public void setFlagStr(String flagStr) {
        this.flagStr = flagStr;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskNodeName() {
        return taskNodeName;
    }

    public void setTaskNodeName(String taskNodeName) {
        this.taskNodeName = taskNodeName;
    }

    public List<AttachmentEntityImpl> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentEntityImpl> attachments) {
        this.attachments = attachments;
    }

    public String getDefNextAssignee() {
        return defNextAssignee;
    }

    public void setDefNextAssignee(String defNextAssignee) {
        this.defNextAssignee = defNextAssignee;
    }

    public UserDTO getNextAssignee() {
        return nextAssignee;
    }

    public void setNextAssignee(UserDTO nextAssignee) {
        this.nextAssignee = nextAssignee;
    }

    public String getRunNodeId() {
        return runNodeId;
    }

    public void setRunNodeId(String runNodeId) {
        this.runNodeId = runNodeId;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}

