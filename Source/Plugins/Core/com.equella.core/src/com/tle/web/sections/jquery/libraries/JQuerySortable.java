/*
 * Copyright 2017 Apereo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tle.web.sections.jquery.libraries;

import com.tle.common.i18n.CurrentLocale;
import com.tle.core.javascript.JavascriptModule;
import com.tle.web.DebugSettings;
import com.tle.web.sections.jquery.JQueryLibraryInclude;
import com.tle.web.sections.render.PreRenderable;

@SuppressWarnings("nls")
public class JQuerySortable implements JavascriptModule
{
	private static final long serialVersionUID = 1L;

	/**
	 * Includes ui.mouse
	 */
	public static final PreRenderable PRERENDER = new JQueryLibraryInclude("jquery.ui.sortable.js", JQueryMouse.PRERENDER).hasMin();

	@Override
	public String getDisplayName()
	{
		return CurrentLocale.get("com.tle.web.sections.jquery.modules.sortable.name");
	}

	@Override
	public String getId()
	{
		return "sortable";
	}

	@Override
	public Object getPreRenderer()
	{
		return PRERENDER;
	}
}
