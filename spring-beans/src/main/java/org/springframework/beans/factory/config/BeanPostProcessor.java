/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * Factory hook that allows for custom modification of new bean instances,
 * e.g. checking for marker interfaces or wrapping them with proxies.
 * <p>
 * 工厂钩子允许自定义修改新的bean实例，例如检查标记界面或使用代理包装它们。
 *
 * <p>ApplicationContexts can autodetect BeanPostProcessor beans in their
 * bean definitions and apply them to any beans subsequently created.
 * Plain bean factories allow for programmatic registration of post-processors,
 * applying to all beans created through this factory.
 * <p>
 * ApplicationContexts可以在它们的bean定义中自动检测BeanPostProcessor(bean后处理器) beans，并将它们应用于随后创建的任何bean。
 * 普通bean工厂允许以编程方式注册后处理器，适用于通过该工厂创建的所有bean。
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 * <p>
 * 通常，通过标记器接口或类似方法填充bean的后处理器将实现{@link #postProcessBeforeInitialization}，
 * 而用代理程序包装bean的后处理器通常将实现{@link #postProcessAfterInitialization}。
 *
 * @author Juergen Hoeller
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 * @since 10.10.2003
 */
// bean后处理器
public interface BeanPostProcessor {

	/**
	 * 初始化前的后期处理
	 * <p>
	 * Apply this BeanPostProcessor to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 *
	 * <p>
	 * 在任何bean初始化回调之前（例如InitializingBean的{@code afterPropertiesSet}或自定义的init-method），
	 * 将此BeanPostProcessor应用于给定的新bean实例<i>之前 </ i>。该bean将已经用属性值填充。
	 * 返回的bean实例可能是原始包装。
	 *
	 * <p>The default implementation returns the given {@code bean} as-is.
	 *
	 * <p>默认实现按原样返回给定的{@code bean}。
	 *
	 * @param bean     the new bean instance   新的bean实例
	 * @param beanName the name of the bean    bean的名称
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * 要使用的bean实例，是原始实例还是包装实例；如果{@code null}，则不会调用后续的BeanPostProcessor
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Apply this BeanPostProcessor to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
	 * or a custom init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * <p>
	 * 在任何bean初始化回调（例如InitializingBean的{@code afterPropertiesSet} 或自定义的init-method）之后，
	 * 将此BeanPostProcessor应用于给定的新bean实例<i> </ i>。该bean将已经用属性值填充。返回的bean实例可能是原始包装。
	 *
	 * <p>In case of a FactoryBean, this callback will be invoked for both the FactoryBean
	 * instance and the objects created by the FactoryBean (as of Spring 2.0). The
	 * post-processor can decide whether to apply to either the FactoryBean or created
	 * objects or both through corresponding {@code bean instanceof FactoryBean} checks.
	 * <p>
	 * 对于FactoryBean，将为FactoryBean实例和由FactoryBean创建的对象（从Spring 2.0开始）调用此回调。
	 * 后处理器可以通过相应的{@code bean instanceof FactoryBean}检查来决定是应用到FactoryBean还是创建的对象，还是两者都应用。
	 *
	 * <p>This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other BeanPostProcessor callbacks.
	 * <p>
	 * 与所有其他BeanPostProcessor回调相反，
	 * 此回调还将在{@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation}方法触发短路后被调用。
	 *
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * 默认实现按原样返回给定的{@code bean}。
	 *
	 * @param bean     the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
