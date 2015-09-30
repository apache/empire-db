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

import org.apache.empire.db.eclipse.Plugin;
import org.apache.empire.db.eclipse.PluginConsts;
import org.apache.empire.db.eclipse.model.ConfigFile;
import org.apache.empire.db.eclipse.util.SWTResourceManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ConfigurationDialog extends TitleAreaDialog
{
    private Navigator navigator;

    private Page      visiblePage;

    private Button    btnNextPage;

    private Button    btnPreviousPage;

    private Button    btnOk;

    private Button    btnCancel;

    public ConfigurationDialog(Shell shell)
    {
        super(shell);
    }

    @Override
    public void create()
    {
        super.create();
        UpdateControls();
    }

    // overriding this methods allows you to set the
    // title of the custom dialog
    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dialog.title"));
    }

    @Override
    protected Point getInitialSize()
    {
        return new Point(470, 685);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        this.btnPreviousPage = createButton(parent, PluginConsts.BUTTON_PREVIOUS_ID, Plugin.getInstance().getMessageService()
                                                                                           .resolveMessageKey("dialog.back"), true);
        this.btnPreviousPage.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    ConfigurationDialog.this.navigator.showPreviousPage();
                    UpdateControls();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });
        this.btnNextPage = createButton(parent, PluginConsts.BUTTON_NEXT_ID,
                                        Plugin.getInstance().getMessageService().resolveMessageKey("dialog.next"), true);
        this.btnNextPage.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    ConfigurationDialog.this.navigator.showNextPage();
                    UpdateControls();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

        this.btnOk = createButton(parent, PluginConsts.BUTTON_OK, Plugin.getInstance().getMessageService().resolveMessageKey("dialog.ok"),
                                  false);
        this.btnOk.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    if (checkRequiredFields())
                    {
                        saveAction();
                        setReturnCode(Window.OK);
                        close();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
        this.btnCancel = createButton(parent, PluginConsts.BUTTON_CANCEL,
                                      Plugin.getInstance().getMessageService().resolveMessageKey("dialog.close"), false);
        this.btnCancel.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    save();
                    setReturnCode(Window.CANCEL);
                    close();
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        setTitleImage(SWTResourceManager.getImage(ConfigurationDialog.class, "/icons/empire-db-logo.gif"));
        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(null);
        final MainPage mainPage = new MainPage(area, SWT.NONE);
        mainPage.addExistingConfigsListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    if (event.widget instanceof Combo)
                    {
                        Combo combo = (Combo) event.widget;
                        save();
                        load(Plugin.getInstance().getConfigFileService().getConfig(combo.getText()));
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });

        mainPage.addBtnAddListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    save();
                    load(Plugin.getInstance().getConfigFileService().getDefaultConfig());
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {

                }
            });
        mainPage.addBtnSaveListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    saveAction();
                    mainPage.setConfigTitleList(Plugin.getInstance().getConfigFileService().getConfigTitles());
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
        mainPage.addBtnDeleteListener(new SelectionListener()
            {

                public void widgetSelected(SelectionEvent arg0)
                {
                    if (MessageDialog.openConfirm(getShell(), Plugin.getInstance().getMessageService().resolveMessageKey("dialog.delete"),
                                                  Plugin.getInstance().getMessageService().resolveMessageKey("dialog.delete.msg")))
                    {
                        deleteConfig();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
        this.visiblePage = mainPage;
        this.navigator = new Navigator();
        this.navigator.addPage(this.visiblePage);
        final SchemaOptionsPage schemaOptions = new SchemaOptionsPage(area, SWT.NONE);
        schemaOptions.addBtnTableDialogListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    ConfigurationDialog.this.navigator.save();
                    DBTablesDialog dbTablesDialog = new DBTablesDialog(getShell(), ConfigurationDialog.this.navigator.getCurrentConfig()
                                                                                                                     .getCodeGenConfig());
                    if (dbTablesDialog.open() == Window.OK)
                    {
                        schemaOptions.getCtlDbTablePattern().setText(dbTablesDialog.getSelectedTables());
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
        this.navigator.addPage(schemaOptions);
        ClassParameterPage classParameterPage = new ClassParameterPage(area, SWT.NONE);
        this.navigator.addPage(classParameterPage);
        this.navigator.setPagesEnabled(false);
        
        setHelpAvailable(false);
        return area;
    }

    private void UpdateControls()
    {
        this.btnPreviousPage.setVisible(this.navigator.hasPreviousPage());
        this.btnNextPage.setVisible(this.navigator.hasNextPage());
    }

    public void save()
    {
        if (ConfigurationDialog.this.navigator.getCurrentConfig() != null
            && MessageDialog.openQuestion(getShell(), Plugin.getInstance().getMessageService().resolveMessageKey("dialog.save"),
                                          Plugin.getInstance().getMessageService().resolveMessageKey("dialog.save.msg")))
        {
            saveAction();
        }
    }

    public void saveAction()
    {
        ConfigFile configFile = this.navigator.save();
        if (configFile != null)
        {
            Plugin.getInstance().getConfigFileService().saveConfig(configFile);
        }
    }

    public void load(ConfigFile config)
    {
        this.navigator.load(config);
    }

    public void deleteConfig()
    {
        Plugin.getInstance().getConfigFileService().deleteConfig(this.navigator.getCurrentConfig().getUuid());
        this.navigator.load(null);
    }

    public boolean hasConfig()
    {
        return this.navigator.getCurrentConfig() != null;
    }

    public String getConfigFileAbsolutPath()
    {
        return Plugin.getInstance().getConfigFileService().getConfigFilePath(this.navigator.getCurrentConfig().getUuid());
    }

    private boolean checkRequiredFields()
    {
        return this.navigator.checkRequiredFields();
    }
}
