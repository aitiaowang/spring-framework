package org.springframework.custom_test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.aspectj.AnnotationDrivenBeanDefinitionParserTests;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/5 9:35
 * @Version: 1.0
 */
public class main {

	private static final String PATH = "/org/springframework/custom_test/";
	private static final String RESOURCE_CONTEXT = PATH + "aspectTest.xml";

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext(RESOURCE_CONTEXT, TestBean.class);
		TestBean test = (TestBean) context.getBean("test");
		test.test();
	}
}
