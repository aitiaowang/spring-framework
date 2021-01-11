/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.EventListener;

/**
 * Interface to be implemented by application event listeners.
 * Based on the standard {@code java.util.EventListener} interface
 * for the Observer design pattern.
	 * 由应用程序事件侦听器实现的接口。基于观察者设计模式的标准{@code java.util.EventListener}接口。
 *
 *
 * <p>As of Spring 3.0, an ApplicationListener can generically declare the event type
 * that it is interested in. When registered with a Spring ApplicationContext, events
 * will be filtered accordingly, with the listener getting invoked for matching event
 * objects only.
 * 从Spring 3.0开始，ApplicationListener可以一般性地声明它感兴趣的事件类型。
 * 在Spring ApplicationContext中注册后，将相应地过滤事件，并且仅针对匹配事件对象调用侦听器。
 *
 *
 * @param <E> the specific ApplicationEvent subclass to listen to
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.event.ApplicationEventMulticaster
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * 处理应用程序事件
	 *
	 * @param event the event to respond to  事件响应
	 */
	void onApplicationEvent(E event);

}
