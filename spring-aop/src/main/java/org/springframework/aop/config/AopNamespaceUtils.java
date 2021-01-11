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

package org.springframework.aop.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;

/**
 * Utility class for handling registration of auto-proxy creators used internally
 * by the '{@code aop}' namespace tags.
 *
 * <p>Only a single auto-proxy creator should be registered and multiple configuration
 * elements may wish to register different concrete implementations. As such this class
 * delegates to {@link AopConfigUtils} which provides a simple escalation protocol.
 * Callers may request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see AopConfigUtils
 * @since 2.0
 */
public abstract class AopNamespaceUtils {

	/**
	 * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
	 * <p>
	 * 在与AOP相关的XML标记中找到的{@code proxy-target-class}属性。
	 */
	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

	/**
	 * The {@code expose-proxy} attribute as found on AOP-related XML tags.
	 * <p>
	 * 在与AOP相关的XML标记中可以找到的{@code Exposure-proxy}属性。
	 */
	private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";


	public static void registerAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	/**
	 * 注册{@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator}
	 *
	 * @param parserContext 解析上下文
	 * @param sourceElement 被解析的元素
	 * @date 2021/1/5 10:29
	 */
	public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {
		// 注册或者升级AnnotationAwareAspectJAutoProxyCreator
		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		// 对于Proxy-target-class(代理-目标-类)以及expose-proxy(暴露-代理)属性的处理
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		// 注册组件并通知，便于监听器做进一步处理
		// 其中beanDefinition的className为AnnotationAwareAspectJAutoProxyCreator
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	/**
	 * 必要时使用类代理
	 * 该方法实现了proxy-target-class属性和expose-proxy属性的处理
	 */
	private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
		if (sourceElement != null) {
			// 对于Proxy-target-class属性的处理
			boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
			if (proxyTargetClass) {
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			//对expose-proxy属性的处理
			boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
			if (exposeProxy) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

	private static void registerComponentIfNecessary(@Nullable BeanDefinition beanDefinition, ParserContext parserContext) {
		if (beanDefinition != null) {
			parserContext.registerComponent(
					new BeanComponentDefinition(beanDefinition, AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME));
		}
	}

}
