/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db.eclipse.handler;

import org.apache.empire.db.codegen.CodeGenerator;
import org.apache.empire.db.eclipse.Plugin;
import org.apache.empire.db.eclipse.ui.ConfigurationDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConfigDialogHandler extends AbstractHandler
{
    public Object execute(ExecutionEvent event)
        throws ExecutionException
    {
    	// loads the configurations
        Plugin.getInstance().getConfigFileService().refreshConfigList();
        // create new window
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        ConfigurationDialog dialog = new ConfigurationDialog(window.getShell());
        if (dialog.open() == Window.OK)
        {
        	// start the class cration
            CodeGenerator.main(new String[] { dialog.getConfigFileAbsolutPath() });
        }
        return null;
    }
}
