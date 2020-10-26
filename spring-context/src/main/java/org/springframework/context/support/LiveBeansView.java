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

package org.springframework.context.support;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapter for live beans view exposure, building a snapshot of current beans
 * and their dependencies from either a local {@code ApplicationContext} (with a
 * local {@code LiveBeansView} bean definition) or all registered ApplicationContexts
 * (driven by the {@value #MBEAN_DOMAIN_PROPERTY_NAME} environment property).
 * <p>
 * 活跃bean视图公开的适配器，从本地{@code ApplicationContext}(使用本地{@code LiveBeansView} bean定义)
 * 或所有注册的ApplicationContexts(由{@value #MBEAN_DOMAIN_PROPERTY_NAME}环境属性驱动)构建当前bean及其依赖关系的快照。
 *
 * <p>Note: This feature is still in beta and primarily designed for use with
 * Spring Tool Suite 3.1 and higher.
 * 注意：该功能仍处于beta(测试)中，主要设计用于Spring Tool Suite 3.1及更高版本。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #getSnapshotAsJson()
 * @see org.springframework.web.context.support.LiveBeansViewServlet
 * @since 3.2
 */
public class LiveBeansView implements LiveBeansViewMBean, ApplicationContextAware {

	/**
	 * The "MBean Domain" property name.
	 * “ MBean域”属性名称。
	 */
	public static final String MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain";

	/**
	 * The MBean application key.
	 * MBean应用程序密钥。
	 */
	public static final String MBEAN_APPLICATION_KEY = "application";

	/**
	 * 应用程序上下文
	 *
	 * @date 2020/10/26 10:43
	 */
	private static final Set<ConfigurableApplicationContext> applicationContexts = new LinkedHashSet<>();

	@Nullable
	private static String applicationName;


	/**
	 * 注册应用程序上下文
	 *
	 * @date 2020/10/26 10:57
	 */
	static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
		String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
		if (mbeanDomain != null) {
			synchronized (applicationContexts) {
				if (applicationContexts.isEmpty()) {
					try {
						MBeanServer server = ManagementFactory.getPlatformMBeanServer();
						applicationName = applicationContext.getApplicationName();
						server.registerMBean(new LiveBeansView(),
								new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					} catch (Throwable ex) {
						throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
					}
				}
				applicationContexts.add(applicationContext);
			}
		}
	}

	/**
	 * 取消注册应用程序上下文
	 *
	 * @return
	 * @date 2020/10/26 10:21
	 */
	static void unregisterApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (applicationContexts) {
			if (applicationContexts.remove(applicationContext) && applicationContexts.isEmpty()) {
				try {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
					if (mbeanDomain != null) {
						server.unregisterMBean(new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					}
				} catch (Throwable ex) {
					throw new ApplicationContextException("Failed to unregister LiveBeansView MBean", ex);
				} finally {
					applicationName = null;
				}
			}
		}
	}


	@Nullable
	private ConfigurableApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}


	/**
	 * Generate a JSON snapshot of current beans and their dependencies,
	 * finding all active ApplicationContexts through {@link #findApplicationContexts()},
	 * then delegating to {@link #generateJson(java.util.Set)}.
	 */
	@Override
	public String getSnapshotAsJson() {
		Set<ConfigurableApplicationContext> contexts;
		if (this.applicationContext != null) {
			contexts = Collections.singleton(this.applicationContext);
		} else {
			contexts = findApplicationContexts();
		}
		return generateJson(contexts);
	}

	/**
	 * Find all applicable ApplicationContexts for the current application.
	 * <p>Called if no specific ApplicationContext has been set for this LiveBeansView.
	 *
	 * @return the set of ApplicationContexts
	 */
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		synchronized (applicationContexts) {
			return new LinkedHashSet<>(applicationContexts);
		}
	}

	/**
	 * Actually generate a JSON snapshot of the beans in the given ApplicationContexts.
	 * <p>This implementation doesn't use any JSON parsing libraries in order to avoid
	 * third-party library dependencies. It produces an array of context description
	 * objects, each containing a context and parent attribute as well as a beans
	 * attribute with nested bean description objects. Each bean object contains a
	 * bean, scope, type and resource attribute, as well as a dependencies attribute
	 * with a nested array of bean names that the present bean depends on.
	 *
	 * @param contexts the set of ApplicationContexts
	 * @return the JSON document
	 */
	protected String generateJson(Set<ConfigurableApplicationContext> contexts) {
		StringBuilder result = new StringBuilder("[\n");
		for (Iterator<ConfigurableApplicationContext> it = contexts.iterator(); it.hasNext(); ) {
			ConfigurableApplicationContext context = it.next();
			result.append("{\n\"context\": \"").append(context.getId()).append("\",\n");
			if (context.getParent() != null) {
				result.append("\"parent\": \"").append(context.getParent().getId()).append("\",\n");
			} else {
				result.append("\"parent\": null,\n");
			}
			result.append("\"beans\": [\n");
			ConfigurableListableBeanFactory bf = context.getBeanFactory();
			String[] beanNames = bf.getBeanDefinitionNames();
			boolean elementAppended = false;
			for (String beanName : beanNames) {
				BeanDefinition bd = bf.getBeanDefinition(beanName);
				if (isBeanEligible(beanName, bd, bf)) {
					if (elementAppended) {
						result.append(",\n");
					}
					result.append("{\n\"bean\": \"").append(beanName).append("\",\n");
					result.append("\"aliases\": ");
					appendArray(result, bf.getAliases(beanName));
					result.append(",\n");
					String scope = bd.getScope();
					if (!StringUtils.hasText(scope)) {
						scope = BeanDefinition.SCOPE_SINGLETON;
					}
					result.append("\"scope\": \"").append(scope).append("\",\n");
					Class<?> beanType = bf.getType(beanName);
					if (beanType != null) {
						result.append("\"type\": \"").append(beanType.getName()).append("\",\n");
					} else {
						result.append("\"type\": null,\n");
					}
					result.append("\"resource\": \"").append(getEscapedResourceDescription(bd)).append("\",\n");
					result.append("\"dependencies\": ");
					appendArray(result, bf.getDependenciesForBean(beanName));
					result.append("\n}");
					elementAppended = true;
				}
			}
			result.append("]\n");
			result.append("}");
			if (it.hasNext()) {
				result.append(",\n");
			}
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * Determine whether the specified bean is eligible for inclusion in the
	 * LiveBeansView JSON snapshot.
	 *
	 * @param beanName the name of the bean
	 * @param bd       the corresponding bean definition
	 * @param bf       the containing bean factory
	 * @return {@code true} if the bean is to be included; {@code false} otherwise
	 */
	protected boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
		return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE &&
				(!bd.isLazyInit() || bf.containsSingleton(beanName)));
	}

	/**
	 * Determine a resource description for the given bean definition and
	 * apply basic JSON escaping (backslashes, double quotes) to it.
	 *
	 * @param bd the bean definition to build the resource description for
	 * @return the JSON-escaped resource description
	 */
	@Nullable
	protected String getEscapedResourceDescription(BeanDefinition bd) {
		String resourceDescription = bd.getResourceDescription();
		if (resourceDescription == null) {
			return null;
		}
		StringBuilder result = new StringBuilder(resourceDescription.length() + 16);
		for (int i = 0; i < resourceDescription.length(); i++) {
			char character = resourceDescription.charAt(i);
			if (character == '\\') {
				result.append('/');
			} else if (character == '"') {
				result.append("\\").append('"');
			} else {
				result.append(character);
			}
		}
		return result.toString();
	}

	private void appendArray(StringBuilder result, String[] arr) {
		result.append('[');
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(StringUtils.arrayToDelimitedString(arr, "\", \""));
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(']');
	}

}
