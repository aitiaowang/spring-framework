/*
 * Copyright 2002-2020 the original author or authors.
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

import java.beans.PropertyEditor;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * 综合FactoryBeanRegistrySupport和ConfigurableBeanFactory的功能
 * <p>
 * Abstract base class for {@link org.springframework.beans.factory.BeanFactory}
 * implementations, providing the full capabilities of the
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI.
 * Does <i>not</i> assume a listable bean factory: can therefore also be used
 * as base class for bean factory implementations which obtain bean definitions
 * from some backend resource (where bean definition access is an expensive operation).
 * <p>
 * {@link org.springframework.beans.factory.BeanFactory}实现的抽象基类，
 * 提供SPI {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}的全部功能。
 * <i>不是</ i>假定有一个可列出的bean工厂：因此也可以用作从某些后端资源（其中bean定义访问是一项昂贵的操作）
 * 获得bean定义的bean工厂实现的基类。
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry},
 * singleton/prototype determination, {@link org.springframework.beans.factory.FactoryBean}
 * handling, aliases, bean definition merging for child bean definitions,
 * and bean destruction ({@link org.springframework.beans.factory.DisposableBean}
 * interface, custom destroy methods). Furthermore, it can manage a bean factory
 * hierarchy (delegating to the parent in case of an unknown bean), through implementing
 * the {@link org.springframework.beans.factory.HierarchicalBeanFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
 * for a given bean name and creating a bean instance for a given bean definition,
 * respectively. Default implementations of those operations can be found in
 * {@link DefaultListableBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see DefaultListableBeanFactory#getBeanDefinition
 * @since 15 April 2001
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/**
	 * Parent bean factory, for bean inheritance support.
	 * 父bean工厂，用于bean继承支持。
	 */
	@Nullable
	private BeanFactory parentBeanFactory;

	/**
	 * ClassLoader to resolve bean class names with, if necessary.
	 * 必要时使用ClassLoader解析Bean类名称
	 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * ClassLoader to temporarily resolve bean class names with, if necessary.
	 * 必要时使用ClassLoader临时解析bean类名称。
	 */
	@Nullable
	private ClassLoader tempClassLoader;

	/**
	 * Whether to cache bean metadata or rather reobtain it for every access.
	 */
	private boolean cacheBeanMetadata = true;

	/**
	 * Resolution strategy for expressions in bean definition values.
	 */
	@Nullable
	private BeanExpressionResolver beanExpressionResolver;

	/**
	 * Spring ConversionService to use instead of PropertyEditors.
	 */
	@Nullable
	private ConversionService conversionService;

	/**
	 * Custom PropertyEditorRegistrars to apply to the beans of this factory.
	 */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

	/**
	 * Custom PropertyEditors to apply to the beans of this factory.
	 */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

	/**
	 * A custom TypeConverter to use, overriding the default PropertyEditor mechanism.
	 */
	@Nullable
	private TypeConverter typeConverter;

	/**
	 * String resolvers to apply e.g. to annotation attribute values.
	 */
	private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();

	/**
	 * BeanPostProcessors to apply.
	 * 要应用的Bean后置处理器集合
	 */
	private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

	/**
	 * Indicates whether any InstantiationAwareBeanPostProcessors have been registered.
	 * 指示是否已注册任何InstantiationAwareBeanPostProcessor(实例化Bean后处理器)。
	 */
	private volatile boolean hasInstantiationAwareBeanPostProcessors;

	/**
	 * Indicates whether any DestructionAwareBeanPostProcessors have been registered.
	 */
	private volatile boolean hasDestructionAwareBeanPostProcessors;

	/**
	 * Map from scope identifier String to corresponding Scope.
	 */
	private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

	/**
	 * Security context used when running with a SecurityManager.
	 */
	@Nullable
	private SecurityContextProvider securityContextProvider;

	/**
	 * Map from bean name to merged RootBeanDefinition.
	 * 从bean名称映射到合并的RootBeanDefinition。
	 */
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

	/**
	 * Names of beans that have already been created at least once.
	 * 至少已经创建一次的bean的名称。
	 */
	private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	/**
	 * Names of beans that are currently in creation.
	 * 当前正在创建的bean的名称。
	 */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation =
			new NamedThreadLocal<>("Prototype beans currently in creation"); // 当前正在创建的原型bean


	/**
	 * Create a new AbstractBeanFactory.
	 * 创建一个新的AbstractBeanFactory。
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * Create a new AbstractBeanFactory with the given parent.
	 * 使用给定的父级创建一个新的AbstractBeanFactory
	 *
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 *                          父bean工厂，如果没有则为{@code null}
	 * @see #getBean
	 */
	public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return doGetBean(name, requiredType, null, false);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return doGetBean(name, null, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 *
	 * @param name         the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args         arguments to use when creating a bean instance using explicit arguments
	 *                     (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 */
	public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
			throws BeansException {

		return doGetBean(name, requiredType, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * <p>
	 * 返回一个实例，该实例可以是指定bean的共享或独立的。
	 *
	 * @param name          the name of the bean to retrieve   要检索的bean的名称
	 * @param requiredType  the required type of the bean to retrieve   检索所需的bean类型
	 * @param args          arguments to use when creating a bean instance using explicit arguments
	 *                      (only applied when creating a new instance as opposed to retrieving an existing one)
	 *                      使用显式参数创建bean实例时要使用的参数（仅在创建新实例而不是检索现有实例时适用）
	 * @param typeCheckOnly whether the instance is obtained for a type check,
	 *                      not for actual use
	 *                      是否获取实例以进行类型检查，并非实际使用
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
			throws BeansException {

		//获取bean的原始名称
		String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		/**
		 * 检查缓存中或者实例工厂中是否有对应的实例;
		 * 为什么会首先使用这段代码呢:
		 * 因为在创建单例bean的时候会存在依赖注入的情况，而在创建依赖的时候为了避免循环依赖，
		 * Spring创建bean的原则是不等bean创建完成就会将创建bean的ObjectFactory提早曝光，
		 * 也就是将ObjectFactory加入到缓存中，一旦下个bean创建时候需要依赖上个bean则直接使用ObjectFactory
		 */
		// 直接尝试从缓存获取或者singletonFactories中的ObjectFactory中获取
		Object sharedInstance = getSingleton(beanName);
		//bean存在 且创建bean参数为空时
		if (sharedInstance != null && args == null) {
			if (logger.isTraceEnabled()) {
				//判断实例是否正在创建中
				if (isSingletonCurrentlyInCreation(beanName)) {
					// 返回还没有完全初始化的单例bean + name + 的主动缓存实例—循环引用的结果
					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				} else {
					// 返回单例bean的缓存实例
					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			// 获取Bean实例的对象
			// 返回对应的实例，有时候存在诸如BeanFactory的情况并不是直接返回实例本身，而是返回指定方法返回的实例
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		} else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			/**
			 * 只有在单例情况下，才会尝试解决循环依赖，原型模式情况下，
			 * 如果存在A中有B的属性，B中有A的属性，那么当依赖注入的时候，
			 * 就会产生当A还未创建完的时候，因为对于B的创建再次返回创建A,
			 * 造成循环依赖，也就是下面的情况
			 */
			//如果我们已经创建(或者正在创建中)了这个bean实例，则失败:我们大概是在一个循环引用中。
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}
			// Check if bean definition exists in this factory.
			// 检查该工厂中是否存在bean定义。
			BeanFactory parentBeanFactory = getParentBeanFactory();
			//如果BeanDefinitionMap中也就是在所有已经加载的类中不包括beanName则尝试从parentBeanFactory中检测
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) { //bean没有被定义
				// Not found -> check parent.
				// 找不到->检查父项。
				String nameToLookup = originalBeanName(name); // 原始Bean名称
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					//获取bean (递归到BeanFactory中寻找)
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				} else if (args != null) {
					// Delegation to parent with explicit args.
					// 使用显式参数委派给父级。 获取bean
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				} else if (requiredType != null) {
					// No args -> delegate to standard getBean method.
					// 没有参数->委托给标准的getBean方法。  获取bean
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				} else {
					return (T) parentBeanFactory.getBean(nameToLookup);
				}
			}
			//非仅仅类型检查 - 意味着要创建bean，则需要标记对应状态
			if (!typeCheckOnly) {
				// 将指定的bean标记为已创建（或将要创建）。
				markBeanAsCreated(beanName);
			}

			try {
				// 合并父子bean(如果有)，返回父bean
				// 将存储XML配置文件的GenericBeanDefinition转换为RootBeanDefinition，
				// 如果指定BeanName是子bean的话同时会合并父类的相关属性
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				//检查bean，如果是抽象bean，抛出异常
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				// 确保当前bean依赖的bean的初始化。
				// 若存在依赖，则需要递归实例化依赖的bean
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						//判断循环依赖
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						//注册从属bean
						registerDependentBean(dep, beanName);
						try {
							getBean(dep);
						} catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// Create bean instance.
				// 创建bean实例。实例化依赖的bean后，就可以实例化mbd本身了
				// singleton模式的创建
				if (mbd.isSingleton()) {
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						} catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							//从单例缓存中显式删除实例：创建过程可能将它
							// 急于放置在该实例中，以允许循环引用解析。
							// 还删除所有收到对该bean的临时引用的bean。
							destroySingleton(beanName);
							throw ex;
						}
					});
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}
				// 指定的scope上实例化bean
				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					//这是一个原型 -> 创建一个新实例。
					Object prototypeInstance = null;
					try {
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					} finally {
						afterPrototypeCreation(beanName);
					}
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				} else {
					String scopeName = mbd.getScope();
					if (!StringUtils.hasLength(scopeName)) {
						throw new IllegalStateException("No scope name defined for bean ´" + beanName + "'");
					}
					Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							} finally {
								afterPrototypeCreation(beanName);
							}
						});
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					} catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; consider " +
										"defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			} catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		// 检查所需的类型是否与实际bean实例的类型匹配。
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return convertedBean;
			} catch (TypeMismatchException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

	@Override
	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
		}
		// Not found -> check parent.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isSingleton(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// In case of FactoryBean, return singleton status of created object if not a dereference.
		if (mbd.isSingleton()) {
			if (isFactoryBean(beanName, mbd)) {
				if (BeanFactoryUtils.isFactoryDereference(name)) {
					return true;
				}
				FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isPrototype()) {
			// In case of FactoryBean, return singleton status of created object if not a dereference.
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}

		// Singleton or scoped - not a prototype.
		// However, FactoryBean may still produce a prototype object...
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			return false;
		}
		if (isFactoryBean(beanName, mbd)) {
			FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(
						(PrivilegedAction<Boolean>) () ->
								((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
										!fb.isSingleton()),
						getAccessControlContext());
			} else {
				return ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
						!fb.isSingleton());
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			if (beanInstance instanceof FactoryBean) {
				if (!BeanFactoryUtils.isFactoryDereference(name)) {
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && typeToMatch.isAssignableFrom(type));
				} else {
					return typeToMatch.isInstance(beanInstance);
				}
			} else if (!BeanFactoryUtils.isFactoryDereference(name)) {
				if (typeToMatch.isInstance(beanInstance)) {
					// Direct match for exposed instance?
					return true;
				} else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
					// Generics potentially only match on the target class, not on the proxy...
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					Class<?> targetType = mbd.getTargetType();
					if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
						// Check raw class match as well, making sure it's exposed on the proxy.
						Class<?> classToMatch = typeToMatch.resolve();
						if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
							return false;
						}
						if (typeToMatch.isAssignableFrom(targetType)) {
							return true;
						}
					}
					ResolvableType resolvableType = mbd.targetType;
					if (resolvableType == null) {
						resolvableType = mbd.factoryMethodReturnType;
					}
					return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
				}
			}
			return false;
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return false;
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
		}

		// Retrieve corresponding bean definition.
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		Class<?> classToMatch = typeToMatch.resolve();
		if (classToMatch == null) {
			classToMatch = FactoryBean.class;
		}
		Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
				new Class<?>[]{classToMatch} : new Class<?>[]{FactoryBean.class, classToMatch});

		// Check decorated bean definition, if any: We assume it'll be easier
		// to determine the decorated bean's type than the proxy's type.
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
		if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
			RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
			Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
			if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
				return typeToMatch.isAssignableFrom(targetClass);
			}
		}

		Class<?> beanType = predictBeanType(beanName, mbd, typesToMatch);
		if (beanType == null) {
			return false;
		}

		// Check bean class whether we're dealing with a FactoryBean.
		if (FactoryBean.class.isAssignableFrom(beanType)) {
			if (!BeanFactoryUtils.isFactoryDereference(name) && beanInstance == null) {
				// If it's a FactoryBean, we want to look at what it creates, not the factory class.
				beanType = getTypeForFactoryBean(beanName, mbd);
				if (beanType == null) {
					return false;
				}
			}
		} else if (BeanFactoryUtils.isFactoryDereference(name)) {
			// Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
			// type but we nevertheless are being asked to dereference a FactoryBean...
			// Let's check the original bean class and proceed with it if it is a FactoryBean.
			beanType = predictBeanType(beanName, mbd, FactoryBean.class);
			if (beanType == null || !FactoryBean.class.isAssignableFrom(beanType)) {
				return false;
			}
		}

		ResolvableType resolvableType = mbd.targetType;
		if (resolvableType == null) {
			resolvableType = mbd.factoryMethodReturnType;
		}
		if (resolvableType != null && resolvableType.resolve() == beanType) {
			return typeToMatch.isAssignableFrom(resolvableType);
		}
		return typeToMatch.isAssignableFrom(beanType);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// Check manually registered singletons.
		// 检查手动注册的单例。
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			} else {
				return beanInstance.getClass();
			}
		}

		// No singleton instance found -> check bean definition.
		// 找不到单例实例->检查bean定义。
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			// 在此工厂中找不到bean定义->委托给父对象。
			return parentBeanFactory.getType(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// Check decorated bean definition, if any: We assume it'll be easier
		// to determine the decorated bean's type than the proxy's type.
		// 检查修饰的bean定义（如果有的话）：我们假设确定修饰的bean的类型比代理的类型更容易
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
		if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
			RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
			Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
			if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
				return targetClass;
			}
		}

		//预测Bean类型
		Class<?> beanClass = predictBeanType(beanName, mbd);

		// Check bean class whether we're dealing with a FactoryBean.
		// 检查bean类是否正在处理FactoryBean。
		if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
			if (!BeanFactoryUtils.isFactoryDereference(name)) {
				// If it's a FactoryBean, we want to look at what it creates, not at the factory class.
				// 如果它是FactoryBean，则我们要查看它创建的内容，而不是工厂类。
				return getTypeForFactoryBean(beanName, mbd);
			} else {
				return beanClass;
			}
		} else {
			return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
		}
	}

	@Override
	public String[] getAliases(String name) {
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<>();
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}
		String[] retrievedAliases = super.getAliases(beanName);
		for (String retrievedAlias : retrievedAliases) {
			String alias = (factoryPrefix ? FACTORY_BEAN_PREFIX : "") + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
		return StringUtils.toStringArray(aliases);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	@Override
	public boolean containsLocalBean(String name) {
		String beanName = transformedBeanName(name);
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	@Override
	@Nullable
	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	@Override
	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	@Override
	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	@Override
	public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	@Override
	@Nullable
	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	@Override
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	/**
	 * Return the set of PropertyEditorRegistrars.
	 */
	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	@Override
	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	/**
	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Return the custom TypeConverter to use, if any.
	 *
	 * @return the custom TypeConverter, or {@code null} if none specified
	 */
	@Nullable
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		} else {
			// Build default TypeConverter, registering custom editors.
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	@Override
	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	@Override
	public boolean hasEmbeddedValueResolver() {
		return !this.embeddedValueResolvers.isEmpty();
	}

	@Override
	@Nullable
	public String resolveEmbeddedValue(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
			if (result == null) {
				return null;
			}
		}
		return result;
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		// Remove from old position, if any
		this.beanPostProcessors.remove(beanPostProcessor);
		// Track whether it is instantiation/destruction aware
		if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
			this.hasInstantiationAwareBeanPostProcessors = true;
		}
		if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
			this.hasDestructionAwareBeanPostProcessors = true;
		}
		// Add to end of list
		this.beanPostProcessors.add(beanPostProcessor);
	}

	@Override
	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 * 返回将应用于此工厂创建的bean的beanpostprocessor列表。
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * Return whether this factory holds a InstantiationAwareBeanPostProcessor
	 * that will get applied to singleton beans on creation.
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		return this.hasInstantiationAwareBeanPostProcessors;
	}

	/**
	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return this.hasDestructionAwareBeanPostProcessors;
	}

	@Override
	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		Scope previous = this.scopes.put(scopeName, scope);
		if (previous != null && previous != scope) {
			if (logger.isDebugEnabled()) {
				logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
			}
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
			}
		}
	}

	@Override
	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	@Override
	@Nullable
	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	/**
	 * Set the security context provider for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the provided security context.
	 */
	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	/**
	 * Delegate the creation of the access control context to the
	 * {@link #setSecurityContextProvider SecurityContextProvider}.
	 * 将访问控制上下文的创建委托给{@link #setSecurityContextProvider SecurityContextProvider}。
	 */
	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.securityContextProvider != null ?
				this.securityContextProvider.getAccessControlContext() :
				AccessController.getContext());
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		setConversionService(otherFactory.getConversionService());
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.typeConverter = otherAbstractFactory.typeConverter;
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.hasInstantiationAwareBeanPostProcessors = this.hasInstantiationAwareBeanPostProcessors ||
					otherAbstractFactory.hasInstantiationAwareBeanPostProcessors;
			this.hasDestructionAwareBeanPostProcessors = this.hasDestructionAwareBeanPostProcessors ||
					otherAbstractFactory.hasDestructionAwareBeanPostProcessors;
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		} else {
			setTypeConverter(otherFactory.getTypeConverter());
			String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
			for (String scopeName : otherScopeNames) {
				this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
			}
		}
	}

	/**
	 * Return a 'merged' BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * <p>This {@code getMergedBeanDefinition} considers bean definition
	 * in ancestors as well.
	 *
	 * @param name the name of the bean to retrieve the merged definition for
	 *             (may be an alias)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException  in case of an invalid bean definition
	 */
	@Override
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		String beanName = transformedBeanName(name);
		// Efficiently check whether bean definition exists in this factory.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// Resolve merged bean definition locally.
		return getMergedLocalBeanDefinition(beanName);
	}

	@Override
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		// No singleton instance found -> check bean definition.
		// 找不到单例实例->检查bean定义。
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			// 在此工厂中找不到bean定义->委托给父对象。
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}
		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}

	@Override
	public boolean isActuallyInCreation(String beanName) {
		return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
	}

	/**
	 * Return whether the specified prototype bean is currently in creation
	 * (within the current thread).
	 * 返回指定的原型bean是否当前正在创建（在当前线程内）。
	 *
	 * @param beanName the name of the bean
	 */
	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		return (curVal != null &&
				(curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
	}

	/**
	 * Callback before prototype creation.
	 * <p>The default implementation register the prototype as currently in creation.
	 *
	 * @param beanName the name of the prototype about to be created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		} else if (curVal instanceof String) {
			Set<String> beanNameSet = new HashSet<>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		} else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * Callback after prototype creation.
	 * <p>The default implementation marks the prototype as not in creation anymore.
	 *
	 * @param beanName the name of the prototype that has been created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		} else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	@Override
	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to the given bean definition.
	 *
	 * @param beanName the name of the bean definition
	 * @param bean     the bean instance to destroy
	 * @param mbd      the merged bean definition
	 */
	protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), getAccessControlContext()).destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
		}
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * Return the bean name, stripping out the factory dereference prefix if necessary,
	 * and resolving aliases to canonical names.
	 * 返回Bean名称，如有必要，去除工厂取消引用前缀，并将别名解析为规范名称。
	 *
	 * @param name the user-specified name   用户指定的名称
	 * @return the transformed bean name     转换后的bean名称
	 */
	protected String transformedBeanName(String name) {
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}

	/**
	 * Determine the original bean name, resolving locally defined aliases to canonical names.
	 * <p>
	 * 确定原始bean名称，将本地定义的别名解析为规范名称。
	 *
	 * @param name the user-specified name   用户指定的名称
	 * @return the original bean name    原始bean名称
	 */
	protected String originalBeanName(String name) {
		// 转换后的bean名称
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}

	/**
	 * Initialize the given BeanWrapper with the custom editors registered
	 * with this factory. To be called for BeanWrappers that will create
	 * and populate bean instances.
	 * <p>The default implementation delegates to {@link #registerCustomEditors}.
	 * Can be overridden in subclasses.
	 *
	 * @param bw the BeanWrapper to initialize
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * <p>To be called for BeanWrappers that will create and populate bean
	 * instances, and for SimpleTypeConverter used for constructor argument
	 * and factory method type conversion.
	 *
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		PropertyEditorRegistrySupport registrySupport =
				(registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
		if (registrySupport != null) {
			registrySupport.useConfigValueEditors();
		}
		if (!this.propertyEditorRegistrars.isEmpty()) {
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					registrar.registerCustomEditors(registry);
				} catch (BeanCreationException ex) {
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						String bceBeanName = bce.getBeanName();
						if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
										"] failed because it tried to obtain currently created bean '" +
										ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		if (!this.customEditors.isEmpty()) {
			this.customEditors.forEach((requiredType, editorClass) ->
					registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
		}
	}


	/**
	 * Return a merged RootBeanDefinition, traversing the parent bean definition
	 * if the specified bean corresponds to a child bean definition.
	 * 返回合并的RootBeanDefinition，遍历父bean定义
	 * 如果指定的bean对应于子bean定义。
	 *
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException  in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		// 首先以最小的锁定快速检查并发映射。
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null) {
			return mbd;
		}
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}

	/**
	 * Return a RootBeanDefinition for the given top-level bean, by merging with
	 * the parent if the given bean's definition is a child bean definition.
	 * <p>
	 * 如果给定顶级bean的定义是子bean定义，则通过与父合并，返回给定顶级bean的RootBeanDefinition。
	 *
	 * @param beanName the name of the bean definition
	 * @param bd       the original bean definition (Root/ChildBeanDefinition)  原始bean定义（Root / ChildBeanDefinition）
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {

		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * Return a RootBeanDefinition for the given bean, by merging with the
	 * parent if the given bean's definition is a child bean definition.
	 * <p>
	 * 如果给定bean的定义是子bean定义，则通过与父级合并，返回给定bean的RootBeanDefinition。
	 *
	 * @param beanName     the name of the bean definition
	 * @param bd           the original bean definition (Root/ChildBeanDefinition)
	 * @param containingBd the containing bean definition in case of inner bean,
	 *                     or {@code null} in case of a top-level bean
	 *                     对于内部bean，包含bean定义;对于顶级bean，包含{@code null}
	 * @return a (potentially merged) RootBeanDefinition for the given bean  给定bean的（可能合并的）RootBeanDefinition
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(
			String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		synchronized (this.mergedBeanDefinitions) {
			RootBeanDefinition mbd = null;

			// Check with full lock now in order to enforce the same merged instance.
			// 立即检查完全锁定，以强制执行相同的合并实例。
			if (containingBd == null) {
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			if (mbd == null) {
				if (bd.getParentName() == null) {
					// Use copy of given root bean definition.
					// 使用给定的根bean定义的副本。
					if (bd instanceof RootBeanDefinition) {
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					} else {
						mbd = new RootBeanDefinition(bd);
					}
				} else {
					// Child bean definition: needs to be merged with parent.
					// 子bean定义：需要与父bean合并
					BeanDefinition pbd;
					try {
						String parentBeanName = transformedBeanName(bd.getParentName());
						if (!beanName.equals(parentBeanName)) {
							pbd = getMergedBeanDefinition(parentBeanName);
						} else {
							BeanFactory parent = getParentBeanFactory();
							if (parent instanceof ConfigurableBeanFactory) {
								pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
							} else {
								throw new NoSuchBeanDefinitionException(parentBeanName,
										"Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
												"': cannot be resolved without a ConfigurableBeanFactory parent");
							}
						}
					} catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					// Deep copy with overridden values.
					// 具有覆盖值的深层复制。
					mbd = new RootBeanDefinition(pbd);
					mbd.overrideFrom(bd);
				}

				// Set default singleton scope, if not configured before.
				// 设置默认的单例作用域（如果之前未配置）。
				if (!StringUtils.hasLength(mbd.getScope())) {
					mbd.setScope(SCOPE_SINGLETON);
				}

				// A bean contained in a non-singleton bean cannot be a singleton itself.
				// Let's correct this on the fly here, since this might be the result of
				// parent-child merging for the outer bean, in which case the original inner bean
				// definition will not have inherited the merged outer bean's singleton status.
				// 非单例bean中包含的bean本身不能是单例。
				// 让我们立即进行更正，因为这可能是外部bean的父子合并的结果，
				// /在这种情况下，原始内部bean的定义不会继承合并的外部bean的单例状态。
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					mbd.setScope(containingBd.getScope());
				}

				// Cache the merged bean definition for the time being
				// (it might still get re-merged later on in order to pick up metadata changes)
				//暂时缓存合并的bean定义
				// （稍后可能仍会重新合并以获取元数据更改）
				if (containingBd == null && isCacheBeanMetadata()) {
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}

			return mbd;
		}
	}

	/**
	 * Check the given merged bean definition,
	 * potentially throwing validation exceptions.
	 * <p>
	 * 检查给定的合并bean定义，可能引发验证异常。
	 * 抽象bean，抛出异常
	 *
	 * @param mbd      the merged bean definition to check
	 * @param beanName the name of the bean
	 * @param args     the arguments for bean creation, if any
	 * @throws BeanDefinitionStoreException in case of validation failure 验证失败
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args)
			throws BeanDefinitionStoreException {

		if (mbd.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}
	}

	/**
	 * Remove the merged bean definition for the specified bean,
	 * recreating it on next access.
	 * <p>
	 * 删除指定bean的合并bean定义，在下次访问时重新创建。
	 *
	 * @param beanName the bean name to clear the merged definition for  Bean名称以清除其合并定义
	 */
	protected void clearMergedBeanDefinition(String beanName) {
		this.mergedBeanDefinitions.remove(beanName);
	}

	/**
	 * Clear the merged bean definition cache, removing entries for beans
	 * which are not considered eligible for full metadata caching yet.
	 * <p>Typically triggered after changes to the original bean definitions,
	 * e.g. after applying a {@code BeanFactoryPostProcessor}. Note that metadata
	 * for beans which have already been created at this point will be kept around.
	 *
	 * @since 4.2
	 */
	public void clearMetadataCache() {
		this.mergedBeanDefinitions.keySet().removeIf(bean -> !isBeanEligibleForMetadataCaching(bean));
	}

	/**
	 * Resolve the bean class for the specified bean definition,
	 * resolving a bean class name into a Class reference (if necessary)
	 * and storing the resolved Class in the bean definition for further use.
	 * <p>
	 * 为指定的bean定义解析bean类，将bean类名称解析为Class引用（如果需要）并将解析后的Class存储在bean定义中以备将来使用。
	 *
	 * @param mbd          the merged bean definition to determine the class for  合并的bean定义来确定其类
	 * @param beanName     the name of the bean (for error handling purposes) Bean的名称（用于错误处理）
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 *                     (also signals that the returned {@code Class} will never be exposed to application code)
	 *                     内部类型匹配时要匹配的类型（还表示返回的{@code Class}永远不会暴露给应用程序代码）
	 * @return the resolved bean class (or {@code null} if none)  解析的Bean类（如果没有，则为{@code null}）
	 * @throws CannotLoadBeanClassException if we failed to load the class
	 */
	@Nullable
	protected Class<?> resolveBeanClass(RootBeanDefinition mbd, String beanName, Class<?>... typesToMatch)
			throws CannotLoadBeanClassException {

		try {
			if (mbd.hasBeanClass()) {
				return mbd.getBeanClass();
			}
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>)
						() -> doResolveBeanClass(mbd, typesToMatch), getAccessControlContext());
			} else {
				return doResolveBeanClass(mbd, typesToMatch);
			}
		} catch (PrivilegedActionException pae) {
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}

	/**
	 * 解析bean
	 *
	 * @param null
	 * @return
	 * @author sxk
	 * @date 2020/12/16 11:28
	 */
	@Nullable
	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch)
			throws ClassNotFoundException {

		ClassLoader beanClassLoader = getBeanClassLoader();
		ClassLoader dynamicLoader = beanClassLoader;
		boolean freshResolve = false;

		if (!ObjectUtils.isEmpty(typesToMatch)) {
			// When just doing type checks (i.e. not creating an actual instance yet),
			// use the specified temporary class loader (e.g. in a weaving scenario).
			//仅进行类型检查时（即尚未创建实际实例），
			//使用指定的临时类加载器（例如，在编织场景中）
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				dynamicLoader = tempClassLoader;
				freshResolve = true;
				if (tempClassLoader instanceof DecoratingClassLoader) {
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						dcl.excludeClass(typeToMatch.getName());
					}
				}
			}
		}

		String className = mbd.getBeanClassName();
		if (className != null) {
			Object evaluated = evaluateBeanDefinitionString(className, mbd);
			if (!className.equals(evaluated)) {
				// A dynamically resolved expression, supported as of 4.2...
				if (evaluated instanceof Class) {
					return (Class<?>) evaluated;
				} else if (evaluated instanceof String) {
					className = (String) evaluated;
					freshResolve = true;
				} else {
					throw new IllegalStateException("Invalid class name expression result: " + evaluated);
				}
			}
			if (freshResolve) {
				// When resolving against a temporary class loader, exit early in order
				// to avoid storing the resolved Class in the bean definition.
				if (dynamicLoader != null) {
					try {
						return dynamicLoader.loadClass(className);
					} catch (ClassNotFoundException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
						}
					}
				}
				return ClassUtils.forName(className, dynamicLoader);
			}
		}

		// Resolve regularly, caching the result in the BeanDefinition...
		// 定期解析，将结果缓存在BeanDefinition中...
		return mbd.resolveBeanClass(beanClassLoader);
	}

	/**
	 * Evaluate the given String as contained in a bean definition,
	 * potentially resolving it as an expression.
	 *
	 * @param value          the value to check
	 * @param beanDefinition the bean definition that the value comes from
	 * @return the resolved value
	 * @see #setBeanExpressionResolver
	 */
	@Nullable
	protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			return value;
		}

		Scope scope = null;
		if (beanDefinition != null) {
			String scopeName = beanDefinition.getScope();
			if (scopeName != null) {
				scope = getRegisteredScope(scopeName);
			}
		}
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}


	/**
	 * 预测Bean类型
	 * <p>
	 * Predict the eventual bean type (of the processed bean instance) for the
	 * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
	 * Does not need to handle FactoryBeans specifically, since it is only
	 * supposed to operate on the raw bean type.
	 * <p>
	 * 预测指定的bean的最终bean类型（已处理bean实例的类型）。由{@link #getType}和{@link #isTypeMatch}调用。
	 * 不需要专门处理FactoryBeans，因为应该仅对原始bean类型进行操作。
	 *
	 * <p>This implementation is simplistic in that it is not able to
	 * handle factory methods and InstantiationAwareBeanPostProcessors.
	 * It only predicts the bean type correctly for a standard bean.
	 * To be overridden in subclasses, applying more sophisticated type detection.
	 * <p>
	 * 此实现非常简单，因为它无法处理工厂方法和InstantiationAwareBeanPostProcessors(实例化感知Bean后处理器)。
	 * 它只能正确预测标准bean的bean类型。在子类中被覆盖，应用更复杂的类型检测。
	 *
	 * @param beanName     the name of the bean
	 * @param mbd          the merged bean definition to determine the type for  合并的bean定义以确定其类型
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 *                     (also signals that the returned {@code Class} will never be exposed to application code)
	 *                     内部类型匹配时要匹配的类型（还表示返回的{@code Class}永远不会暴露给应用程序代码）
	 * @return the type of the bean, or {@code null} if not predictable
	 */
	@Nullable
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = mbd.getTargetType();
		if (targetType != null) {
			return targetType;
		}
		if (mbd.getFactoryMethodName() != null) {
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * Check whether the given bean is defined as a {@link FactoryBean}.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the corresponding bean definition
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
		return (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
	}

	/**
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean already.
	 * <p>
	 * 尽可能确定给定FactoryBean定义的bean类型。仅在尚未为目标bean注册单例实例时调用。
	 *
	 * <p>The default implementation creates the FactoryBean via {@code getBean}
	 * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
	 * this, typically by just instantiating the FactoryBean but not populating it yet,
	 * trying whether its {@code getObjectType} method already returns a type.
	 * If no type found, a full FactoryBean creation as performed by this implementation
	 * should be used as fallback.
	 * <p>
	 * 默认实现通过{@code getBean} 创建FactoryBean来调用其{@code getObjectType}方法。
	 * 鼓励子类优化此操作，通常仅实例化FactoryBean而不填充它，并尝试其{@code getObjectType}方法是否已经返回类型。
	 * 如果未找到类型，则应使用此实现执行的完整FactoryBean创建作为后备。
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the merged bean definition for the bean
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 */
	@Nullable
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		if (!mbd.isSingleton()) {
			return null;
		}
		try {
			FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
			return getTypeForFactoryBean(factoryBean);
		} catch (BeanCreationException ex) {
			if (ex.contains(BeanCurrentlyInCreationException.class)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Bean currently in creation on FactoryBean type check: " + ex);
				}
			} else if (mbd.isLazyInit()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Bean creation exception on lazy FactoryBean type check: " + ex);
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean creation exception on non-lazy FactoryBean type check: " + ex);
				}
			}
			onSuppressedException(ex);
			return null;
		}
	}

	/**
	 * Mark the specified bean as already created (or about to be created).
	 * 将指定的bean标记为已创建（或将要创建）。
	 *
	 * <p>This allows the bean factory to optimize its caching for repeated
	 * creation of the specified bean.
	 * 这允许bean工厂优化其缓存，以便重复创建指定的bean。
	 *
	 * @param beanName the name of the bean
	 */
	protected void markBeanAsCreated(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			synchronized (this.mergedBeanDefinitions) {
				if (!this.alreadyCreated.contains(beanName)) {
					// Let the bean definition get re-merged now that we're actually creating
					// the bean... just in case some of its metadata changed in the meantime.
					//现在，我们实际上是在创建bean时，请重新合并bean定义……以防万一其元数据在此期间发生了变化
					//从缓存中清除合并的Bean定义
					clearMergedBeanDefinition(beanName);
					this.alreadyCreated.add(beanName);
				}
			}
		}
	}

	/**
	 * Perform appropriate cleanup of cached metadata after bean creation failed.
	 *
	 * @param beanName the name of the bean
	 */
	protected void cleanupAfterBeanCreationFailure(String beanName) {
		synchronized (this.mergedBeanDefinitions) {
			this.alreadyCreated.remove(beanName);
		}
	}

	/**
	 * Determine whether the specified bean is eligible for having
	 * its bean definition metadata cached.
	 *
	 * @param beanName the name of the bean
	 * @return {@code true} if the bean's metadata may be cached
	 * at this point already
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * Remove the singleton instance (if any) for the given bean name,
	 * but only if it hasn't been used for other purposes than type checking.
	 * <p>
	 * 除去给定bean名称的单例实例（如果有的话），但仅限于除类型检查以外没有用于其他目的的情况。
	 *
	 * @param beanName the name of the bean
	 * @return {@code true} if actually removed, {@code false} otherwise
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		// 如果bean没有创建
		if (!this.alreadyCreated.contains(beanName)) {
			// 删除bean的对应所有缓存
			removeSingleton(beanName);
			return true;
		} else {
			//bean已经被创建，直接返回false
			return false;
		}
	}

	/**
	 * Check whether this factory's bean creation phase already started,
	 * i.e. whether any bean has been marked as created in the meantime.
	 * 检查该工厂的bean创建阶段是否已经开始，
	 * 例如，在此期间是否有任何bean被标记为已创建。
	 *
	 * @see #markBeanAsCreated
	 * @since 4.2.2
	 */
	protected boolean hasBeanCreationStarted() {
		return !this.alreadyCreated.isEmpty();
	}

	/**
	 * Get the object for the given bean instance, either the bean
	 * instance itself or its created object in case of a FactoryBean.
	 * 获取给定bean实例的对象，可以是bean实例本身，也可以是FactoryBean时创建的对象。
	 * <p>
	 * 该方法做的主要工作：
	 * 1.对FactoryBean正确性的校验
	 * 2.对非FactoryBean不做任何处理
	 * 3.对bean进行转换
	 * 4.将从Factory中解析bean的工作委托给getObjectFromFactoryBean
	 *
	 * @param beanInstance the shared bean instance  共享bean实例
	 * @param name         the name that may include factory dereference prefix  可能包含工厂取消引用前缀的名称
	 * @param beanName     the canonical bean name
	 * @param mbd          the merged bean definition
	 * @return the object to expose for the bean
	 */
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// Don't let calling code try to dereference the factory if the bean isn't a factory.
		// 如果bean不是工厂，不要让调用代码尝试取消对工厂的引用。
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (beanInstance instanceof NullBean) {
				return beanInstance;
			}
			// 如果指定的name是工厂相关（以&为前缀），且beanInstance又不是FactoryBean类型，则验证不通过
			if (!(beanInstance instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
		}

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.
		// If it's a FactoryBean, we use it to create a bean instance, unless the
		// caller actually wants a reference to the factory.
		/**
		 * 现在我们有了一个bean的实例，这个实例可能会是正常的bean或则是FactoryBean,
		 * 如果是FactoryBean我们使用她创建实例，但是如果用户想要直接获取工厂实例而不是工厂的getObject方法对应的实例，
		 * 那么传入的name应该加入前缀
		 */
		if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd == null) {
			// 获取工厂Bean的缓存对象
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// Return bean instance from factory.
			// 到这里已经明确知道，beanInstance一定是FactoryBean类型
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			//containsBeanDefinition检测beanDefinitionMap中，也就是在所有已经加载的类中检测是否定义beanName
			if (mbd == null && containsBeanDefinition(beanName)) {  // bean已被定义
				// 将存储XML配置文件的GenericBeanDefinition转换为RootBeanDefinition，
				// 如果指定BeanName是子bean的话，同时会合并父类的相关属性
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			// 是否是用户定义的而不是应用程序本身定义的
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			//从工厂bean中获取对象
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * Determine whether the given bean name is already in use within this factory,
	 * i.e. whether there is a local bean or alias registered under this name or
	 * an inner bean created with this name.
	 *
	 * @param beanName the name to check
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * Determine whether the given bean requires destruction on shutdown.
	 * <p>The default implementation checks the DisposableBean interface as well as
	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
	 *
	 * @param bean the bean instance to check
	 * @param mbd  the corresponding bean definition
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see AbstractBeanDefinition#getDestroyMethodName()
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (bean.getClass() != NullBean.class &&
				(DisposableBeanAdapter.hasDestroyMethod(bean, mbd) || (hasDestructionAwareBeanPostProcessors() &&
						DisposableBeanAdapter.hasApplicableProcessors(bean, getBeanPostProcessors()))));
	}

	/**
	 * Add the given bean to the list of disposable beans in this factory,
	 * registering its DisposableBean interface and/or the given destroy method
	 * to be called on factory shutdown (if applicable). Only applies to singletons.
	 * <p>
	 * 将给定的bean添加到该工厂的一次性bean列表中，注册其DisposableBean(一次性bean)接口和/或给定的destroy(销毁)方法在工厂关闭时调用（如果适用）。
	 * 仅适用于单例。
	 *
	 * @param beanName the name of the bean
	 * @param bean     the bean instance
	 * @param mbd      the bean definition for the bean
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				// Register a DisposableBean implementation that performs all destruction
				// work for the given bean: DestructionAwareBeanPostProcessors,
				// DisposableBean interface, custom destroy method.
				//注册一个对所有给定bean执行所有销毁工作的DisposableBean实现：DestructionAwareBeanPostProcessors，
				// DisposableBean接口，自定义destroy方法。
				registerDisposableBean(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			} else {
				// A bean with a custom scope...
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
		}
	}


	//---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * Does not consider any hierarchy this factory may participate in.
	 * Invoked by {@code containsBean} when no cached singleton instance is found.
	 * <p>
	 * 检查此bean工厂是否包含具有给定名称的bean定义。
	 * 不考虑该工厂可能参与的任何层次结构。当未找到缓存的单例实例时，由{@code containsBean}调用。
	 *
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 * <p>
	 * 根据具体bean工厂实现的性质，此操作可能很昂贵（例如，由于外部注册表中的目录查找）。
	 * 但是，对于可列出的bean工厂，这通常仅相当于本地哈希查找：因此，该操作是该地方的公共接口的一部分。
	 * 在这种情况下，此模板方法和公共接口方法都可以使用相同的实现。
	 *
	 * @param beanName the name of the bean to look for  要寻找的Bean的名字
	 * @return if this bean factory contains a bean definition with the given name  如果此bean工厂包含具有给定名称的bean定义
	 * @see #containsBean
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 */
	protected abstract boolean containsBeanDefinition(String beanName);

	/**
	 * Return the bean definition for the given bean name.
	 * Subclasses should normally implement caching, as this method is invoked
	 * by this class every time bean definition metadata is needed.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 *
	 * @param beanName the name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name (never {@code null})
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if the bean definition cannot be resolved
	 * @throws BeansException                                                  in case of errors
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * Create a bean instance for the given merged bean definition (and arguments).
	 * The bean definition will already have been merged with the parent definition
	 * in case of a child definition.
	 * <p>All bean retrieval methods delegate to this method for actual bean creation.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the merged bean definition for the bean
	 * @param args     explicit arguments to use for constructor or factory method invocation
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException;

}
