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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects that can manage a number of
 * {@link ApplicationListener} objects, and publish events to them.
 * <p>
 * 由可以管理多个{@link ApplicationListener}对象并向其发布事件的对象实现的接口。
 *
 * <p>An {@link org.springframework.context.ApplicationEventPublisher}, typically
 * a Spring {@link org.springframework.context.ApplicationContext}, can use an
 * ApplicationEventMulticaster as a delegate for actually publishing events.
 * <p>
 * 通常是Spring {@link org.springframework.context.ApplicationContext}的
 * {@link org.springframework.context.ApplicationEventPublisher}可以将ApplicationEventMulticaster用作实际发布事件的委托。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public interface ApplicationEventMulticaster {

	/**
	 * Add a listener to be notified of all events.
	 * <p>
	 * 添加一个侦听器以通知所有事件。
	 *
	 * @param listener the listener to add
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * Add a listener bean to be notified of all events.
	 *
	 * @param listenerBeanName the name of the listener bean to add
	 */
	void addApplicationListenerBean(String listenerBeanName);

	/**
	 * Remove a listener from the notification list.
	 *
	 * @param listener the listener to remove
	 */
	void removeApplicationListener(ApplicationListener<?> listener);

	/**
	 * Remove a listener bean from the notification list.
	 *
	 * @param listenerBeanName the name of the listener bean to add
	 */
	void removeApplicationListenerBean(String listenerBeanName);

	/**
	 * Remove all listeners registered with this multicaster.
	 * <p>After a remove call, the multicaster will perform no action
	 * on event notification until new listeners are being registered.
	 */
	void removeAllListeners();

	/**
	 * Multicast the given application event to appropriate listeners.
	 * <p>
	 * 将给定的应用程序事件多播到适当的侦听器。
	 *
	 * <p>Consider using {@link #multicastEvent(ApplicationEvent, ResolvableType)}
	 * if possible as it provides a better support for generics-based events.
	 * <p>
	 * 如果可能，请考虑使用{@link #multicastEvent(ApplicationEvent, ResolvableType)}，因为它为基于泛型的事件提供了更好的支持。
	 *
	 * @param event the event to multicast
	 */
	void multicastEvent(ApplicationEvent event);

	/**
	 * Multicast the given application event to appropriate listeners.
	 * <p>
	 * 将给定的应用程序事件多播到适当的侦听器。
	 *
	 * <p>If the {@code eventType} is {@code null}, a default type is built
	 * based on the {@code event} instance.
	 * <p>
	 * 如果{@code eventType}为{@code null}，则将基于{@code event}实例构建默认类型。
	 *
	 * @param event     the event to multicast
	 * @param eventType the type of event (can be null)
	 * @since 4.2
	 */
	void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}
