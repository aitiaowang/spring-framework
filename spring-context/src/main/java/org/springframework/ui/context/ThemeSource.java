/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.ui.context;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects that can resolve {@link Theme Themes}.
 * This enables parameterization and internationalization of messages
 * for a given 'theme'.
 * 由可以解析{@link Theme Themes}的对象实现的接口。这可以为给定的“主题”实现消息的参数化和国际化。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see Theme
 */
public interface ThemeSource {

	/**
	 * Return the Theme instance for the given theme name.
	 * 由可以解析{@link Theme Themes}的对象实现的接口。这可以为给定的“主题”实现消息的参数化和国际化。
	 * <p>The returned Theme will resolve theme-specific messages, codes,
	 * file paths, etc (e.g. CSS and image files in a web environment).
	 * 返回的主题将解析主题特定的消息，代码，文件路径等（例如，网络环境中的CSS和图像文件）。
	 *
	 * @param themeName the name of the theme  主题名称
	 * @return the corresponding Theme, or {@code null} if none defined.
	 * Note that, by convention, a ThemeSource should at least be able to
	 * return a default Theme for the default theme name "theme" but may also
	 * return default Themes for other theme names.
	 * 相应的主题，如果未定义，则为{@code null}。 请注意，按照惯例，
	 * ThemeSource应该至少能够为默认主题名称“theme”返回默认主题，但也可以为其他主题名称返回默认主题。
	 * @see org.springframework.web.servlet.theme.AbstractThemeResolver#ORIGINAL_DEFAULT_THEME_NAME
	 */
	@Nullable
	Theme getTheme(String themeName);

}
