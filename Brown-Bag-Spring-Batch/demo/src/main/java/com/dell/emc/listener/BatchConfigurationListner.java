package com.dell.emc.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchConfigurationListner implements JobExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfigurationListner.class);

	@Override
	public void beforeJob(JobExecution jobExecution) {

		logger.info("Need to perform some clean up activities before running the job");

	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		
		long instanceId = jobExecution.getJobInstance().getInstanceId();
		
		logger.info("Ran the job with Instance Id : "+ instanceId);

		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			// job success
			logger.info("Post job completion activities like sending email, changing extention or ftp etc");

		} else if (jobExecution.getStatus() == BatchStatus.FAILED) {
			// job failure
		}

	}

}
