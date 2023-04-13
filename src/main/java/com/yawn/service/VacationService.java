package com.yawn.service;

import com.alibaba.fastjson.JSONObject;
import com.yawn.entity.VacTask;
import com.yawn.entity.Vacation;
import com.yawn.util.ActivitiUtil;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Created by yawn on 2018-01-08 13:44
 */
@Service
public class VacationService {

    private static final String PROCESS_DEFINE_KEY = "支永林测试流程";
    protected org.slf4j.Logger Logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private IdentityService identityService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RebutService rebutService;

    public Object startVac(String userName, Vacation vac) {

        //发起人逻辑
        identityService.setAuthenticatedUserId(userName);
        // 开始流程
        Map<String, Object> vars = new HashMap<>(4);
        vars.put("applyUser", userName);
        vars.put("days", vac.getDays());
        vars.put("reason", vac.getReason());
        vars.put("handler", "managea");
        //变量替换

        ArrayList<Object> userList = new ArrayList<>();
        userList.add("empa");
        userList.add("empb");
        vars.put("userList", userList);


        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINE_KEY, UUID.randomUUID()
                .toString(), vars);

        runtimeService.addUserIdentityLink(processInstance.getId(), userName, IdentityLinkType.STARTER);
        runtimeService.addGroupIdentityLink(processInstance.getId(), "empGroup", IdentityLinkType.CANDIDATE);

        System.out.println("vacationInstance.id +   " + processInstance.getId());

