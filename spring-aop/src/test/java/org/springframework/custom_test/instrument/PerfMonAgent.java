package org.springframework.custom_test.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

/**
 * @Description: java类作用描述
 * @author: sxk
 * @CreateDate: 2021/1/7 18:30
 * @Version: 1.0
 */
public class PerfMonAgent {

	static private Instrumentation inst = null;

	/**
	 * This method  is called before the application's main-method is called,
	 * when the agent is specified to the java VM
	 * <p>
	 * 在将代理指定给Java VM时，将在调用应用程序的主方法之前调用此方法。
	 *
	 * @param agentArgs
	 * @param _inst
	 */
	public static void premain(String agentArgs, Instrumentation _inst) {
		// PerfMonAgent.premain（）被调用
		System.out.println("PerfMonAgent.premain() was called.");
		// Initialize the static variables we use to track information
		// 初始化用于跟踪信息的静态变量
		inst = _inst;
		// Set up the class-file transformer
		// 设置类文件转换器
		ClassFileTransformer transformer = new PerfMonXformer();
		// 将PerfMonXformer实例添加到JVM
		System.out.println("Adding a PerfMonXformer instance to the JVM");
		inst.addTransformer(transformer);
	}
}
