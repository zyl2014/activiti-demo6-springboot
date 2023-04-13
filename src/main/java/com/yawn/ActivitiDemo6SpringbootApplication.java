package com.yawn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.activiti.spring.boot.SecurityAutoConfiguration;

/**
 * @author yawn
 */
@SpringBootApplication(exclude= SecurityAutoConfiguration.class)
public class ActivitiDemo6SpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(ActivitiDemo6SpringbootApplication.class, args);
	}
}
