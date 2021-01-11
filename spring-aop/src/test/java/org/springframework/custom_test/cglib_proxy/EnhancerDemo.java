package org.springframework.custom_test.cglib_proxy;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/7 15:20
 * @Version: 1.0
 */
public class EnhancerDemo {

	public static void main(String[] args) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(EnhancerDemo.class);
		enhancer.setCallback(new MethodInterceptorImpl());
		EnhancerDemo demo = (EnhancerDemo) enhancer.create();
		demo.test();
		System.out.println(demo);
	}

	public void test() {
		System.out.println("EnhancerDemo test()");
	}

	private static class MethodInterceptorImpl implements MethodInterceptor {

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
			System.out.println("Before invoke " + method);
			Object result = methodProxy.invokeSuper(o, objects);
			System.out.println("after invoke " + method);
			return result;
		}
	}
}
