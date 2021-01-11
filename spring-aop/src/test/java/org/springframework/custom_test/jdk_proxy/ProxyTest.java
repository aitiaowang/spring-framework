package org.springframework.custom_test.jdk_proxy;

import org.junit.Test;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/7 11:13
 * @Version: 1.0
 */
public class ProxyTest {

	/**
	 * 使用JDK代理的方式
	 * <p>
	 * 1.构造函数，将代理的对象传入
	 * 2.invoke方法，此方法中实现了AOP增强的所有逻辑
	 * 3.getProxy方法，此方法千篇一律，但是必不可少
	 *
	 * @param null
	 * @return
	 * @author sxk
	 * @date 2021/1/7 11:41
	 */
	@Test
	public void testProxy() {
		// 实例化目标对象
		UserService userService = new UserServiceImpl();
		//实例化InvocationHandler
		MyInvocationHandler invocationHandler = new MyInvocationHandler(userService);
		// 根据目标对象生成代理对象
		UserService proxy = (UserService) invocationHandler.getProxy();
		//调用代理对象的方法
		proxy.add();
	}
}
