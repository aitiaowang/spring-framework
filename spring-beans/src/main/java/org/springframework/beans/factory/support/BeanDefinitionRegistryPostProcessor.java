/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * Extension to the standard {@link BeanFactoryPostProcessor} SPI, allowing for
 * the registration of further bean definitions <i>before</i> regular
 * BeanFactoryPostProcessor detection kicks in. In particular,
 * BeanDefinitionRegistryPostProcessor may register further bean definitions
 * which in turn define BeanFactoryPostProcessor instances.
 * <p>
 * 对标准{@link BeanFactoryPostProcessor} SPI的扩展，允许在常规之前在<i>进行进一步的Bean定义的注册BeanFactoryPostProcessor检测开始。
 * 尤其是，BeanDefinitionRegistryPostProcessor可以注册进一步的Bean定义依次定义BeanFactoryPostProcessor实例。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.annotation.ConfigurationClassPostProcessor
 * @since 3.0.1
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean definition registry after its
	 * standard initialization. All regular bean definitions will have been loaded,
	 * but no beans will have been instantiated yet. This allows for adding further
	 * bean definitions before the next post-processing phase kicks in.
	 * <p>
	 * 在标准初始化之后，修改应用程序上下文的内部bean定义注册表。所有常规bean定义都将已加载，但尚未实例化任何bean。
	 * 这允许在下一个后处理阶段开始之前添加更多的bean定义。
	 *
	 * @param registry the bean definition registry used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}
