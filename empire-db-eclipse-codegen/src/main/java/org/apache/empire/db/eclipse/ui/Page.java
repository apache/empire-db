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
package org.apache.empire.db.eclipse.ui;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.eclipse.Plugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public abstract class Page extends Composite implements IPage
{
    public Page(Composite parent, int style)
    {
        super(parent, style);
        this.setSize(450, 500);
        this.setBounds(10, 0, 450, 500);
    }

    protected void setControlText(Text ctlText, String value)
    {
        ctlText.setText(value != null ? value : "");
    }

    protected boolean checkControlFilled(Text ctlText, String field)
    {
        if (StringUtils.isNotEmpty(ctlText.getText()))
        {
            return true;
        }
        MessageDialog.openError(getShell(), Plugin.getInstance().getMessageService().resolveMessageKey("dialog.errorFieldRequired"),
                                Plugin.getInstance().getMessageService().resolveMessageKey("dialog.errorFieldRequired.msg", field));
        return false;
    }
}
