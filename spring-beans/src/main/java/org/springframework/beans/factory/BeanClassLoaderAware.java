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

package org.springframework.beans.factory;

/**
 * Callback that allows a bean to be aware of the bean
 * {@link ClassLoader class loader}; that is, the class loader used by the
 * present bean factory to load bean classes.
 * <p>
 * 允许bean知道bean的回调{@link ClassLoader class loader};也就是说，当前bean工厂使用的类加载器来加载bean类。
 *
 * <p>This is mainly intended to be implemented by framework classes which
 * have to pick up application classes by name despite themselves potentially
 * being loaded from a shared class loader.
 * <p>
 * 这主要是由框架类实现的，尽管它们可能是从共享类加载器中加载的，但它们必须按名称选择应用程序类。
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link BeanFactory BeanFactory javadocs}.
 * <p>
 * 有关所有bean生命周期方法的列表，请参见{@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 * @since 2.0
 */
public interface BeanClassLoaderAware extends Aware {

	/**
	 * Callback that supplies the bean {@link ClassLoader class loader} to
	 * a bean instance.
	 * <p>
	 * 向bean实例提供bean {@link ClassLoader class loader}的回调。
	 *
	 * <p>Invoked <i>after</i> the population of normal bean properties but
	 * <i>before</i> an initialization callback such as
	 * {@link InitializingBean InitializingBean's}
	 * {@link InitializingBean#afterPropertiesSet()}
	 * method or a custom init-method.
	 * <p>
	 * 在之后填充了普通的bean属性，但是在之前调用了初始化回调，
	 * 例如{@link InitializingBean InitializingBean's {@link InitializingBean＃afterPropertiesSet()} 方法或自定义的初始化方法。
	 *
	 * @param classLoader the owning class loader
	 */
	void setBeanClassLoader(ClassLoader classLoader);

}
