/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.atlassian.plugin.PluginAccessor;

/**
 * Global Artifactory server management form action
 *
 * @author Noam Y. Tenne
 */
public class JfrogConfigAction extends GlobalAdminAction implements GlobalAdminSecurityAware {

	private ServerConfigManager serverConfigManager;

	private PluginAccessor pluginAccessor;

	public JfrogConfigAction(ServerConfigManager serverConfigManager, PluginAccessor pluginAccessor) {
		this.serverConfigManager = serverConfigManager;
		this.pluginAccessor = pluginAccessor;
	}

	public boolean isMissedMigration() {
		pluginAccessor.getEnabledPlugins();
		return serverConfigManager.isMissedMigration();
	}
}
