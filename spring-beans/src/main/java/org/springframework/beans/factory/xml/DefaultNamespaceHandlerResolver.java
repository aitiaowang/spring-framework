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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 默认命名空间处理程序解析器
 * <p>
 * Default implementation of the {@link NamespaceHandlerResolver} interface.
 * Resolves namespace URIs to implementation classes based on the mappings
 * contained in mapping file.
 * <p>
 * {@link NamespaceHandlerResolver}接口的默认实现。根据映射文件中包含的映射将名称空间URI解析为实现类。
 *
 * <p>By default, this implementation looks for the mapping file at
 * {@code META-INF/spring.handlers}, but this can be changed using the
 * {@link #DefaultNamespaceHandlerResolver(ClassLoader, String)} constructor.
 * <p>
 * 默认情况下，此实现在* {@code META-INF / spring.handlers}处查找映射文件，
 * 但是可以使用{{link #DefaultNamespaceHandlerResolver（ClassLoader，String）}构造函数来更改此文件。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see NamespaceHandler
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 */
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

	/**
	 * 默认处理程序映射位置
	 * The location to look for the mapping files. Can be present in multiple JAR files.
	 * 查找映射文件的位置。可以存在于多个JAR文件中。
	 */
	public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";


	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * ClassLoader to use for NamespaceHandler classes.
	 */
	@Nullable
	private final ClassLoader classLoader;

	/**
	 * Resource location to search for.  要搜索的资源位置。
	 */
	private final String handlerMappingsLocation;

	/**
	 * Stores the mappings from namespace URI to NamespaceHandler class name / instance.
	 * <p>
	 * 存储从名称空间URI到NamespaceHandler类名称/实例的映射
	 */
	@Nullable
	private volatile Map<String, Object> handlerMappings;


	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * default mapping file location.
	 * <p>This constructor will result in the thread context ClassLoader being used
	 * to load resources.
	 *
	 * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
	 */
	public DefaultNamespaceHandlerResolver() {
		this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * default mapping file location.
	 *
	 * @param classLoader the {@link ClassLoader} instance used to load mapping resources
	 *                    (may be {@code null}, in which case the thread context ClassLoader will be used)
	 * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
	 */
	public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader) {
		this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * supplied mapping file location.
	 *
	 * @param classLoader             the {@link ClassLoader} instance used to load mapping resources
	 *                                may be {@code null}, in which case the thread context ClassLoader will be used)
	 * @param handlerMappingsLocation the mapping file location
	 */
	public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader, String handlerMappingsLocation) {
		Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.handlerMappingsLocation = handlerMappingsLocation;
	}


	/**
	 * Locate the {@link NamespaceHandler} for the supplied namespace URI
	 * from the configured mappings.
	 * <p>
	 * 从配置的映射中找到提供的名称空间URI的{@link NamespaceHandler}。
	 *
	 * @param namespaceUri the relevant namespace URI 相关名称空间URI
	 * @return the located {@link NamespaceHandler}, or {@code null} if none found
	 */
	@Override
	@Nullable
	public NamespaceHandler resolve(String namespaceUri) {
		// 获取所有已经配置的handler映射
		Map<String, Object> handlerMappings = getHandlerMappings();
		// 根据命名空间找到对应的信息
		Object handlerOrClassName = handlerMappings.get(namespaceUri);
		if (handlerOrClassName == null) {
			return null;
		}
		//已经做过解析的情况，直接从缓存读取
		else if (handlerOrClassName instanceof NamespaceHandler) {
			return (NamespaceHandler) handlerOrClassName;
		} else {
			// 没有做过解析，则返回的是类路径
			String className = (String) handlerOrClassName;
			try {
				//使用反射将类路径转换为类
				Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
				if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
					throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
							"] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
				}
				//初始化类
				NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
				// 调用自定义的NamespaceHandler的初始化方法
				namespaceHandler.init();
				//记录在缓存
				handlerMappings.put(namespaceUri, namespaceHandler);
				return namespaceHandler;
			} catch (ClassNotFoundException ex) {
				throw new FatalBeanException("Could not find NamespaceHandler class [" + className +
						"] for namespace [" + namespaceUri + "]", ex);
			} catch (LinkageError err) {
				throw new FatalBeanException("Unresolvable class definition for NamespaceHandler class [" +
						className + "] for namespace [" + namespaceUri + "]", err);
			}
		}
	}

	/**
	 * Load the specified NamespaceHandler mappings lazily.
	 * <p>
	 * 延迟加载指定的NamespaceHandler映射。
	 */
	private Map<String, Object> getHandlerMappings() {
		Map<String, Object> handlerMappings = this.handlerMappings;
		// 如果没有被缓存，则开始进行缓存
		if (handlerMappings == null) {
			synchronized (this) {
				handlerMappings = this.handlerMappings;
				if (handlerMappings == null) {
					if (logger.isTraceEnabled()) {
						// 从this.handlerMappingsLocation中加载NamespaceHandler映射
						logger.trace("Loading NamespaceHandler mappings from [" + this.handlerMappingsLocation + "]");
					}
					try {
						// this.handlerMappingsLocation在构造函数中已经被初始化为 :META-INF/Spring.handlers
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
						if (logger.isTraceEnabled()) {
							logger.trace("Loaded NamespaceHandler mappings: " + mappings);
						}
						handlerMappings = new ConcurrentHashMap<>(mappings.size());
						// 将properties格式文件合并到Map格式的handlerMappings
						CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
						this.handlerMappings = handlerMappings;
					} catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
					}
				}
			}
		}
		return handlerMappings;
	}


	@Override
	public String toString() {
		return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
	}

}
