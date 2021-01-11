package org.springframework.custom_test;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/5 9:15
 * @Version: 1.0
 */
@Aspect
public class AspectJTest {
	@Pointcut("execution(* org.springframework.custom_test.*.*(..))")
	public void test() {

	}

	@Before("test()")
	public void beforeTest() {
		System.out.println("beforeTest()");
	}

	@After("test()")
	public void afterTest() {
		System.out.println("afterTest()");
	}

	@Around("test()")
	public Object arountTest(ProceedingJoinPoint point) {
		System.out.println("before1");
		Object o = null;
		try {
			o = point.proceed();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		System.out.println("after1");
		return o;
	}

}
