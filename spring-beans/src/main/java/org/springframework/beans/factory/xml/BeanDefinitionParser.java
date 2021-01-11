/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;

/**
 * Interface used by the {@link DefaultBeanDefinitionDocumentReader} to handle custom,
 * top-level (directly under {@code <beans/>}) tags.
 *
 * <p>Implementations are free to turn the metadata in the custom tag into as many
 * {@link BeanDefinition BeanDefinitions} as required.
 *
 * <p>The parser locates a {@link BeanDefinitionParser} from the associated
 * {@link NamespaceHandler} for the namespace in which the custom tag resides.
 *
 * @author Rob Harrop
 * @see NamespaceHandler
 * @see AbstractBeanDefinitionParser
 * @since 2.0
 */
public interface BeanDefinitionParser {

	/**
	 * Parse the specified {@link Element} and register the resulting
	 * {@link BeanDefinition BeanDefinition(s)} with the
	 * {@link org.springframework.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry}
	 * embedded in the supplied {@link ParserContext}.
	 * <p>
	 * 解析指定的{@link Element}并将产生的{@link BeanDefinition BeanDefinition(s)}注册到
	 * {@link org.springframework.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry}
	 * 嵌入在提供的{@link ParserContext}中。
	 *
	 * <p>Implementations must return the primary {@link BeanDefinition} that results
	 * from the parse if they will ever be used in a nested fashion (for example as
	 * an inner tag in a {@code <property/>} tag). Implementations may return
	 * {@code null} if they will <strong>not</strong> be used in a nested fashion.
	 * <p>
	 * 如果实现将以嵌套方式使用（例如，作为{@code <property/>}标记中的内部标记），
	 * 则实现必须返回从解析产生的主{@link BeanDefinition}。如果实现<strong>不</strong>以嵌套方式使用，则实现可能返回{@code null}。
	 *
	 * @param element       the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
	 *                      将被解析为一个或多个{@link BeanDefinition BeanDefinitions}的元素
	 * @param parserContext the object encapsulating the current state of the parsing process;
	 *                      provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 *                      封装解析过程当前状态的对象；提供对{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的访问权限
	 * @return the primary {@link BeanDefinition}
	 */
	@Nullable
	BeanDefinition parse(Element element, ParserContext parserContext);

}
