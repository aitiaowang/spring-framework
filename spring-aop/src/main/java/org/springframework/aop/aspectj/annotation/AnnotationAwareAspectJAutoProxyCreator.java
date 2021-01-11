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

package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link AspectJAwareAdvisorAutoProxyCreator} subclass that processes all AspectJ
 * annotation aspects in the current application context, as well as Spring Advisors.
 * <p>
 * {@link AspectJAwareAdvisorAutoProxyCreator}子类，用于处理当前应用程序上下文以及Spring Advisor中的所有AspectJ 注释方面。
 *
 * <p>Any AspectJ annotated classes will automatically be recognized, and their
 * advice applied if Spring AOP's proxy-based model is capable of applying it.
 * This covers method execution joinpoints.
 * <p>
 * 如果Spring AOP的基于代理的模型能够应用任何AspectJ注释的类，则将自动识别它们的建议。这涵盖了方法执行连接点
 *
 * <p>If the &lt;aop:include&gt; element is used, only @AspectJ beans with names matched by
 * an include pattern will be considered as defining aspects to use for Spring auto-proxying.
 * <p>
 * 如果使用<aop：include>元素，则仅将名称与包含模式匹配的@AspectJ bean视为定义要用于Spring自动代理的方面。
 *
 * <p>Processing of Spring Advisors follows the rules established in
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}.
 * <p>
 * Spring Advisor的处理遵循{@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}中建立的规则。
 *
 * <p>
 * 对于Aop的实现，基本都是靠{@link AnnotationAwareAspectJAutoProxyCreator}去完成，它可以根据
 * {@code @Point} 注解定义的切点来自动代理相匹配的bean，但是为了简洁配置，Spring使用了自定义配置来
 * 帮助我们自动注册{@link AnnotationAwareAspectJAutoProxyCreator},
 * 其注册过程就是在{@link org.springframework.aop.config.AopConfigUtils#registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry, Object)}
 * 实现的
 * <p>
 * {@link AnnotationAwareAspectJAutoProxyCreator} 间接继承了{@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * 在实现获取增强的方法中，除了保留父类的获取配置文件中定义的增强外，同时还添加了获取Bean的注解增强的功能，
 * 其实现正是由{@code this.aspectJAdvisorsBuilder.buildAspectJAdvisors}来实现的
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.aspectj.annotation.AspectJAdvisorFactory
 * @since 2.0
 */
@SuppressWarnings("serial")
// 支持注释的AspectJ自动代理创建器
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

	@Nullable
	private List<Pattern> includePatterns;

	@Nullable
	private AspectJAdvisorFactory aspectJAdvisorFactory;

	@Nullable
	private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;


	/**
	 * Set a list of regex patterns, matching eligible @AspectJ bean names.
	 * <p>Default is to consider all @AspectJ beans as eligible.
	 */
	public void setIncludePatterns(List<String> patterns) {
		this.includePatterns = new ArrayList<>(patterns.size());
		for (String patternText : patterns) {
			this.includePatterns.add(Pattern.compile(patternText));
		}
	}

	public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
		Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
	}

	@Override
	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		super.initBeanFactory(beanFactory);
		if (this.aspectJAdvisorFactory == null) {
			this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
		}
		this.aspectJAdvisorsBuilder =
				new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
	}


	/**
	 * {@link AnnotationAwareAspectJAutoProxyCreator} 间接继承了{@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
	 * 在实现获取增强的方法中，除了保留父类的获取配置文件中定义的增强外，同时还添加了获取Bean的注解增强的功能，
	 * 其实现正是由{@code this.aspectJAdvisorsBuilder.buildAspectJAdvisors}来实现的
	 *
	 * @return
	 */
	@Override
	protected List<Advisor> findCandidateAdvisors() {
		// Add all the Spring advisors found according to superclass rules.
		// 添加根据超类规则找到的所有Spring顾问程序。(调用父类的获取配置文件中定义的增强)
		/**
		 * 当使用注解方式配置AOP时候，并不是丢弃了对XML配置的支持；
		 * 在这里调用父类方法加载配置文件中的AOP声明
		 */
		List<Advisor> advisors = super.findCandidateAdvisors();
		// Build Advisors for all AspectJ aspects in the bean factory.
		// Bean工厂中所有AspectJ方面的构建顾问。(获取Bean的注解增强的功能，)
		if (this.aspectJAdvisorsBuilder != null) {
			advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
		}
		return advisors;
	}

	@Override
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		// Previously we setProxyTargetClass(true) in the constructor, but that has too
		// broad an impact. Instead we now override isInfrastructureClass to avoid proxying
		// aspects. I'm not entirely happy with that as there is no good reason not
		// to advise aspects, except that it causes advice invocation to go through a
		// proxy, and if the aspect implements e.g the Ordered interface it will be
		// proxied by that interface and fail at runtime as the advice method is not
		// defined on the interface. We could potentially relax the restriction about
		// not advising aspects in the future.
		return (super.isInfrastructureClass(beanClass) ||
				(this.aspectJAdvisorFactory != null && this.aspectJAdvisorFactory.isAspect(beanClass)));
	}

	/**
	 * Check whether the given aspect bean is eligible for auto-proxying.
	 * <p>
	 * 检查给定的方面bean是否符合自动代理的条件。
	 *
	 * <p>If no &lt;aop:include&gt; elements were used then "includePatterns" will be
	 * {@code null} and all beans are included. If "includePatterns" is non-null,
	 * then one of the patterns must match.
	 * <p>
	 * 如果未使用任何<aop：include>元素，则“includePatterns”将为{@code null}并且包括所有bean。
	 * 如果“ includePatterns”不为空，其中中一个模式必须匹配。
	 */
	protected boolean isEligibleAspectBean(String beanName) {
		if (this.includePatterns == null) {
			return true;
		} else {
			for (Pattern pattern : this.includePatterns) {
				if (pattern.matcher(beanName).matches()) {
					return true;
				}
			}
			return false;
		}
	}


	/**
	 * Subclass of BeanFactoryAspectJAdvisorsBuilderAdapter that delegates to
	 * surrounding AnnotationAwareAspectJAutoProxyCreator facilities.
	 * <p>
	 * BeanFactoryAspectJAdvisorsBuilderAdapter的子类，委托给周围的AnnotationAwareAspectJAutoProxyCreator工具。
	 */
	private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

		public BeanFactoryAspectJAdvisorsBuilderAdapter(
				ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {

			super(beanFactory, advisorFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
		}
	}

}
