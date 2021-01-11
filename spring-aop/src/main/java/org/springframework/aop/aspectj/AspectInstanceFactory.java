/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * Interface implemented to provide an instance of an AspectJ aspect.
 * Decouples from Spring's bean factory.
 * <p>
 * 实现以提供AspectJ切面的实例的接口。 与Spring的bean工厂分离。
 *
 * <p>Extends the {@link org.springframework.core.Ordered} interface
 * to express an order value for the underlying aspect in a chain.
 * <p>
 * AOP底层，就是采用【动态代理】模式实现的。采用了两种代理：JDK动态代理和CGLIB动态代理。
 * <p>
 * 基本术语（一些名词）：
 * （1）切面(Aspect)
 * 切面泛指[*交叉业务逻辑*]。事务处理和日志处理可以理解为切面。常用的切面有通知(Advice)与顾问(Advisor)。实际就是对主业务逻辑的一种增强。
 * <p>
 * (2)织入（Weaving）
 * 织入是指将切面代码插入到目标对象的过程。代理的invoke方法完成的工作，可以称为织入。
 * <p>
 * （3） 连接点(JoinPoint)
 * 连接点是指可以被切面织入的方法。通常业务接口的方法均为连接点
 * <p>
 * （4）切入点(PointCut)
 * 切入点指切面具体织入的方法
 * 注意：被标记为final的方法是不能作为连接点与切入点的。因为最终的是不能被修改的，不能被增强的。
 * <p>
 * (5)目标对象（Target）
 * 目标对象指将要被增强的对象。即包含主业务逻辑的类的对象。
 * <p>
 * （6）通知（Advice）
 * 通知是切面的一种实现，可以完成简单的织入功能。通知定义了增强代码切入到目标代码的时间点，是目标方法执行之前执行，还是执行之后执行等。切入点定义切入的位置，通知定义切入的时间。
 * <p>
 * （7）顾问(Advisor)
 * 顾问是切面的另一种实现，能够将通知以更为复杂的方式织入到目标对象中，是将通知包装为更复杂切面的装配器。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @since 2.0
 */
public interface AspectInstanceFactory extends Ordered {

	/**
	 * Create an instance of this factory's aspect.
	 *
	 * @return the aspect instance (never {@code null})
	 */
	Object getAspectInstance();

	/**
	 * Expose the aspect class loader that this factory uses.
	 *
	 * @return the aspect class loader (or {@code null} for the bootstrap loader)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	@Nullable
	ClassLoader getAspectClassLoader();

}
