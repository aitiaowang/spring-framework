/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context;

/**
 * Strategy interface for processing Lifecycle beans within the ApplicationContext.
 * 用于在ApplicationContext中处理Lifecycle bean的策略接口。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
// 生命周期处理器
public interface LifecycleProcessor extends Lifecycle {

	/**
	 * Notification of context refresh, e.g. for auto-starting components.
	 * <p>
	 * 上下文刷新的通知，例如用于自动启动组件。
	 * <p>
	 * 启动所有实现了Lifecycle接口的bean
	 */
	void onRefresh();

	/**
	 * Notification of context close phase, e.g. for auto-stopping components.
	 * 上下文关闭阶段的通知，例如用于自动停止组件。
	 */
	void onClose();

}
