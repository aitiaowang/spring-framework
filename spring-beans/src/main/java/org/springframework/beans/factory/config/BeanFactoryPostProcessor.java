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

/**
 * Allows for custom modification of an application context's bean definitions,
 * adapting the bean property values of the context's underlying bean factory.
 * <p>
 * 允许自定义修改应用程序上下文的Bean定义，调整上下文的基础Bean工厂的Bean属性值。
 *
 *
 * <p>Application contexts can auto-detect BeanFactoryPostProcessor beans in
 * their bean definitions and apply them before any other beans get created.
 * <p>
 * 应用程序上下文可以在它们的bean定义中自动检测BeanFactoryPostProcessor bean，并在创建任何其他bean之前应用它们。
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context.
 * <p>
 * 对于针对系统管理员的自定义配置文件很有用，这些文件覆盖了在应用程序上下文中配置的Bean属性。
 *
 * <p>See PropertyResourceConfigurer and its concrete implementations
 * for out-of-the-box solutions that address such configuration needs.
 * <p>
 * 请参阅PropertyResourceConfigurer及其具体实现，以了解解决此类配置需求的即用型解决方案。
 *
 * <p>A BeanFactoryPostProcessor may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 * <p>
 * BeanFactoryPostProcessor可以与bean定义进行交互并对其进行修改，但不能与bean实例进行交互。
 * 这样做可能导致bean实例化过早，从而违反了容器并造成了意外的副作用。
 * 如果需要与bean实例进行交互，请考虑实现{@link BeanPostProcessor}。
 *
 * @author Juergen Hoeller
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 * @since 06.07.2003
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * 当Spring加载任何实现了这个接口的bean的配置时，都会在bean工厂载入所有bean的配置之后执行postProcessBeanFactory方法，
	 * 在{@link PropertyResourceConfigurer}类中实现了postProcessBeanFactory方法，
	 * 在方法中先后调用了mergerProperties,convertProperties,processProperties这3个方法，
	 * 分别得到配置，将得到的配置转换为合适的类型，最后将配置内容告知BeanFactory。
	 * 正是通过实现BeanFactoryPostProcessor接口，BeanFactory会在实例化任何bean之前获得配置信息，从而能够正确解析bean描述文件中的变量引用
	 * <p>
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for overriding or adding
	 * properties even to eager-initializing beans.
	 * <p>
	 * 在标准初始化之后，修改应用程序上下文的内部bean工厂。所有bean定义都将被加载，但是还没有实例化bean。
	 * 这甚至可以覆盖或添加属性，甚至可以用于初始化bean。
	 *
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
