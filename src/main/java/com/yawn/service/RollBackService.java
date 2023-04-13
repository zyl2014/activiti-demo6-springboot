package com.yawn.service;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yonglin.zhi. Date: 2023/4/13 Time: 18:20
 */
public class RollBackService {
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    /**
     * @param taskId 当前任务ID
     * @description 任务回退
     * @author giserDev
     * @email giserDev@163.com
     * @date 2020-10-18 02:04:03
     */
    public void taskRollBack(String taskId) {


        /**    取得当前任务	*/
        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .singleResult();

        /**    取得流程实例	*/
        ProcessInstance currInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(currTask.getProcessInstanceId())
                .singleResult();
        if (currInstance == null) {
            /**    流程结束	*/
            return;
        }

        /**    取得流程定义	*/
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) (processEngine.getRepositoryService()
                .getProcessDefinition(currTask.getProcessDefinitionId()));
        if (definition == null) {
            return;
        }


    }
}