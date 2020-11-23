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

import org.springframework.beans.BeansException;

/**
 * Defines a factory which can return an Object instance
 * (possibly shared or independent) when invoked.
 * 定义一个工厂，该工厂在调用时可以返回Object实例（可能是共享的或独立的）。
 *
 * <p>This interface is typically used to encapsulate a generic factory which
 * returns a new instance (prototype) of some target object on each invocation.
 * 此接口通常用于封装通用工厂，该工厂在每次调用时都会返回某个目标对象的新实例（原型）。
 *
 *
 * <p>This interface is similar to {@link FactoryBean}, but implementations
 * of the latter are normally meant to be defined as SPI instances in a
 * {@link BeanFactory}, while implementations of this class are normally meant
 * to be fed as an API to other beans (through injection). As such, the
 * {@code getObject()} method has different exception handling behavior.
 * <p>
 * 此接口类似于{@link FactoryBean}，但是后者的实现通常是在{@link BeanFactory}中定义为SPI实例，
 * 而此类的实现通常是作为API馈送。其他beans（通过注射）。因此，{@code getObject()}方法具有不同的异常处理行为。
 *
 * @param <T> the object type 对象类型
 * @author Colin Sampaleanu
 * @see FactoryBean
 * @since 1.0.2
 */
@FunctionalInterface
public interface ObjectFactory<T> {

	/**
	 * Return an instance (possibly shared or independent)
	 * of the object managed by this factory.
	 * 返回此工厂管理的对象的一个​​实例（可能是共享的或独立的）
	 *
	 * @return the resulting instance  结果实例
	 * @throws BeansException in case of creation errors 如果出现创建错误
	 */
	T getObject() throws BeansException;

}
