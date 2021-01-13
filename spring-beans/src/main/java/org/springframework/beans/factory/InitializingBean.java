/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * Interface to be implemented by beans that need to react once all their properties
 * have been set by a {@link BeanFactory}: e.g. to perform custom initialization,
 * or merely to check that all mandatory properties have been set.
 * <p>
 * 由{@link BeanFactory}设置完所有属性后需要进行响应的bean所实现的接口：例如执行自定义初始化，或仅检查所有必需属性是否已设置。
 * <p>
 * 实现此接口的bean会在初始化时调用其{@link #afterPropertiesSet()}来进行bean的初始化逻辑
 * <p>
 * 实现{@link InitializingBean#afterPropertiesSet()}接口的bean会在初始化时调用其{@link #afterPropertiesSet()}来进行bean的初始化逻辑，
 * {@link #afterPropertiesSet()}在调用完{@link org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(Object, String)}
 * 方法后调用
 *
 * <p>An alternative to implementing {@code InitializingBean} is specifying a custom
 * init method, for example in an XML bean definition. For a list of all bean
 * lifecycle methods, see the {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableBean
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getInitMethodName()
 */
public interface InitializingBean {

	/**
	 * Invoked by the containing {@code BeanFactory} after it has set all bean properties
	 * and satisfied {@link BeanFactoryAware}, {@code ApplicationContextAware} etc.
	 * <p>
	 * 由包含的{@code BeanFactory}设置了所有bean属性并满足{@link BeanFactoryAware}，{@code ApplicationContextAware}等之后调用。
	 *
	 * <p>This method allows the bean instance to perform validation of its overall
	 * configuration and final initialization when all bean properties have been set.
	 * <p>
	 * 设置所有bean属性后，此方法允许bean实例执行其整体*配置的验证和最终初始化。
	 *
	 * @throws Exception in the event of misconfiguration (such as failure to set an
	 *                   essential property) or if initialization fails for any other reason
	 */
	void afterPropertiesSet() throws Exception;

}
