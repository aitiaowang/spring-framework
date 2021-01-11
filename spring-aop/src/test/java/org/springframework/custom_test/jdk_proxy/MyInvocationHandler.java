package org.springframework.custom_test.jdk_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/7 10:48
 * @Version: 1.0
 */
public class MyInvocationHandler implements InvocationHandler {

	//目标对象
	private Object target;

	/**
	 * 构造方法
	 *
	 * @param target 目标对象
	 */
	public MyInvocationHandler(Object target) {
		super();
		this.target = target;
	}


	/**
	 * 执行目标对象的方法
	 * <p>
	 * 处理代理实例上的方法调用并返回结果。在与之关联的代理实例上调用方法时，将在调用处理程序上调用该方法。
	 *
	 * @param proxy  在其上调用方法的代理实例
	 * @param method 与代理实例上调用的接口方法相对应的{@code Method}实例。
	 *               {@code方法}对象的声明类将是在其中声明该方法的接口，该接口可能是代理类通过其继承该方法的代理接口的超接口。
	 * @param args   一个对象数组，其中包含在代理实例的方法调用中传递的参数的值；如果接口方法不接受任何参数或为{@code null}。
	 *               原始类型的参数包装在适当的原始包装器类的实例中，例如{@code java.lang.Integer}或{@code java.lang.Boolean}。
	 * @return 从代理实例上的方法调用返回的值。如果接口方法的声明的返回类型是原始类型，则此方法返回的值必须是相应的原始包装器类的实例。
	 * 否则，它必须是可分配给*声明的返回类型的类型。如果此方法返回的值为{@code null}，并且接口方法的返回类型为原语，
	 * 则在代理实例上的方法调用将抛出{@code NullPointerException}。如果此方法返回的值与上述接口方法的声明的返回类型不兼容，
	 * 则方法将在代理实例上调用{@code ClassCastException}。
	 * @see java.lang.reflect.UndeclaredThrowableException
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 在目标对象的方法执行之前简单的打印一下
		System.out.println("------------------before-------------------");
		// 执行目标对象的方法
		Object invoke = method.invoke(target, args);
		// 在目标对象的方法执行之后简单的打印一下
		System.out.println("------------------after--------------------");
		return invoke;
	}

	/**
	 * 获取目标对象的代理对象
	 *
	 * @return 代理对象
	 */
	public Object getProxy() {
		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), target.getClass().getInterfaces(), this);
	}
}
