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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 对接口SingletonbeanRegister各函数的实现
 * <p>
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 * <p>
 * 共享bean实例的通用注册表，实现{@link org.springframework.beans.factory.config.SingletonBeanRegistry}。
 * 允许注册应该由bean名称获得的所有注册表调用者共享的单例实例。
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 * <p>
 * 还支持{@link org.springframework.beans.factory.DisposableBean}实例的注册（可能对应或不对应已注册的单例），
 * 在关闭注册表时被销毁。可以注册bean之间的依赖关系以强制执行适当的关闭命令
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @since 2.0
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * Maximum number of suppressed exceptions to preserve.
	 * 保留的最异常数。
	 */
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;


	/**
	 * Cache of singleton objects: bean name to bean instance.
	 * 单例对象的高速缓存：bean名称到bean实例。
	 * <p>
	 * 用于保存beanName和创建bean实例之间的关系，bean name --> bean instance
	 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/**
	 * Cache of singleton factories: bean name to ObjectFactory.
	 * 单例工厂的高速缓存：Bean名称为ObjectFactory。
	 * <p>
	 * 用于保存BeanName和创建bean的工厂之间的关系， bean name --> ObjectFactory
	 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/**
	 * Cache of early singleton objects: bean name to bean instance.
	 * 早期单例对象的高速缓存：Bean名称到Bean实例。
	 * <p>
	 * 保存BeanName和创建bean实例之间的关系，与{@link singletonObjects}的不同之处在于，	当一个单例bean被放到这里面后，
	 * 那么当bean还在创建过程中，就可以通过{@link org.springframework.beans.factory.BeanFactory#getBean}方法获取到了，
	 * 其目的是用来检测循环引用
	 */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/**
	 * Set of registered singletons, containing the bean names in registration order.
	 * 已注册的单例集，按注册顺序包含Bean名称。
	 * <p>
	 * 用来保存当前所有已注册的bean
	 */
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/**
	 * 正在创建的bean集合，便于对循环依赖进行检查
	 * Names of beans that are currently in creation.
	 * 当前正在创建的bean的名称。
	 */
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * Names of beans currently excluded from in creation checks.
	 * 当前从创建检查中排除的bean名称。
	 */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * Collection of suppressed Exceptions, available for associating related causes.
	 * 异常的集合，可用于关联相关原因
	 */
	@Nullable
	private Set<Exception> suppressedExceptions;

	/**
	 * Flag that indicates whether we're currently within destroySingletons.
	 * 指示我们当前是否在destroySingletons中的标志。
	 */
	private boolean singletonsCurrentlyInDestruction = false;

	/**
	 * Disposable bean instances: bean name to disposable instance.
	 * 一次性bean实例：一次性实例的bean名称。
	 */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/**
	 * Map between containing bean names: bean name to Set of bean names that the bean contains.
	 * 在包含的Bean名称之间映射：Bean名称到Bean包含的Bean名称集
	 */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/**
	 * Map between dependent bean names: bean name to Set of dependent bean names.
	 * 在从属bean名称之间映射：从bean名称到从属bean名称集。
	 */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/**
	 * Map between depending bean names: bean name to Set of bean names for the bean's dependencies.
	 * 在相关的Bean名称之间映射：Bean名称到Bean依赖项的Bean名称集。
	 */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	/**
	 * 注册单例bean
	 *
	 * @param singletonObject 现有的单例对象
	 * @date 2020/11/23 18:20
	 */
	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		synchronized (this.singletonObjects) {
			//判断bean是否注在缓存中
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			//添加单例缓存
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * 注册单例bean
	 * <p>
	 * Add the given singleton object to the singleton cache of this factory.
	 * 将给定的单例对象添加到该工厂的单例缓存中。
	 * <p>To be called for eager registration of singletons.
	 *
	 * @param beanName        the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			//添加单例缓存
			this.singletonObjects.put(beanName, singletonObject);
			//移除单例工厂
			this.singletonFactories.remove(beanName);
			//移除早期单例
			this.earlySingletonObjects.remove(beanName);
			//添加已注册的单例bean
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * 注册单例bean
	 * <p>
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * 如有必要，添加给定的单例工厂以构建指定的单例
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * 被要求急切地注册单例，例如能够解析循环引用。
	 *
	 * @param beanName         the name of the bean
	 * @param singletonFactory the factory for the singleton object 单例对象的工厂
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			//原有单例工厂中不包含，则添加
			if (!this.singletonObjects.containsKey(beanName)) {
				//单例工厂缓存
				this.singletonFactories.put(beanName, singletonFactory);
				//早期bean移除
				this.earlySingletonObjects.remove(beanName);
				//添加已注册的单例bean
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		// 参数true设置标识允许早期依赖
		return getSingleton(beanName, true);
	}

	/**
	 * 三级缓存中获取实例，只查询
	 * <p>
	 * Return the (raw) singleton object registered under the given name.
	 * 返回以给定名称注册的（原始）单例对象。
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * 检查已经实例化的单例，并且还允许对当前创建的单例的早期引用（解析循环引用）。
	 *
	 * @param beanName            the name of the bean to look for 要寻找的bean的名字
	 * @param allowEarlyReference whether early references should be created or not   为true，允许早期依赖
	 * @return the registered singleton object, or {@code null} if none found 注册的单例对象；如果找不到，则为{@code null}
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock
		//检查缓存中是否存在单例
		Object singletonObject = this.singletonObjects.get(beanName);
		//如果不存在，且bean正在创建中
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			//从早期依赖中获取
			singletonObject = this.earlySingletonObjects.get(beanName);
			//早期依赖中不存在且允许创建早期依赖
			if (singletonObject == null && allowEarlyReference) {
				// 锁定全局变量并进行处理
				synchronized (this.singletonObjects) {
					// Consistent creation of early reference within full singleton lock
					//在完整的单例锁定中一致创建早期参考
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {
							//获取对应bean工厂
							/**
							 * 当某些方法需要提前初始化的时候则会调用addSingletonFactory方法
							 *将对应的ObjectFactory初始化策略存储在singletonFactories
							 */
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								//调用预先设定的getObject方法
								singletonObject = singletonFactory.getObject();
								//记录在早期依赖缓存中
								//earlySingletonObjects和singletonFactories互斥，一个添加，一个删除
								this.earlySingletonObjects.put(beanName, singletonObject);
								//缓存工厂中移除
								this.singletonFactories.remove(beanName);
							}
						}
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * 返回以给定名称注册的（原始）单例对象，如果尚未注册，则创建并注册一个新对象。
	 * <p>
	 * 1.检查缓存是否已经加载过
	 * 2.若没有加载，检查bean的单例工厂是否正在销毁
	 * 3.加载单例前，记录加载状态
	 * 4.通过调用参数传入的ObjectFactory的个体Object方法实例化bean
	 * 5.加载单例后的处理方法调用，移除缓存中对改bean的正在加载状态的记录，对应第三步
	 * 6.将结果记录到缓存并删除加载bean过程中所记录的各种辅助状态
	 * 7.返回处理结果
	 *
	 * @param beanName         the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 *                         with, if necessary
	 * @return the registered singleton object
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			// 首先检查对应的bean是否已经加载过，因为singleton模式其实就是复用已创建的bean，所以这一步是必须的
			Object singletonObject = this.singletonObjects.get(beanName);
			// 如果为空才可以进行singleton的bean的初始化
			if (singletonObject == null) {
				//在工厂的单例销毁时不允许创建单例bean
				if (this.singletonsCurrentlyInDestruction) {
					//单例bean创建不允许当单例工厂在销毁时(不要在销毁方法实现中从BeanFactory请求bean !)
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
									"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				/**
				 * 标记bean在创建中， {@see singletonsCurrentlyInCreation}中添加
				 */
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					//初始化bean
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				} catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					//在此期间，是否使单例对象隐式出现-> 如果是，请继续处理它，因为异常指示该状态。
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				} catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				} finally {
					//异常置空
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					/**
					 * 标记bean创建完成，从 {@see singletonsCurrentlyInCreation} 中移除
					 */
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					//新单例，添加单例缓存
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * Register an exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * <p>The default implementation preserves any given exception in this registry's
	 * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
	 * them as related causes to an eventual top-level {@link BeanCreationException}.
	 *
	 * @param ex the Exception to register
	 * @see BeanCreationException#getRelatedCauses()
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * <p>
	 * 从该工厂的单例缓存中删除具有给定名称的Bean，以便在创建失败时清除急于注册的单例。
	 *
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}

	/**
	 * 设置当前bean正在创建
	 *
	 * @param beanName
	 * @param inCreation 是否创建中 true 创建中 false 非创建中
	 */
	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			// 非创建中，则加入排除的bean集合中。
			this.inCreationCheckExclusions.add(beanName);
		} else {
			//创建中，从排除bean集合中移除
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * bean是否正在创建中
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * 返回指定的单例bean当前是否正在创建（在整个工厂内）。
	 *
	 * @param beanName the name of the bean  bean的名称
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * 创建单例之前的回调。
	 * <p>The default implementation register the singleton as currently in creation.
	 * 默认实现将单例注册为当前正在创建中。
	 * <p>
	 * 便于对循环依赖进行检测
	 *
	 * @param beanName the name of the singleton about to be created  要创建的单例的名称
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		//inCreationCheckExclusions中未包含 && 标记单例在创建中
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * 创建单例后的回调。
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * 默认实现将单例标记为不在创建中。
	 *
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		//inCreationCheckExclusions中未包含 && 标记单例不在创建中（移除）
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>
	 * 将给定的bean添加到此注册表中的一次性bean列表中。
	 *
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * <p>
	 * 一次性Bean通常对应于注册的单例，与Bean名称匹配，但可能是不同的实例
	 * （例如，单例的DisposableBean适配器自然不实现Spring的DisposableBean接口）。
	 *
	 * @param beanName the name of the bean
	 * @param bean     the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 *
	 * @param containedBeanName  the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * <p>
	 * 在给定的bean被销毁之前，为给定的bean注册一个要被销毁的依赖bean。
	 *
	 * @param beanName          the name of the bean
	 * @param dependentBeanName the name of the dependent bean  依赖bean的名称
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		//获取bean的原始名称
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * 确定指定的依赖项是否已注册为依赖于给定bean或其任何传递依赖项。
	 *
	 * @param beanName          the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			// TODO 猜测，这里有循环依赖
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * 确定是否已为给定名称注册了依赖bean。
	 *
	 * @param beanName the name of the bean to check 要检查的bean的名称
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 *
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 *
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		clearSingletonCache();
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 *
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * <p>
	 * 销毁给定的bean。如果找到相应的一次性bean实例，则委托给{@code destroyBean}。
	 *
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		// 删除给定名称的已注册单例bean（如果有）
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		// 销毁相应的DisposableBean（一次性bean）实例。
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * <p>
	 * 销毁给定的bean。必须先破坏依赖于给定bean的bean，然后再破坏bean本身。不应抛出任何异常。
	 *
	 * @param beanName the name of the bean
	 * @param bean     the bean instance to destroy  销毁的bean实例
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		// 首先触发依赖bean的破坏...(销毁依赖bean)
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			// 在完全同步内以确保断开连接集
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		if (dependencies != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				// 销毁bean
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		// 销毁bean
		if (bean != null) {
			try {
				bean.destroy();
			} catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		// 触发被包含bean的销毁…
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			// 在完全同步内以确保断开连接集
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		// 从其他bean的依赖项中删除破坏的bean。
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		// 移除销毁的bean的已准备好的依赖项信息。
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>
	 * 将单例互斥锁公开给子类和外部协作者。
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 * <p>
	 * 如果子类执行任何扩展的单例创建阶段，则子类应在给定的Object上进行同步。
	 * 特别是，子类不应该在单例创建中涉及它们自己的互斥体，以避免在惰性初始化情况下出现死锁的可能性。
	 */
	@Override
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
