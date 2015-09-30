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

import java.sql.Connection;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.eclipse.CodeGenConfig;
import org.apache.empire.db.eclipse.Plugin;
import org.apache.empire.db.eclipse.util.SWTResourceManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MainPage extends Page
{
    private final Text   ctlConfigTitle;
    private final Text   ctlServerUrl;
    private final Text   ctlServerPort;
    private final Text   ctlUser;
    private final Text   ctlPassword;
    private final Text   ctlPasswordConfirm;
    private final Text   ctlSID;

    private final Combo  ctlExistingConfigs;
    private final Combo  ctlDatabaseType;

    private final Group  groupJdbc;

    private final Button btnAdd;
    private final Button btnSave;
    private final Button btnDelete;
    private final Label  lblDatabaseType;
    private final Label  lblServerAddress;
    private final Label  lblServerPort;
    private final Label  lblUser;
    private final Label  lblPassword;
    private final Label  lblPasswordConfirm;
    private final Label  lblSid;
    private final Label  lblConfigurationTitle;
    private final Button btnTestConnection;
    private final Label  lblTestConResult;

    /**
     * Create the composite.
     * 
     * @param parent
     * @param style
     */
    public MainPage(Composite parent, int style)
    {
        super(parent, style);
        setLayout(null);

        Label lblExistingConfigurations = new Label(this, SWT.NONE);
        lblExistingConfigurations.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
        lblExistingConfigurations.setBounds(6, 31, 154, 15);
        lblExistingConfigurations.setText(Plugin.getInstance().getMessageService().resolveMessageKey("existingConfigs"));

        this.ctlExistingConfigs = new Combo(this, SWT.READ_ONLY);
        this.ctlExistingConfigs.setItems(Plugin.getInstance().getConfigFileService().getConfigTitles());
        this.ctlExistingConfigs.setBounds(165, 27, 248, 23);
        this.btnAdd = new Button(this, SWT.NONE);
        this.btnAdd.setBounds(419, 25, 26, 26);
        this.btnAdd.setImage(SWTResourceManager.getImage(MainPage.class, "/icons/addButton.png"));

        this.lblConfigurationTitle = new Label(this, SWT.NONE);
        this.lblConfigurationTitle.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
        this.lblConfigurationTitle.setBounds(6, 62, 154, 15);
        this.lblConfigurationTitle.setText(Plugin.getInstance().getMessageService().resolveMessageKey("configTitle"));

        this.ctlConfigTitle = new Text(this, SWT.BORDER);
        this.ctlConfigTitle.setBounds(165, 59, 216, 21);

        this.btnSave = new Button(this, SWT.NONE);
        this.btnSave.setImage(SWTResourceManager.getImage(MainPage.class, "/icons/save.png"));
        this.btnSave.setBounds(387, 56, 26, 26);

        this.btnDelete = new Button(this, SWT.NONE);
        this.btnDelete.setBounds(419, 56, 26, 26);
        this.btnDelete.setImage(SWTResourceManager.getImage(MainPage.class, "/icons/deleteButton.png"));

        this.groupJdbc = new Group(this, SWT.NONE);
        this.groupJdbc.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.BOLD));
        this.groupJdbc.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbConnection"));
        this.groupJdbc.setBounds(5, 110, 440, 243);

        this.lblDatabaseType = new Label(this.groupJdbc, SWT.NONE);
        this.lblDatabaseType.setBounds(10, 21, 154, 15);
        this.lblDatabaseType.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbConnectionType"));

        this.ctlDatabaseType = new Combo(this.groupJdbc, SWT.READ_ONLY);
        this.ctlDatabaseType.setBounds(180, 18, 250, 23);
        this.ctlDatabaseType.setItems(Plugin.getInstance().getDriverClassNames());

        this.lblServerAddress = new Label(this.groupJdbc, SWT.NONE);
        this.lblServerAddress.setBounds(10, 49, 154, 17);
        this.lblServerAddress.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbServer"));

        this.ctlServerUrl = new Text(this.groupJdbc, SWT.BORDER);
        this.ctlServerUrl.setBounds(180, 46, 250, 21);

        this.lblServerPort = new Label(this.groupJdbc, SWT.NONE);
        this.lblServerPort.setBounds(10, 75, 154, 15);
        this.lblServerPort.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbPort"));

        this.ctlServerPort = new Text(this.groupJdbc, SWT.BORDER);
        this.ctlServerPort.setBounds(180, 72, 78, 21);

        this.ctlSID = new Text(this.groupJdbc, SWT.BORDER);
        this.ctlSID.setBounds(180, 99, 250, 21);

        this.lblSid = new Label(this.groupJdbc, SWT.NONE);
        this.lblSid.setBounds(10, 102, 154, 15);
        this.lblSid.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbSid"));

        this.lblUser = new Label(this.groupJdbc, SWT.NONE);
        this.lblUser.setBounds(10, 129, 154, 15);
        this.lblUser.setText(Plugin.getInstance().getMessageService().resolveMessageKey("jdbcUser"));

        this.ctlUser = new Text(this.groupJdbc, SWT.BORDER);
        this.ctlUser.setBounds(180, 126, 250, 21);

        this.lblPassword = new Label(this.groupJdbc, SWT.NONE);
        this.lblPassword.setBounds(10, 156, 154, 15);
        this.lblPassword.setText(Plugin.getInstance().getMessageService().resolveMessageKey("jdbcPassword"));

        this.ctlPassword = new Text(this.groupJdbc, SWT.BORDER | SWT.PASSWORD);
        this.ctlPassword.setBounds(180, 153, 250, 21);

        this.lblPasswordConfirm = new Label(this.groupJdbc, SWT.NONE);
        this.lblPasswordConfirm.setBounds(10, 183, 154, 15);
        this.lblPasswordConfirm.setText(Plugin.getInstance().getMessageService().resolveMessageKey("jdbcPasswordConfirm"));

        this.ctlPasswordConfirm = new Text(this.groupJdbc, SWT.BORDER | SWT.PASSWORD);
        this.ctlPasswordConfirm.setBounds(180, 180, 250, 21);

        this.btnTestConnection = new Button(this.groupJdbc, SWT.NONE);
        this.btnTestConnection.setImage(SWTResourceManager.getImage(MainPage.class, "/icons/testConnection.gif"));
        this.btnTestConnection.setText(Plugin.getInstance().getMessageService().resolveMessageKey("testConnection"));
        this.btnTestConnection.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.testConnection"));
        this.btnTestConnection.setBounds(10, 207, 166, 26);

        this.lblTestConResult = new Label(this.groupJdbc, SWT.NONE);
        this.lblTestConResult.setBounds(180, 213, 250, 15);
        this.btnTestConnection.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    try
                    {
                        if (!MainPage.this.ctlPassword.getText().equals(MainPage.this.ctlPasswordConfirm.getText()))
                        {
                            MessageDialog.openError(getShell(),
                                                    Plugin.getInstance().getMessageService().resolveMessageKey("dialog.passwordMismatch"),
                                                    Plugin.getInstance().getMessageService()
                                                          .resolveMessageKey("dialog.passwordMismatch.msg"));
                            return;
                        }
                        Connection con = Plugin.getInstance().getJDBCConnection(MainPage.this.ctlDatabaseType.getText(),
                                                                                MainPage.this.ctlServerUrl.getText(),
                                                                                MainPage.this.ctlServerPort.getText(),
                                                                                MainPage.this.ctlSID.getText(),
                                                                                MainPage.this.ctlUser.getText(),
                                                                                MainPage.this.ctlPassword.getText());
                        if (con != null && !con.isClosed())
                        {
                            MainPage.this.lblTestConResult.setText(Plugin.getInstance().getMessageService()
                                                                         .resolveMessageKey("connectionSuccess"));
                            MainPage.this.lblTestConResult.setForeground(new Color(getDisplay(), 0, 255, 0));
                        }
                        else
                        {
                            MainPage.this.lblTestConResult.setText(Plugin.getInstance().getMessageService()
                                                                         .resolveMessageKey("connectionFailed"));
                            MainPage.this.lblTestConResult.setForeground(new Color(getDisplay(), 255, 0, 0));
                        }
                        if (con != null)
                        {
                            con.close();
                        }
                    }
                    catch (Exception e)
                    {
                        MainPage.this.lblTestConResult.setText(Plugin.getInstance().getMessageService()
                                                                     .resolveMessageKey("connectionFailed"));
                        MainPage.this.lblTestConResult.setForeground(new Color(getDisplay(), 255, 0, 0));
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });
    }

    public void save(CodeGenConfig config)
    {
        config.setConfigTitle(this.ctlConfigTitle.getText());
        config.setJdbcServer(this.ctlServerUrl.getText());
        config.setJdbcPort(this.ctlServerPort.getText());
        config.setJdbcSID(this.ctlSID.getText());
        config.setJdbcUser(this.ctlUser.getText());
        config.setJdbcPwd(this.ctlPassword.getText());
        config.setJdbcType(this.ctlDatabaseType.getText());
    }

    public void load(CodeGenConfig config)
    {
        setControlText(this.ctlConfigTitle, config.getConfigTitle());
        setControlText(this.ctlServerUrl, config.getJdbcServer());
        setControlText(this.ctlServerPort, config.getJdbcPort());
        setControlText(this.ctlSID, config.getJdbcSID());
        setControlText(this.ctlUser, config.getJdbcUser());
        setControlText(this.ctlPassword, config.getJdbcPwd());
        this.ctlDatabaseType.setText(!StringUtils.isEmpty(config.getJdbcType()) ? config.getJdbcType() : " ");
        this.lblTestConResult.setText(" ");
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.ctlConfigTitle.setEnabled(enabled);
        this.ctlServerUrl.setEnabled(enabled);
        this.ctlServerPort.setEnabled(enabled);
        this.ctlUser.setEnabled(enabled);
        this.ctlPassword.setEnabled(enabled);
        this.ctlPasswordConfirm.setEnabled(enabled);
        this.ctlSID.setEnabled(enabled);
        this.ctlDatabaseType.setEnabled(enabled);
        this.btnTestConnection.setEnabled(enabled);
    }

    public boolean checkRequiredFields()
    {
        if (!this.ctlPassword.getText().equals(this.ctlPasswordConfirm.getText()))
        {
            MessageDialog.openError(getShell(), Plugin.getInstance().getMessageService().resolveMessageKey("dialog.passwordMismatch"),
                                    Plugin.getInstance().getMessageService().resolveMessageKey("dialog.passwordMismatch.msg"));
            return false;
        }
        return checkControlFilled(this.ctlConfigTitle, this.lblConfigurationTitle.getText())
               && checkControlFilled(this.ctlServerUrl, this.lblServerAddress.getText())
               && checkControlFilled(this.ctlServerPort, this.lblServerPort.getText())
               && checkControlFilled(this.ctlUser, this.lblUser.getText())
               && checkControlFilled(this.ctlPassword, this.lblPassword.getText())
               && checkControlFilled(this.ctlPasswordConfirm, this.lblPasswordConfirm.getText())
               && checkControlFilled(this.ctlSID, this.lblSid.getText());
    }

    public void addExistingConfigsListener(SelectionListener listener)
    {
        this.ctlExistingConfigs.addSelectionListener(listener);
    }

    public void addBtnAddListener(SelectionListener listener)
    {
        this.btnAdd.addSelectionListener(listener);
    }

    public void addBtnSaveListener(SelectionListener listener)
    {
        this.btnSave.addSelectionListener(listener);
    }

    public void addBtnDeleteListener(SelectionListener listener)
    {
        this.btnDelete.addSelectionListener(listener);
    }

    public void setConfigTitleList(String[] configTitleList)
    {
        this.ctlExistingConfigs.setItems(configTitleList);
    }

    @Override
    protected void checkSubclass()
    {
        // Disable the check that prevents subclassing of SWT components
    }
}
