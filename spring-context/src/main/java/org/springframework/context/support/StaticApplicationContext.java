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

package org.springframework.context.support;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context.ApplicationContext} implementation
 * which supports programmatic registration of beans and messages,
 * rather than reading bean definitions from external configuration sources.
 * Mainly useful for testing.
 * {@link org.springframework.context.ApplicationContext}实现支持以编程方式注册bean和消息，
 * 而不是从外部配置源读取bean定义。 主要用于测试。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerPrototype
 * @see #registerBeanDefinition
 * @see #refresh
 */
public class StaticApplicationContext extends GenericApplicationContext {

	private final StaticMessageSource staticMessageSource;


	/**
	 * Create a new StaticApplicationContext.
	 * 创建一个新的StaticApplicationContext。
	 *
	 * @see #registerSingleton
	 * @see #registerPrototype
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public StaticApplicationContext() throws BeansException {
		this(null);
	}

	/**
	 * Create a new StaticApplicationContext with the given parent.
	 * 使用给定的父级创建一个新的StaticApplicationContext。
	 *
	 * @see #registerSingleton
	 * @see #registerPrototype
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public StaticApplicationContext(@Nullable ApplicationContext parent) throws BeansException {
		super(parent);

		// Initialize and register a StaticMessageSource.
		this.staticMessageSource = new StaticMessageSource();
		getBeanFactory().registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.staticMessageSource);
	}


	/**
	 * Overridden to turn it into a no-op, to be more lenient towards test cases.
	 * 重写以将其变为无操作，以更宽容地对待测试用例。
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * Return the internal StaticMessageSource used by this context.
	 * Can be used to register messages on it.
	 * 返回此上下文使用的内部StaticMessageSource。 可用于在其上注册消息。
	 *
	 * @see #addMessage
	 */
	public final StaticMessageSource getStaticMessageSource() {
		return this.staticMessageSource;
	}

	/**
	 * Register a singleton bean with the underlying bean factory.
	 * 向基础bean工厂注册一个singleton bean。
	 *
	 * <p>For more advanced needs, register with the underlying BeanFactory directly.
	 * 对于更高级的需求，请直接向基础BeanFactory注册。
	 *
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerSingleton(String name, Class<?> clazz) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(clazz);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * Register a singleton bean with the underlying bean factory.
	 * 向基础bean工厂注册一个singleton bean。
	 * <p>For more advanced needs, register with the underlying BeanFactory directly.
	 * 对于更高级的需求，请直接向基础BeanFactory注册。
	 *
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerSingleton(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(clazz);
		bd.setPropertyValues(pvs);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * Register a prototype bean with the underlying bean factory.
	 * 向基础bean工厂注册原型bean。
	 *
	 * <p>For more advanced needs, register with the underlying BeanFactory directly.
	 * 对于更高级的需求，请直接向基础BeanFactory注册。
	 *
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerPrototype(String name, Class<?> clazz) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		bd.setBeanClass(clazz);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * Register a prototype bean with the underlying bean factory.
	 * 向基础bean工厂注册原型bean。
	 *
	 * <p>For more advanced needs, register with the underlying BeanFactory directly.
	 * 对于更高级的需求，请直接向基础BeanFactory注册。
	 *
	 * @see #getDefaultListableBeanFactory
	 */
	public void registerPrototype(String name, Class<?> clazz, MutablePropertyValues pvs) throws BeansException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
		bd.setBeanClass(clazz);
		bd.setPropertyValues(pvs);
		getDefaultListableBeanFactory().registerBeanDefinition(name, bd);
	}

	/**
	 * Associate the given message with the given code.
	 * 将给定的消息与给定的代码相关联。
	 *
	 * @param code           lookup code 查找代码
	 * @param locale         locale message should be found within 语言环境消息应在
	 * @param defaultMessage message associated with this lookup code  与该查找代码关联的消息
	 * @see #getStaticMessageSource
	 */
	public void addMessage(String code, Locale locale, String defaultMessage) {
		getStaticMessageSource().addMessage(code, locale, defaultMessage);
	}

}
