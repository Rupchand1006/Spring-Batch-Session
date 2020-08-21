package com.dell.emc.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class FlowersSelectionStepExecutionListener implements StepExecutionListener {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfigurationListner.class);

	@Override
	public void beforeStep(StepExecution stepExecution) {
		logger.info("Executing before step logic");

	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		
		logger.info("Executing after step logic");
		
		String flowerType = stepExecution.getJobParameters().getString("type");
		
		return flowerType.equalsIgnoreCase("roses") ? new ExitStatus("TRIM_REQUIRED")
				: new ExitStatus("NO_TRIM_REQUIRED");
	}

}
