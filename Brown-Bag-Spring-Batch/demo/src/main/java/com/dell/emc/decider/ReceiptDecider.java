package com.dell.emc.decider;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class ReceiptDecider implements JobExecutionDecider {

	private static final Logger logger = LoggerFactory.getLogger(DeliveryDecider.class);

	@Override
	public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {

		String exitCode = new Random().nextFloat() < .70f ? "CORRECT" : "INCORRECT";
		logger.info("The item delivered is: " + exitCode);
		return new FlowExecutionStatus(exitCode);
	}

}