        return true;
    }

    public Object myVac(String userName) {
        List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery()
                .startedBy(userName)
                .list();
        List<Vacation> vacList = new ArrayList<>();
        for (ProcessInstance instance : instanceList) {
            Vacation vac = getVac(instance);
            vacList.add(vac);
        }
        Logger.warn("我申请的任务单子:{}", JSONObject.toJSONString(vacList));
        return vacList;
    }

    private Vacation getVac(ProcessInstance instance) {
        Integer days = runtimeService.getVariable(instance.getId(), "days", Integer.class);
        String reason = runtimeService.getVariable(instance.getId(), "reason", String.class);
        Vacation vac = new Vacation();
        vac.setApplyUser(instance.getStartUserId());
        vac.setDays(days);
        vac.setReason(reason);
        vac.setFlowInstanceId(instance.getId());
        Date startTime = instance.getStartTime(); // activiti 6 才有
        vac.setApplyTime(startTime);
        vac.setApplyStatus(instance.isEnded() ? "申请结束" : "等待审批");
        return vac;
    }


    public Object myAudit(String userName) {
        List<Task> taskList = taskService.createTaskQuery()
                .taskCandidateOrAssigned(userName)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<Task> taskList2 = taskService.createTaskQuery()
                .taskUnassigned()
                .orderByTaskCreateTime()
                .desc()
                .list();

        //        List<Task> tasks = new ArrayList<>();
        //        tasks.add(taskList);
        taskList.addAll(taskList2);

        //1.分配人 和 候选人  不同的逻辑点
        //2.基于 activity的 用户组进行识别，也是先转为用户再进行判定

        Group group = identityService.createGroupQuery()
                .groupMember(userName)
                .singleResult();
        List<String> groupList = new ArrayList<>();
        groupList.add(group.getId());
        List<Task> list = taskService.createTaskQuery()
                .taskCandidateGroupIn(groupList)
                .list();
        taskList.addAll(list);
        List<VacTask> vacTaskList = new ArrayList<>();
        for (Task task : taskList) {
            VacTask vacTask = new VacTask();
            vacTask.setId(task.getId());

            //查看执行后的节点信息
            Map<String, String> runNodes = rebutService.getRunNodes(task.getId());
            Logger.warn("taskId:{},runNodes:{}", task.getId(), JSONObject.toJSONString(runNodes));

            vacTask.setName(task.getName());
            vacTask.setCreateTime(task.getCreateTime());
            vacTask.setAuditor(task.getAssignee());
            vacTask.setEndTime(task.getClaimTime());
            String instanceId = task.getProcessInstanceId();
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();
            Vacation vac = getVac(instance);
            vacTask.setTaskId(task.getId());
            vac.setFlowInstanceId(task.getProcessInstanceId());
            vacTask.setVac(vac);
            vacTaskList.add(vacTask);
        }
        return vacTaskList;
    }

    public Object passAudit(String userName, VacTask vacTask) {
        String taskId = vacTask.getId();
        String result = vacTask.getVac()
                .getResult();
        Map<String, Object> vars = new HashMap<>();
        vars.put("result", result);
        vars.put("auditor", userName);
        vars.put("auditTime", new Date());
        taskService.claim(taskId, userName);
        taskService.complete(taskId, vars);
        return true;
    }

    public Object myVacRecord(String userName) {
        List<HistoricProcessInstance> hisProInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINE_KEY)
                .startedBy(userName)
                //                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();

        List<Vacation> vacList = new ArrayList<>();
        for (HistoricProcessInstance h : hisProInstance) {

            HistoricProcessInstanceEntityImpl hisInstance = (HistoricProcessInstanceEntityImpl) h;

            Vacation vacation = new Vacation();
            vacation.setApplyUser(hisInstance.getStartUserId());
            vacation.setApplyTime(hisInstance.getStartTime());
            vacation.setEndTime(hisInstance.getEndTime());
            vacation.setFlowInstanceId(hisInstance.getProcessInstanceId());
            List<HistoricVariableInstance> varInstanceList = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hisInstance.getId())
                    .list();
            ActivitiUtil.setVars(vacation, varInstanceList);
            vacList.add(vacation);
        }
        return vacList;
    }


    public Object myAuditRecord(String userName) {
        List<HistoricProcessInstance> hisProInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINE_KEY)
                .involvedUser(userName)
                .orderByProcessInstanceEndTime()
                .desc()
                .list();



        List<String> auditTaskNameList = new ArrayList<>();
        //        auditTaskNameList.add("经理审批");
        //        auditTaskNameList.add("总监审批");
        List<Vacation> vacList = new ArrayList<>();
        for (HistoricProcessInstance hisInstance : hisProInstance) {

            //            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            //                    .processInstanceId(hisInstance.getId())
            //                    .singleResult();


            List<HistoricTaskInstance> hisTaskInstanceList = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(hisInstance.getId())
                    //                    .processFinished()
                    .taskAssignee(userName)
                    //                    .taskNameIn(auditTaskNameList)
                    .orderByHistoricTaskInstanceEndTime()
                    .desc()
                    .list();
            //            boolean isMyAudit = false;
            for (HistoricTaskInstance t : hisTaskInstanceList) {
                //                if (taskInstance.getAssignee()
                //                        .equals(userName)) {
                ////                    isMyAudit = true;
                //                }

                HistoricTaskInstanceEntityImpl taskInstance = (HistoricTaskInstanceEntityImpl) t;


                Vacation vacation = new Vacation();
                vacation.setFlowInstanceId(hisInstance.getId());
                vacation.setApplyUser(hisInstance.getStartUserId());
                vacation.setTaskName(taskInstance.getName());
                vacation.setTaskId(taskInstance.getId());
                vacation.setTaskStatus(taskInstance.getEndTime() != null ? "已完结" : "处理中");
                vacation.setApplyStatus(hisInstance.getEndTime() != null ? "已结束" : "审批中");
                vacation.setAuditor(taskInstance.getAssignee());
                //                vacation.setApplyStatus("申请结束");
                vacation.setApplyTime(taskInstance.getStartTime());
                List<HistoricVariableInstance> varInstanceList = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(hisInstance.getId())
                        .list();
                ActivitiUtil.setVars(vacation, varInstanceList);
                vacList.add(vacation);
            }

        }
        Collections.sort(vacList, Comparator.comparing(Vacation::getAuditTime).reversed());
        return vacList;
    }

    public Object accept(String userName, String taskId) {
        //领取操作
        taskService.setAssignee(taskId, userName);
        Logger.info("领取任务:{},{}", userName, taskId);
        return null;
    }

    public List<Object> noAccept() {
        return taskService.createTaskQuery()
                .taskUnassigned()
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .map(Task::getId)
                .collect(Collectors.toList());

    }
}
