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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * Internal representation of a null bean instance, e.g. for a {@code null} value
 * returned from {@link FactoryBean#getObject()} or from a factory method.
 * <p>
 * 空bean实例的内部表示形式，例如从{@link FactoryBean#getObject()}或工厂方法返回的{@code null}值。
 *
 * <p>Each such null bean is represented by a dedicated {@code NullBean} instance
 * which are not equal to each other, uniquely differentiating each bean as returned
 * from all variants of {@link org.springframework.beans.factory.BeanFactory#getBean}.
 * However, each such instance will return {@code true} for {@code #equals(null)}
 * and returns "null" from {@code #toString()}, which is how they can be tested
 * externally (since this class itself is not public).
 * <p>
 * 每个这样的空bean都由专用的{@code NullBean}实例表示，它们彼此不相等，
 * 将返回的每个bean与{@link org.springframework.beans.factory.BeanFactory#getBean}的所有变体唯一地区分开来。
 * 但是，每个这样的实例将为{@code #equals(null)}返回{@code true} ，
 * 并从{@code #toString（）}返回“null”，这是从外部对其进行测试的方式（因为此类本身不是公开的）。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
final class NullBean {

	NullBean() {
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || obj == null);
	}

	@Override
	public int hashCode() {
		return NullBean.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
