package com.dell.emc.batch;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.dell.emc.batch.exception.OrderProcessingException;
import com.dell.emc.batch.item.mapper.OrderRowMapper;
import com.dell.emc.batch.item.reader.SimpleItemReader;
import com.dell.emc.batch.item.writer.DBItemWriter;
import com.dell.emc.batch.item.writer.FlatFileItemWriter;
import com.dell.emc.batch.item.writer.SimpleItemWriter;
import com.dell.emc.batch.item.writer.WriteIntoDB;
import com.dell.emc.batch.listner.BatchConfigurationListner;
import com.dell.emc.batch.listner.CustomRetryListener;
import com.dell.emc.batch.listner.CustomSkipListener;
import com.dell.emc.batch.processor.FreeShippingItemProcessor;
import com.dell.emc.batch.processor.SampleItemProcessor;
import com.dell.emc.batch.processor.TrackedOrderItemProcessor;
import com.dell.emc.batch.processor.TrackedOrderItemProcessorSkipFeature;
import com.dell.emc.batch.processor.TrackedShippingOrderItemProcessor;
import com.dell.emc.model.Order;
import com.dell.emc.model.OrderFieldSetMapper;
import com.dell.emc.model.TrackedOrder;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	public static String[] tokens = new String[] { "order_id", "first_name", "last_name", "email", "cost", "item_id",
			"item_name" };
	
	public static String[] names = new String[] { "orderId", "firstName", "lastName", "email", "cost", "itemId",
			"itemName", "shipDate" };
	
	public static String ORDER_SQL = "select order_id, first_name, last_name,  email, cost, item_id, item_name,ship_date from SHIPPED_ORDER order by order_id";
	
	public static String ORDER_TRACKER_SQL = "insert into "
			+ "TRACKED_ORDER(order_id, first_name, last_name, email, item_id, item_name, cost, ship_date, tracking_number, free_shipping)"
			+ " values(:orderId,:firstName,:lastName,:email,:itemId,:itemName,:cost,:shipDate,:trackingNumber, :freeShipping)";
	
	

	private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Bean
	public ItemReader<String> itemReader() {
		return new SimpleItemReader();
	}
	
	@Bean
	public ItemProcessor<String, String> itemProcessor() {
		return new SampleItemProcessor();
	}

	@Bean
	public ItemWriter<String> itemWriter() {
		return new SimpleItemWriter();
	}

	@Bean
	public ItemWriter<Order> flatFileItemWriter() {
		return new FlatFileItemWriter();
	}
	
	@Bean
	public ItemWriter<Order> dbItemWriter(){
		return new DBItemWriter();
	}
	
	@Bean
	public ItemWriter<Order> writeIntoDB(){
		return new WriteIntoDB();
	}
	
	
	@Autowired
	public DataSource dataSource;

	
	@Autowired
	private BatchConfigurationListner listner;
	
	
	
	// Steps

	@Bean
	public Step chunkBasedStep() {
		return this.stepBuilderFactory.get("chunkBasedStep").<String, String>chunk(2).reader(itemReader())
				.processor(itemProcessor())/*.faultTolerant().skip(Exception.class).skipLimit(10).retry(Exception.class).retryLimit(2)*/
				.writer(itemWriter()).build();
	}
	
	
	/*********************************************flatFileJob START ***********************************/

	@Bean
	public ItemReader<Order> flatFileItemReader() {
		FlatFileItemReader<Order> itemReader = new FlatFileItemReader<Order>();
		itemReader.setLinesToSkip(1); // don't want to read header
		
		itemReader.setResource(new FileSystemResource("D:/Git_CodeBase/Brown-Bag-Spring-Batch/demo-chunk/data/shipped_orders.csv"));

		DefaultLineMapper<Order> lineMapper = new DefaultLineMapper<Order>();
		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
		tokenizer.setNames(tokens);

		lineMapper.setLineTokenizer(tokenizer);

		lineMapper.setFieldSetMapper(new OrderFieldSetMapper());

		itemReader.setLineMapper(lineMapper);
		return itemReader;

	}
	
	@Bean
	public Step flatFileStep() {
		return this.stepBuilderFactory.get("flatFileStep").<Order, Order>chunk(10).reader(flatFileItemReader())
				.writer(flatFileItemWriter()).build();
	}
	
	/********************************************* flatFileJob END ***********************************/
	
	
	/******************************** jdbcCursorJob SingleThread  START *****************************/
	
	@Bean
	public ItemReader<Order> itemReaderForDB() {
		return new JdbcCursorItemReaderBuilder<Order>()
				.dataSource(dataSource)
				.name("jdbcCursorItemReader")
				.sql(ORDER_SQL)
				.rowMapper(new OrderRowMapper())
				.build();

	}
	
	@Bean
	public Step readDataFromDBStep() {
		return this.stepBuilderFactory.get("readDataFromDBStep").<Order, Order>chunk(10).reader(itemReaderForDB())
				.writer(dbItemWriter()).build();
	}

	/******************************** jdbcCursorJob  END *****************************/
	
	
	/** ********************************** jdbcPagingJob - MultiThread - START **********************************************/
	
	@Bean
	public PagingQueryProvider queryProvider() throws Exception {
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		
		factory.setSelectClause("select order_id, first_name, last_name, email, cost, item_id, item_name, ship_date");
		factory.setFromClause("from SHIPPED_ORDER");
		factory.setSortKey("order_id");
		factory.setDataSource(dataSource);
		return factory.getObject();
	}
	
	@Bean
	public ItemReader<Order> jdbcPagingItemReader() throws Exception {
		return new JdbcPagingItemReaderBuilder<Order>()
				.dataSource(dataSource)
				.name("jdbcPagingItemReader")
				.queryProvider(queryProvider())
				.rowMapper(new OrderRowMapper())
				.pageSize(10)
				.saveState(false) // MT ENABLE
				.build();

	 }
	
	
	@Bean
	public Step jdbcPagingStep() {
		try {
			return this.stepBuilderFactory.get("jdbcPagingStep").<Order, Order>chunk(10).reader(jdbcPagingItemReader())
					.writer(WriterIntoFile()).build();
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}
	
	
	@Bean
	public ItemWriter<Order> WriterIntoFile() {
		
		org.springframework.batch.item.file.FlatFileItemWriter<Order> itemWriter = new org.springframework.batch.item.file.FlatFileItemWriter<Order>();
		
		itemWriter.setResource(new FileSystemResource("D:/Git_CodeBase/Brown-Bag-Spring-Batch/demo-chunk/data/shipped_orders_output.csv"));
		
		DelimitedLineAggregator<Order> aggregator = new DelimitedLineAggregator<Order>();
		
		aggregator.setDelimiter(",");
		
		BeanWrapperFieldExtractor<Order> fieldExtractor = new BeanWrapperFieldExtractor<Order>();
		
		fieldExtractor.setNames(names);
		
		aggregator.setFieldExtractor(fieldExtractor);
		
		itemWriter.setLineAggregator(aggregator);
		
		return itemWriter;
	}
	
	/**********************************************jdbcPagingJob  END ***************************************************************/
	
	/****************************************************chunkBasedReadWriteDBJob START *********************************************/
	
	@Bean
	public Step chunkBasedReadWriteDBStep() {
		
		//Creates a step builder and initializes its job repository and transaction manager. Note that if the builder isuse to create a @Bean definition then the name of the step and the bean name might be different.
		
		try {
			return this.stepBuilderFactory.get("chunkBasedReadWriteDBStep").<Order, Order>chunk(10).reader(jdbcPagingItemReader())
					.writer(writeIntoDB()).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	/********************************************************** FILTERING ITEM PROCESSOR START *********************************************************/
	@Bean
	public Step validatorEmailStep() {
		try {
			return this.stepBuilderFactory.get("validatorEmailStep")
					.<Order, Order>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(orderValidatingItemProcessor())
					.writer(validatorEmailWriter())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Bean
	public ItemProcessor<Order, Order> orderValidatingItemProcessor() {
		BeanValidatingItemProcessor<Order> itemProcessor = new BeanValidatingItemProcessor<Order>();
		itemProcessor.setFilter(true);
		return itemProcessor;
	}
	
	@Bean
	public ItemWriter<Order> validatorEmailWriter() {
		return new JsonFileItemWriterBuilder<Order>()
				.jsonObjectMarshaller(new JacksonJsonObjectMarshaller<Order>())
				.resource(new FileSystemResource("D:/Git_CodeBase/Brown-Bag-Spring-Batch/demo-chunk/data/shipped_orders_output.json"))
				.name("jsonItemWriter")
				.build();
	}
	
	/********************************************************FILTERING ITEM PROCESSOR  END *****************************************************/
	
	/********************************************************TRANSFORM ITEM PROCESSOR START  *****************************************************/
	
	@Bean
	public Step orderTrackerStep() {
		try {
			return this.stepBuilderFactory.get("orderTrackerStep")
					.<Order, TrackedOrder>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(trackedOrderItemProcessor())
					.writer(orderTrackerWritr())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> trackedOrderItemProcessor() {
		return new TrackedOrderItemProcessor(); 
	}
	
	@Bean
	public ItemWriter<TrackedOrder> orderTrackerWritr() {
		return new JsonFileItemWriterBuilder<TrackedOrder>()
				.jsonObjectMarshaller(new JacksonJsonObjectMarshaller<TrackedOrder>())
				.resource(new FileSystemResource("D:/Git_CodeBase/Brown-Bag-Spring-Batch/demo-chunk/data/shipped_orders_tracker_output.json"))
				.name("jsonItemWriter")
				.build();
	}


	/********************************************************TRANSFORM ITEM PROCESSOR END  *****************************************************/
	
	/*******************************************************************************************************************************************/
	
	

	@Bean
	public Step compsoiteOrderProcessorStep() {
		try {
			return this.stepBuilderFactory.get("compsoiteOrderProcessorStep")
					.<Order, TrackedOrder>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(compositeItemProcessor())
					.writer(compositeItemWriter())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> compositeItemProcessor() {
		return new CompositeItemProcessorBuilder<Order,TrackedOrder>()
				.delegates(orderValidatingItemProcessor(), trackedOrderItemProcessor())
				.build();
	}
	
	@Bean
	public ItemWriter<TrackedOrder> compositeItemWriter() {
		return new JsonFileItemWriterBuilder<TrackedOrder>()
				.jsonObjectMarshaller(new JacksonJsonObjectMarshaller<TrackedOrder>())
				.resource(new FileSystemResource("D:/Git_CodeBase/Brown-Bag-Spring-Batch/demo-chunk/data/shipped_orders_filter_tracker_output.json"))
				.name("jsonItemWriter")
				.build();
	}

	
	
	/****************************************************************** TRANSFORM ITEM PROCESSOR END *************************************************************************/
	
	
	
	
	
	/****************************************************************** MULTI PROCESSOR START , SHPPING - TRACKING - Free ****************************************************/
	
	
	@Bean
	public Step orderWithMultiProcessorStep() {
		try {
			return this.stepBuilderFactory.get("orderWithMultiProcessorStep")
					.<Order, TrackedOrder>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(compositeItemMultiProcessor())
					.writer(orderWithMultiProcessorWriter())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> compositeItemMultiProcessor() {
		return new CompositeItemProcessorBuilder<Order,TrackedOrder>()
				.delegates(orderValidatingItemProcessor(), trackedOrderItemProcessor(), freeShippingItemProcessor())
				.build();
	}
	
	
	@Bean
	public ItemProcessor<TrackedOrder, TrackedOrder> freeShippingItemProcessor() {
		return new FreeShippingItemProcessor();
	}
	
	@Bean
	public ItemWriter<TrackedOrder> orderWithMultiProcessorWriter() {
		return new JsonFileItemWriterBuilder<TrackedOrder>()
				.jsonObjectMarshaller(new JacksonJsonObjectMarshaller<TrackedOrder>())
				.resource(new FileSystemResource("D:/Git_CodeBase/Brown-Bag-Spring-Batch/demo-chunk/data/shipped_orders_filter_tracker_fee_output.json"))
				.name("jsonItemWriter")
				.build();
	}

	
	/****************************************************************** MULTI PROCESSOR END , SHPPING - TRACKING - Free ****************************************************/
	
	/**************************** SKIP ********************/
	
	@Bean
	public Step orderWithMultiProcessorSkipJob() {
		try {
			return this.stepBuilderFactory.get("orderWithMultiProcessorSkipJob")
					.<Order, TrackedOrder>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(compositeItemMultiProcessorSkip())
					.faultTolerant()
					.skip(OrderProcessingException.class)
					.skipLimit(5) // default 0 , error - org.springframework.batch.core.step.skip.SkipLimitExceededException: Skip limit of '0' exceeded
					.listener(new CustomSkipListener())
					.writer(orderWithMultiProcessorWriter())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> compositeItemMultiProcessorSkip() {
		return new CompositeItemProcessorBuilder<Order,TrackedOrder>()
				.delegates(orderValidatingItemProcessor(), trackedOrderItemProcessorWithSkip(), freeShippingItemProcessor())
				.build();
	}
	
	
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> trackedOrderItemProcessorWithSkip() {
		return new TrackedOrderItemProcessorSkipFeature(); 
	}
	
	/************************************SKIP END***********************************************/
	
	/**************************************RETRY START *********************************************/
	@Bean
	public Step orderWithMultiProcessorRetryStep() {
		try {
			return this.stepBuilderFactory.get("orderWithMultiProcessorRetryStep")
					.<Order, TrackedOrder>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(compositeItemMultiProcessorRetry())
					.faultTolerant()
					.retry(OrderProcessingException.class)
					.retryLimit(3)
					.listener(new CustomRetryListener())
					.writer(orderWithMultiProcessorWriter())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> compositeItemMultiProcessorRetry() {
		return new CompositeItemProcessorBuilder<Order,TrackedOrder>()
				.delegates(orderValidatingItemProcessor(), trackedOrderItemProcessorWithSkip(), freeShippingItemProcessor())
				.build();
	}
	
	/**************************************RETRY END *********************************************/
	
	
	/****************************** MULTITHREADED *************************************************/
	
	@Bean
	public Step multiTheadedTrackingStep() {
		try {
			return this.stepBuilderFactory.get("multiTheadedTrackingStep")
					.<Order, TrackedOrder>chunk(10)
					.reader(jdbcPagingItemReader())
					.processor(multiTheadedTrackingProcessor())
					.faultTolerant()
					.retry(OrderProcessingException.class)
					.retryLimit(3)
					.listener(new CustomRetryListener())
					.writer(multiTheadedTrackingWriter())
					.taskExecutor(taskExecutor()) // MT
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(10);
		return executor;
	}
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> multiTheadedTrackingProcessor() {
		return new CompositeItemProcessorBuilder<Order,TrackedOrder>()
				.delegates(orderValidatingItemProcessor(), trackedShippingOrderItemProcessor(), freeShippingItemProcessor())
				.build();
	}
	
	
	@Bean
	public ItemProcessor<Order, TrackedOrder> trackedShippingOrderItemProcessor() {
		return new TrackedShippingOrderItemProcessor();
	}
	
	@Bean
	public ItemWriter<TrackedOrder> multiTheadedTrackingWriter() {
		return new JdbcBatchItemWriterBuilder<TrackedOrder>().dataSource(dataSource)
				.sql(ORDER_TRACKER_SQL).beanMapped().build();
	}
	

	@Bean
	public Job job() {
		
		//Creates a job builder and initializes its job repository. Note that if the builder is used to create a @Beandefinition then the name of the job and the bean name might be different.
		
		 /**1- Reading sample data and log into the writer**/
		//return this.jobBuilderFactory.get("chunkBasedJob").listener(listner).start(chunkBasedStep()).build();

		/**2 - Reading data from FlatFile and log the message in the writer**/
		// return this.jobBuilderFactory.get("flatFileJob").listener(listner).start(flatFileStep()).build();
		
		/*3 - Reading data from database and log the message in the writer**/
		//return this.jobBuilderFactory.get("jdbcCursorJob").listener(listner).start(readDataFromDBStep()).build();

		/**4th - Reading data from data base and write into CSV file**/
		// return this.jobBuilderFactory.get("jdbcPagingJob").listener(listner).start(jdbcPagingStep()).build();
		
		/**5th - reading data from Database and write into Database or refer payroll-process spring job**/
		//return this.jobBuilderFactory.get("chunkBasedReadWriteDBJob").listener(listner).start(chunkBasedReadWriteDBStep()).build();
		
		/**6th - Reading from database and apply filtering and write into JSON , email suffix with .gov**/
		// return this.jobBuilderFactory.get("validatorEmailJob").listener(listner).start(validatorEmailStep()).build();
		
		/**7th - Transformation - Item processor, one type to another type**/
		
		//return this.jobBuilderFactory.get("orderTrackerJob").listener(listner).start(orderTrackerStep()).build();
		
		/**8th- Composite Processor, Filtering+Transformation**/
		
		//return this.jobBuilderFactory.get("compsoiteOrderProcessorJob").listener(listner).start(compsoiteOrderProcessorStep()).build();
		
		
		/**8th- Composite Processor NESTED CHAIN , Filtering+Transformation**/
		return this.jobBuilderFactory.get("orderWithMultiProcessorJob").listener(listner).start(orderWithMultiProcessorStep()).build();
		
		
		/**9th- Composite Processor SKIP IMPL START**/
		//return this.jobBuilderFactory.get("orderWithMultiProcessorSkipJob").listener(listner).start(orderWithMultiProcessorSkipJob()).build();
		
		/**10th - RETRY *****************************/
		//return this.jobBuilderFactory.get("orderWithMultiProcessorRetryJob").listener(listner).start(orderWithMultiProcessorRetryStep()).build();
		
		/** 11 Multithread based, read from db and write into DB **/
		
		//return this.jobBuilderFactory.get("multiTheadedTrackingJob").listener(listner).start(multiTheadedTrackingStep()).build();
	
	}
	
	
	
	

	
	

}
