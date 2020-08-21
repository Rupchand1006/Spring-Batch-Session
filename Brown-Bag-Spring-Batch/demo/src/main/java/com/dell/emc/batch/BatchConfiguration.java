package com.dell.emc.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dell.emc.decider.DeliveryDecider;
import com.dell.emc.decider.ReceiptDecider;
import com.dell.emc.listener.BatchConfigurationListner;
import com.dell.emc.listener.FlowersSelectionStepExecutionListener;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	BatchConfigurationListner listner;

	@Bean
	public JobExecutionDecider decider() {
		return new DeliveryDecider();
	}

	@Bean
	public JobExecutionDecider receiptDecider() {
		return new ReceiptDecider();
	}

	@Bean
	@Qualifier("packageItemStep")
	public Step packageItemStep() {
		return this.stepBuilderFactory.get("packageItemStep").tasklet(new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

				// First Demo, uncomment java -jar -Dspring.profiles.active=QA
				// target\demo-0.0.1-SNAPSHOT.jar
				// logger.info("The item has been packaged.");

				// Second Run, With Job parameters java -jar -Dspring.profiles.active=QA
				// target\demo-0.0.1-SNAPSHOT.jar item="books" run.date="2020-08-17"

				String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
				String date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();

				logger.info(String.format("The %s has been packaged on %s.", item, date));

				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step driveToAddressStep() {
		return this.stepBuilderFactory.get("driveToAddressStep").tasklet(new Tasklet() {

			// Restartable and Conditional Flow Test
			boolean GOT_LOST = false;

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

				if (GOT_LOST) {
					throw new RuntimeException("Got lost driving to the address");
				}

				logger.info("Successfully arrived at the address.");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step givePackageToCustomerStep() {
		return this.stepBuilderFactory.get("givePackageToCustomer").tasklet(new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Given the package to the customer.");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step storePackageStep() {
		return this.stepBuilderFactory.get("storePackageStep").tasklet(new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Storing the package while the customer address is located.");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step leaveAtDoorStep() {
		return this.stepBuilderFactory.get("leaveAtDoorStep").tasklet(new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Leaving the package at the door.");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step thankCustomerStep() {
		return this.stepBuilderFactory.get("thankCustomerStep").tasklet(new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Thanking the customer.");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step refundStep() {
		return this.stepBuilderFactory.get("refundStep").tasklet(new Tasklet() {

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Refunding customer money.");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	@Qualifier("deliverPackageJob")
	public Job deliverPackageJob() {
		
		
		/*
		  return
		  this.jobBuilderFactory.get("deliverPackageJob").listener(listner).start(
		  packageItemStep()) .build();
		 */
		  
		  
		/*return this.jobBuilderFactory.get("deliverPackageJob").listener(listner).start(packageItemStep())

				.next(driveToAddressStep()).next(givePackageToCustomerStep())
				.next(driveToAddressStep()).on("FAILED").to(storePackageStep()).from(driveToAddressStep()).on("*")
				.to(givePackageToCustomerStep()).end()
				.build();*/
		
		return this.jobBuilderFactory.get("deliverPackageJob").listener(listner)
					.start(packageItemStep())
					.next(driveToAddressStep())
						.on("FAILED").to(storePackageStep())
					.from(driveToAddressStep())
						.on("*").to(decider())
							.on("PRESENT").to(givePackageToCustomerStep())
								.next(receiptDecider()).on("CORRECT").to(thankCustomerStep())
								.from(receiptDecider()).on("INCORRECT").to(refundStep())
						.from(decider())
							.on("NOT_PRESENT").to(leaveAtDoorStep())
					.end()
					.build();
		
		
		/*
		 * 	return this.jobBuilderFactory.get("deliverPackageJob")
				.start(packageItemStep())
				.next(driveToAddressStep())
					.on("FAILED").fail()
				.from(driveToAddressStep())
					.on("*").to(decider())
						.on("PRESENT").to(givePackageToCustomerStep())
							.next(receiptDecider()).on("CORRECT").to(thankCustomerStep())
							.from(receiptDecider()).on("INCORRECT").to(refundStep())
					.from(decider())
						.on("NOT_PRESENT").to(leaveAtDoorStep())
				.end()
				.build();
		 */

	}
	
	// Another Batch job
	@Bean
	public StepExecutionListener selectFlowerListener() {
		return new FlowersSelectionStepExecutionListener();
	}
	
	 @Bean
	    public Job prepareFlowers() {
	        return this.jobBuilderFactory.get("prepareFlowersJob")
	        		.start(selectFlowersStep())
	        			.on("TRIM_REQUIRED").to(removeThornsStep()).next(arrangeFlowersStep())
	        		.from(selectFlowersStep())
	        			.on("NO_TRIM_REQUIRED").to(arrangeFlowersStep())
	        		.from(arrangeFlowersStep()).on("*").to(deliveryFlow())
	        		.end()
	        		.build();
	    }
	 
	 @Bean
	    public Step selectFlowersStep() {
	        return this.stepBuilderFactory.get("selectFlowersStep").tasklet(new Tasklet() {

	            @Override
	            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
	                System.out.println("Gathering flowers for order.");
	                return RepeatStatus.FINISHED; 
	            }
	            
	        }).listener(selectFlowerListener()).build();
	    }

	    @Bean
	    public Step removeThornsStep() {
	        return this.stepBuilderFactory.get("removeThornsStep").tasklet(new Tasklet() {

	            @Override
	            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
	                System.out.println("Remove thorns from roses.");
	                return RepeatStatus.FINISHED; 
	            }
	            
	        }).build();
	    }
	    
	    @Bean
	    public Step arrangeFlowersStep() {
	        return this.stepBuilderFactory.get("arrangeFlowersStep").tasklet(new Tasklet() {

	            @Override
	            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
	                System.out.println("Arranging flowers for order.");
	                return RepeatStatus.FINISHED; 
	            }
	            
	        }).build();
	    }

		
	    @Bean
		public Flow deliveryFlow() {
			return new FlowBuilder<SimpleFlow>("deliveryFlow").start(driveToAddressStep())
						.on("FAILED").fail()
					.from(driveToAddressStep())
						.on("*").to(decider())
							.on("PRESENT").to(givePackageToCustomerStep())
								.next(receiptDecider()).on("CORRECT").to(thankCustomerStep())
								.from(receiptDecider()).on("INCORRECT").to(refundStep())
						.from(decider())
							.on("NOT_PRESENT").to(leaveAtDoorStep()).build();

		}
}
