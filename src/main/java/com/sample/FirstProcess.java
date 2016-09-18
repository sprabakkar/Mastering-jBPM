package com.sample;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.persistence.jpa.KieStoreServices;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class FirstProcess {
	public static final String PU_NAME = "localjbpm-persistenceunit";
	public static final String DATASOURCE_NAME = "java:jboss/datasources/jbpmDS";
	private static final String PROPERTIES_FILE = "/localJBPM.properties";

	private static Properties properties;

	public static KieSession ksession = null;
	public static TaskService taskService = null;
	public static RuntimeManager manager = null;
	public static RuntimeEngine engine = null;

	public static void main(String[] args) {
		// Creating the knowledge base
		dssource();
		KieServices ks = KieServices.Factory.get();
		KieStoreServices storeservice = ks.getStoreServices();
		KieContainer kContainer = ks.getKieClasspathContainer();
		KieBase kbase = kContainer.getKieBase("kbase");

		// Creating a runtime manager
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		Environment env = KnowledgeBaseFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		
		// current Bitronix transaction manager
		env.set(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager());
		
		RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder().entityManagerFactory(emf).knowledgeBase(kbase);
			
		RuntimeManager runtimeManager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get(),"com.sample:jbpm-example:1.0.0");
		RuntimeEngine engine = runtimeManager.getRuntimeEngine(EmptyContext.get());
		KieSession ksession = engine.getKieSession();
		TaskService taskService = engine.getTaskService();
		
		
		//ksession = storeservice.newKieSession(kbase, null, env);
		//long id = ksession.getIdentifier();
		//ksession.dispose();
		//KieSession loadedsession = storeservice.loadKieSession(id, kbase,null,env);
		//ksession = loadedsession;
		
		
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", "krisv");
        
     // start the transaction
        UserTransaction ut = null;
        synchronized (ksession) {
		try {
			ut = InitialContext.doLookup("java:comp/UserTransaction");
			ut.begin();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
		ksession.startProcess("com.sample.FirstProcess");
		
	    // let john execute Task 1
	    List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("krisv", "en-UK");
	    //TaskSummary task = list.get(0);
	    for(TaskSummary task: list){
		    System.out.println("Prabakar executing task " + task.getName() + "(" + task.getId() + ": " + task.getDescription() + ")");
		    System.out.println("krisv is executing task " + task.getName());
		    
	        // Claim Task
	        //taskService.claim(task.getId(), "krisv");
		    
	        // Start Task
		    taskService.start(task.getId(), "krisv");
		    
		    //Map<String, Object> params = new HashMap<>();
		    //params.put("output", true);

		    // Complete Task
		    taskService.complete(task.getId(), "krisv", null);
	    }
	   

	    try {
			ut.commit();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (javax.transaction.RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    
        }
        runtimeManager.disposeRuntimeEngine(engine);  
        
        System.exit(0);  

	}

	public static void dssource() {
		PoolingDataSource ds = new PoolingDataSource();
		ds.setUniqueName("jdbc/jbpm-ds");
		ds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
		ds.setMaxPoolSize(3);
		ds.setAllowLocalTransactions(true);
		ds.getDriverProperties().put("user", "root");
		ds.getDriverProperties().put("password", "password");
		ds.getDriverProperties().put("url", "jdbc:mysql://localhost:3306/jbpm");
		ds.getDriverProperties().put("driverClassName", "com.mysql.jdbc.Driver");
		ds.init();
	}
	

/**
 * 
 *            void start( long taskId, String userId );
              void stop( long taskId, String userId );
              void release( long taskId, String userId );
              void suspend( long taskId, String userId );
              void resume( long taskId, String userId );
              void skip( long taskId, String userId );
              void delegate(long taskId, String userId, String targetUserId);
              void complete( long taskId, String userId, Map<String, Object> results );
              
 			jbpm-persistence-jpa (org.jbpm)
	        drools-persistence-jpa (org.drools)
	        persistence-api (javax.persistence)
	        hibernate-entitymanager (org.hibernate)
	        hibernate-annotations (org.hibernate)
	        hibernate-commons-annotations (org.hibernate)
	        hibernate-core (org.hibernate)
	        commons-collections (commons-collections)
	        dom4j (dom4j)
	        jta (javax.transaction)
	        btm (org.codehaus.btm)
	        javassist (javassist)
	        slf4j-api (org.slf4j)
	        slf4j-jdk14 (org.slf4j)
	        h2 (com.h2database)
	        jbpm-test (org.jbpm) for testing only, do not include it in the actual application
**/

/*	private static RuntimeManager createRuntimeManager(KieBase kbase) {
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

	public static PoolingDataSource setupDataSource() {

		PoolingDataSource pds = new PoolingDataSource();
		pds.setUniqueName(properties.getProperty("persistence.datasource.name",
				DATASOURCE_NAME));
		pds.setClassName(LrcXADataSource.class.getName());
		pds.setMaxPoolSize(5);
		pds.setAllowLocalTransactions(true);
		pds.getDriverProperties().put("user",
				properties.getProperty("persistence.datasource.user", "root"));
		pds.getDriverProperties().put(
				"password",
				properties.getProperty("persistence.datasource.password",
						"password"));
		pds.getDriverProperties().put(
				"url",
				properties.getProperty("persistence.datasource.url",
						"jdbc:mysql://localhost:3306/jbpm"));
		pds.getDriverProperties().put(
				"driverClassName",
				properties.getProperty(
						"persistence.datasource.driverClassName",
						"com.mysql.jdbc.jdbc2.optional.MysqlXADataSource"));
		pds.init();
		return pds;
	}*/
}
