package com.dell.emc.decider;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class DeliveryDecider implements JobExecutionDecider {

	private static final Logger logger = LoggerFactory.getLogger(DeliveryDecider.class);

	@Override
	public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {

		String result = LocalDateTime.now().getHour() < 12 ? "PRESENT" : "NOT_PRESENT";
		logger.info("Decider result is:  " + result);
		return new FlowExecutionStatus(result);
	}

}
