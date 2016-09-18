package com.sample;

import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.runtime.manager.impl.task.SynchronizedTaskService;
import org.jbpm.test.JBPMHelper;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;

public class ProcessMain {

	public static final String PU_NAME = "localjbpm-persistenceunit";
	public static final String DATASOURCE_NAME = "jdbc/localjbpm-ds";
	private static final String PROPERTIES_FILE = "/localJBPM.properties";

	private static Properties properties;

	public static KieSession ksession = null;
	public static TaskService taskService = null;
	public static RuntimeManager manager = null;
	public static RuntimeEngine engine = null;

	public static void initWf() {
		KieServices ks = KieServices.Factory.get();
		KieContainer kContainer = ks.getKieClasspathContainer();
		KieBase kbase = kContainer.getKieBase();

		manager = createRuntimeManager(kbase);
		engine = manager.getRuntimeEngine(null);
		ksession = engine.getKieSession();
		taskService = engine.getTaskService();
		// User user = new UserImpl("nino");
		// SynchronizedTaskService synchtaskservice = (SynchronizedTaskService)
		// taskService;
		//
		// try {
		// synchtaskservice.addUser(user);
		// } catch (Exception exc) {
		// System.err.println(exc.getMessage());
		// }

	}

	public static void closeWF() {
		manager.disposeRuntimeEngine(engine);
	}

	public static void init() {
		getProperties();
		initWf();
		long processid = startProcess();
		doTasks(processid);
		closeWF();
		System.exit(0);
	}

	private static void getFirstTaskAndComplete(String username) {
		List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner(
				username, "en-UK");
		TaskSummary task = list.get(0);
		System.out.println(username + " is executing task " + task.getName());
		taskService.start(task.getId(), username);
		taskService.complete(task.getId(), username, null);
	}

	private static void handleOrder(String username) {
		getFirstTaskAndComplete(username);
	}

	private static void assignOrder(String username) {
		getFirstTaskAndComplete(username);
	}

	private static void assignDelivery(String username) {
		getFirstTaskAndComplete(username);
	}

	private static void makePizza(String username) {
		getFirstTaskAndComplete(username);
	}

	private static void deliver(String username) {
		getFirstTaskAndComplete(username);
	}

	private static void doTasks(long processid) {

		SynchronizedTaskService synchtaskservice = (SynchronizedTaskService) taskService;
		handleOrder("nino");
		assignOrder("maria");
		makePizza("luigi");
		assignDelivery("maria");
		deliver("salvatore");
	}

	private static RuntimeManager createRuntimeManager(KieBase kbase) {
		setupDataSource();
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory(properties.getProperty(
						"persistence.persistenceunit.name", PU_NAME));

		RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory
				.get().newDefaultBuilder().persistence(true)
				.entityManagerFactory(emf).knowledgeBase(kbase)
				.userGroupCallback(new MyUserCallback());

		return RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(
				builder.get(), "com.sample");
	}

	private static Properties getProperties() {
		properties = new Properties();
		try {
			properties.load(JBPMHelper.class
					.getResourceAsStream(PROPERTIES_FILE));
		} catch (Throwable t) {
			// do nothing, use defaults
		}
		return properties;

	}

	public static long startProcess() {

		org.kie.api.runtime.process.ProcessInstance pi = ksession
				.startProcess("com.sample.FirstProcess");

		System.out.println(pi.getId());
		return pi.getId();
	}

	public static PoolingDataSource setupDataSource() {

		PoolingDataSource pds = new PoolingDataSource();
		pds.setUniqueName(properties.getProperty("persistence.datasource.name",	DATASOURCE_NAME));
		pds.setClassName(LrcXADataSource.class.getName());
		pds.setMaxPoolSize(5);
		pds.setAllowLocalTransactions(true);
		pds.getDriverProperties().put("user",
				properties.getProperty("persistence.datasource.user", "root"));
		pds.getDriverProperties().put("password",
				properties.getProperty("persistence.datasource.password", "password"));
		pds.getDriverProperties().put(
				"url",
				properties.getProperty("persistence.datasource.url", "jdbc:mysql://localhost:3306/jbpm"));
		pds.getDriverProperties().put(
				"driverClassName",
				properties.getProperty(
						"persistence.datasource.driverClassName",
						"com.mysql.jdbc.jdbc2.optional.MysqlXADataSource"));
		pds.init();
		return pds;
	}

}