package com.yawn.controller;

import com.yawn.entity.User;
import com.yawn.service.UserService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * @author Created by yawn on 2018-01-08 13:39
 */
@Controller
public class LoginController {

    @Resource
    private UserService userService;

    @Resource
    private ProcessEngineConfiguration configuration;
    @Resource
    private ProcessEngine engine;

    private volatile boolean init = false;

    @PostMapping("/login")
    @ResponseBody
    public boolean login(HttpSession session, @RequestBody User user) {
        String userName = user.getUserName();
        String password = user.getPassword();
        boolean login = userService.login(userName, password);
        if (login) {
            session.setAttribute("userName", userName);
            return true;
        }
        return false;
    }


    @GetMapping("/init")
    public boolean initConfig() {
        initUser();
        init();
        return Boolean.TRUE;
    }

    public void initUser() {
        IdentityService is = engine.getIdentityService();
        // 添加用户组
        Group empGroup = saveGroup(is, "empGroup", "员工组");
        Group manageGroup = saveGroup(is, "manageGroup", "经理组");
        Group dirGroup = saveGroup(is, "dirGroup", "总监组");
        // 添加用户
        org.activiti.engine.identity.User empA = saveUser(is, "empa"); // 员工a
        org.activiti.engine.identity.User empB = saveUser(is, "empb"); // 员工b
        org.activiti.engine.identity.User managea = saveUser(is, "managea"); // 经理a
        org.activiti.engine.identity.User manageb = saveUser(is, "manageb"); // 经理b
        org.activiti.engine.identity.User dira = saveUser(is, "dira"); // 总监a
        // 绑定关系
        saveRel(is, empA, empGroup);
        saveRel(is, empB, empGroup);
        saveRel(is, managea, manageGroup);
        saveRel(is, manageb, manageGroup);
        saveRel(is, dira, dirGroup);
    }

    org.activiti.engine.identity.User saveUser(IdentityService is, String id) {
        org.activiti.engine.identity.User u = is.newUser(id);
        u.setPassword("123456");
        is.saveUser(u);
        return u;
    }

    Group saveGroup(IdentityService is, String id, String name) {
        Group g = is.newGroup(id);
        g.setName(name);
        is.saveGroup(g);
        return g;
    }

    void saveRel(IdentityService is, org.activiti.engine.identity.User u, Group g) {
        is.createMembership(u.getId(), g.getId());
    }


    //    @PostConstruct
    public void init() {
        RepositoryService rs = engine.getRepositoryService();
        Deployment dep = rs.createDeployment()
                //                .addClasspathResource("processes/test001.bpmn20.xml")
                .addClasspathResource("processes/OperationAndMaintenanceProcess.bpmn.xml")
                .deploy();
        ProcessDefinition pd = rs.createProcessDefinitionQuery()
                .deploymentId(dep.getId())
                .singleResult();
        //        rs.addCandidateStarterGroup(pd.getId(), "empGroup");
    }

    @GetMapping("/exit")
    public String exit(HttpSession session) {
        session.removeAttribute("userName");
        return "login";
    }
}
