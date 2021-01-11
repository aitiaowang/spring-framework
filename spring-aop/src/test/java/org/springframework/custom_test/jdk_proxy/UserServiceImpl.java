package org.springframework.custom_test.jdk_proxy;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/7 10:46
 * @Version: 1.0
 */
public class UserServiceImpl implements UserService {

	@Override
	public void add() {
		System.out.println("-------------------add------------------");
	}
}
