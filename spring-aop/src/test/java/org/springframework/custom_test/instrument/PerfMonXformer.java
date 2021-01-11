package org.springframework.custom_test.instrument;

import com.sun.corba.se.impl.encoding.CodeSetConversion;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * java在1.5以后引入了{@link java.lang.instrument}，可以由此实现一个java agent (java 代理)，通过此agent来修改类的字节码即改变一个类；
 * 类似于一种更低级，更松耦合的AOP,可以从底层来改变一个类的行为
 *
 * @Description: 测试 instrument(仪器类)
 * @author: sxk
 * @CreateDate: 2021/1/7 17:59
 * @Version: 1.0
 */
public class PerfMonXformer implements ClassFileTransformer {

	/**
	 * 此方法的实现可以转换提供的类文件并返回新的替换类文件。
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] transformer = null;
		System.out.println("TransForming ===>" + className);
		ClassPool pool = ClassPool.getDefault();
		CtClass cl = null;
		try {
			//从给定的类文件创建一个新的类（或接口）。 如果已经存在同名的类，则新类覆盖先前的类。
			cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
			if (cl.isInterface() == false) {
				// 获取在类中声明的所有构造函数和方法。
				CtBehavior[] methods = cl.getDeclaredBehaviors();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].isEmpty() == false) {
						// 修改method字节码
						//doMethod(methods[i]);
					}
				}
				transformer = cl.toBytecode();
			}
		} catch (Exception e) {
			System.out.println("Could not instrument " + className + " , exception ==> " + e.getMessage());
		} finally {
			if (cl != null) {
				// 从ClassPool中删除此CtClass对象。调用此方法后，不能在已删除的CtClass对象上调用任何方法。
				cl.detach();
			}
		}
		return transformer;
	}

	private void doMethod(CtBehavior method) {
		try {
			method.insertBefore("long stime = System.nanoTime();");
			method.insertAfter("System.out.println(\"leave \" + method.getName() + \" and time \" + (System.nanoTime() - stime));");
		} catch (CannotCompileException e) {
			e.printStackTrace();
		}
	}
}
