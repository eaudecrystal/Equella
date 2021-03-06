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

package com.tle.core.remoterepo.parser.mods.impl;

import org.w3c.dom.Node;

import com.tle.core.xml.XmlDocument;

/**
 * @author aholland
 */
public class ModsTitleInfo extends ModsPart
{
	ModsTitleInfo(XmlDocument xml, Node context)
	{
		super(xml, xml.node("titleInfo", context));
	}

	String getTitle()
	{
		// TODO: could be multiple titles with different "type" attributes. If
		// only we were using PropBagEX like the rest of the code in EQUELLA,
		// then we could use the LangUtils method to capture a LanguageBundle
		// rather than rewriting it again.
		return xml.nodeValue("title", context);
	}

	String getSubTitle()
	{
		return xml.nodeValue("subTitle", context);
	}
}
