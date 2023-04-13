package com.yawn.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.yawn.costant.Constant;
import com.yawn.entity.ApprovalOpinionConverter;
import com.yawn.entity.ApprovalOpinionDTO;
import com.yawn.entity.ApprovalOpinionVO;
import com.yawn.entity.CustomException;
import com.yawn.util.DictEnum;
import com.yawn.util.UserUtils;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.util.CollectionUtil;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author yonglin.zhi. Date: 2023/4/12 Time: 17:56
 */
@Service
public class RebutService {
    public static final String ACT_APPLY_OPINION_LIST = "applyOpinionList";

    @Resource
    private HistoryService historyService;

    @Resource
    private TaskService taskService;

    @Resource
    private RepositoryService repositoryService;

    /**
     * 根据任务id查询已经执行的任务节点信息
     */
    public Map<String, String> getRunNodes(String taskId) {
        // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
        List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(taskId)
                .activityType("userTask")   //用户任务
                .finished()       //已经执行的任务节点
                .orderByHistoricActivityInstanceEndTime()
                .asc()
                .list();
        Map<String, String> map = new LinkedHashMap<String, String>();
        // 已执行的节点ID集合
        if (CollectionUtil.isNotEmpty(historicActivityInstanceList)) {

            // map = historicActivityInstanceList.stream().collect(Collectors.toMap(HistoricActivityInstance::getActivityId,HistoricActivityInstance::getActivityName,(k1,k2)->k1));
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstanceList) {
                if (!map.containsKey(historicActivityInstance.getActivityId())) {
                    map.put(historicActivityInstance.getActivityId(), historicActivityInstance.getActivityName());
                }
            }
            return map;
        }
        return map;
    }
    /**
     * 驳回到指定节点
     * @param approvalOpinionVO    //申请流程 审批信息
     * @param task  //任务信息
     * @param map
     * @return
     */
    public boolean runNodes(ApprovalOpinionVO approvalOpinionVO, Task task, Map<String, Object> map) {
        String myTaskId = null;
        //判断当前用户是否为该节点处理人
//        if (UserUtils.getUserId().equals(task.getAssignee())) {
//            myTaskId = task.getId();
//        }
        //如果当前节点处理人不是该用户,就无法进行驳回操作
        if (null == myTaskId) {
            throw new CustomException("当前用户无法驳回");
        }
        //获取当前节点
        String currActivityId = task.getTaskDefinitionKey();
        String processDefinitionId = task.getProcessDefinitionId();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        FlowNode currFlow = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currActivityId);

        if (null == currFlow) {
            List<SubProcess> subProcessList = bpmnModel.getMainProcess().findFlowElementsOfType(SubProcess.class, true);
            for (SubProcess subProcess : subProcessList) {
                FlowElement flowElement = subProcess.getFlowElement(currActivityId);
                if (flowElement != null) {
                    currFlow = (FlowNode) flowElement;
                    break;
                }
            }
        }
        //获取目标节点
        FlowNode targetFlow = (FlowNode) bpmnModel.getFlowElement(approvalOpinionVO.getRunNodeId());

        //如果不是同一个流程(子流程)不能驳回
        if (!(currFlow.getParentContainer().equals(targetFlow.getParentContainer()))) {
            throw new CustomException("此处无法进行驳回操作");
        }

        //记录原活动方向
        List<SequenceFlow> oriSequenceFlows = new ArrayList<>();
        oriSequenceFlows.addAll(currFlow.getOutgoingFlows());

        //清理活动方向
        currFlow.getOutgoingFlows().clear();

        //建立新的方向
        List<SequenceFlow> newSequenceFlows = new ArrayList();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        newSequenceFlow.setId(uuid);
        newSequenceFlow.setSourceFlowElement(currFlow);  //原节点
        newSequenceFlow.setTargetFlowElement(targetFlow);  //目标节点
        newSequenceFlows.add(newSequenceFlow);
        currFlow.setOutgoingFlows(newSequenceFlows);

        //审批意见叠加
        //variables 审批意见     act_ru_variable  变量表
        Map<String, Object> variables = task.getProcessVariables();
        //拒绝,通过,驳回 驳回指定节点
        List<ApprovalOpinionDTO> approvalOpinionDTOs = new ArrayList<>();
        //获取工作流审批记录
        Object options = variables.get(ACT_APPLY_OPINION_LIST);
        if (null != options) {
            approvalOpinionDTOs = JSONObject.parseArray(options.toString(), ApprovalOpinionDTO.class);
        }
        //添加审批过后的返回提升信息
        //ApprovalOpinionConverter 实体类转换器(没有 vo dto 等要求的可以不用转换,直接用一个类就可以了)
        ApprovalOpinionDTO applyOpinionDTO = ApprovalOpinionConverter.INSTANCE.vo2dto(approvalOpinionVO);
        applyOpinionDTO.setFlagStr(applyOpinionDTO.getTaskNodeName()+"撤回到"+targetFlow.getName());
        approvalOpinionDTOs.add(applyOpinionDTO);
        map.put(ACT_APPLY_OPINION_LIST, JSONObject.toJSONString(approvalOpinionDTOs));

        //完成节点任务
        taskService.complete(task.getId(), map);
        //恢复原方向
        currFlow.setOutgoingFlows(oriSequenceFlows);
        return true;
    }


    /**
     * 审批处理
     *
     * @param approvalOpinionVO
     * @return
     */
    public boolean handleApproval(ApprovalOpinionVO approvalOpinionVO) {
        Task task = taskService.createTaskQuery()
                .taskTenantId(UserUtils.getTenantId())  //租户
                .taskId(approvalOpinionVO.getTaskId())
                .includeProcessVariables()     //节点审批信息
                .singleResult();
        if (task == null) {
            throw new CustomException("流程未启动或已执行完成");
        }
        if (StringUtils.isNotEmpty(task.getAssignee()) && !UserUtils.getUserId().equals(task.getAssignee())) {
            throw new CustomException("当前用户不是审核人，无法进行审核");
        }

        //task.getAssignee() 获取审批人
        if (StringUtils.isEmpty(task.getAssignee())) {
            Set<String> candidates = getCandiates(task.getId());
            if (candidates.isEmpty()) {
                throw new CustomException("当前用户不是审核人，无法进行审核");
            }
            if (candidates.contains(UserUtils.getUserId())) {
                taskService.claim(task.getId(), UserUtils.getUserId());
            }
        }
        //获取流程变量
        Map<String, Object> processVariables = task.getProcessVariables();
        approvalOpinionVO.setCreateTime(new Date());
        approvalOpinionVO.setOpId(UserUtils.getUserId());
        approvalOpinionVO.setOpName("");
        approvalOpinionVO.setTaskNodeName(task.getName());
        Map<String, Object> map = Maps.newHashMap();
        map.put(Constant.ACT_TASK_FLAG, approvalOpinionVO.getFlag());    //审批操作 结果

        //驳回操作
        // if (DictEnum.APPLY_APPROVAL_OPINION_REJECT.getKey().equals(approvalOpinionVO.getFlag())) {
        //      return rejected(approvalOpinionVO, task, map);
        // }
        //驳回到指定步骤
        if(DictEnum.APPLY_APPROVAL_OPINION_ASSIGN.getKey().equals(approvalOpinionVO.getFlag())){
            return  runNodes(approvalOpinionVO, task, map);
        }
        // 审批信息叠加
        List<ApprovalOpinionDTO> opinionDTOS = new ArrayList<>();
        Object options = processVariables.get(Constant.ACT_APPLY_OPINION_LIST);
        if (options != null) {
            opinionDTOS = JSONObject.parseArray(options.toString(), ApprovalOpinionDTO.class);
        }
        ApprovalOpinionDTO applyOpinionDTO = ApprovalOpinionConverter.INSTANCE.vo2dto(approvalOpinionVO);
        opinionDTOS.add(applyOpinionDTO);
        map.put(Constant.ACT_APPLY_OPINION_LIST, JSONObject.toJSONString(opinionDTOS));
        taskService.complete(applyOpinionDTO.getTaskId(), map);
        return true;
    }

    private Set<String> getCandiates(String id) {
        return null;
    }

}
