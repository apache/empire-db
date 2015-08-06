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

import org.apache.empire.db.eclipse.CodeGenConfig;
import org.apache.empire.db.eclipse.Plugin;
import org.apache.empire.db.eclipse.util.SWTResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SchemaOptionsPage extends Page
{
    private final Text   ctlDbCatalog;
    private final Text   ctlDbSchema;
    private final Text   ctlTimestampCol;
    private final Text   ctlDbTablePattern;

    private final Text   ctlTargetFolder;
    private final Text   ctlPackageName;
    private final Text   ctlTablePackageName;
    private final Text   ctlViewPackageName;
    private final Text   ctlRecordPackageName;

    private final Group  groupRequired;

    private final Label  lblTargetFolder;
    private final Label  lblPackageName;
    private final Label  lblTablePackageName;
    private final Label  lblViewPackageName;
    private final Label  lblRecordPackageName;
    private final Button btnDirectoryDialog;
    private final Button btnOpenTableDialog;

    public SchemaOptionsPage(Composite parent, int style)
    {
        super(parent, style);
        setLayout(null);

        Group grpSchemaOptions = new Group(this, SWT.NONE);
        grpSchemaOptions.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.BOLD));
        grpSchemaOptions.setText(Plugin.getInstance().getMessageService().resolveMessageKey("schemaOptions"));
        grpSchemaOptions.setBounds(5, 30, 440, 128);

        Label lblDbCatalog = new Label(grpSchemaOptions, SWT.NONE);
        lblDbCatalog.setBounds(10, 23, 154, 17);
        lblDbCatalog.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbCatalog"));
        lblDbCatalog.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbCatalog"));

        this.ctlDbCatalog = new Text(grpSchemaOptions, SWT.BORDER);
        this.ctlDbCatalog.setBounds(180, 20, 250, 21);
        this.ctlDbCatalog.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbCatalog"));

        Label lblDbSchema = new Label(grpSchemaOptions, SWT.NONE);
        lblDbSchema.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbSchema"));
        lblDbSchema.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbSchema"));
        lblDbSchema.setBounds(10, 49, 154, 17);

        this.ctlDbSchema = new Text(grpSchemaOptions, SWT.BORDER);
        this.ctlDbSchema.setBounds(180, 46, 250, 21);
        this.ctlDbSchema.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbSchema"));

        Label lblDbTablePattern = new Label(grpSchemaOptions, SWT.NONE);
        lblDbTablePattern.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbTablePattern"));
        lblDbTablePattern.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbTablePattern"));
        lblDbTablePattern.setBounds(10, 75, 154, 17);

        this.ctlDbTablePattern = new Text(grpSchemaOptions, SWT.BORDER);
        this.ctlDbTablePattern.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbTablePattern"));
        this.getCtlDbTablePattern().setBounds(180, 72, 218, 21);

        Label lblTimestampCol = new Label(grpSchemaOptions, SWT.NONE);
        lblTimestampCol.setText(Plugin.getInstance().getMessageService().resolveMessageKey("timestampColumn"));
        lblTimestampCol.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.timestampColumn"));
        lblTimestampCol.setBounds(10, 98, 154, 17);

        this.ctlTimestampCol = new Text(grpSchemaOptions, SWT.BORDER);
        this.ctlTimestampCol.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.timestampColumn"));
        this.ctlTimestampCol.setBounds(180, 98, 250, 21);

        this.btnOpenTableDialog = new Button(grpSchemaOptions, SWT.NONE);
        this.btnOpenTableDialog.setBounds(404, 69, 26, 26);
        this.btnOpenTableDialog.setImage(SWTResourceManager.getImage(SchemaOptionsPage.class, "/icons/db_element.gif"));

        this.groupRequired = new Group(this, SWT.NONE);
        this.groupRequired.setText(Plugin.getInstance().getMessageService().resolveMessageKey("requiredParams"));
        this.groupRequired.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.BOLD));
        this.groupRequired.setBounds(5, 180, 440, 149);

        this.lblTargetFolder = new Label(this.groupRequired, SWT.NONE);
        this.lblTargetFolder.setBounds(10, 18, 154, 15);
        this.lblTargetFolder.setText(Plugin.getInstance().getMessageService().resolveMessageKey("targetFolder"));
        this.lblTargetFolder.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.targetFolder"));
        
        this.ctlTargetFolder = new Text(this.groupRequired, SWT.BORDER);
        this.ctlTargetFolder.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.targetFolder"));
        this.ctlTargetFolder.setBounds(180, 15, 218, 21);
        

        this.btnDirectoryDialog = new Button(this.groupRequired, SWT.NONE);
        this.btnDirectoryDialog.setImage(SWTResourceManager.getImage(SchemaOptionsPage.class, "/icons/folder.gif"));
        this.btnDirectoryDialog.setBounds(404, 12, 26, 26);
        this.btnDirectoryDialog.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
                    directoryDialog.setFilterPath(SchemaOptionsPage.this.ctlTargetFolder.getText());
                    String path = directoryDialog.open();
                    if (path != null)
                    {
                        SchemaOptionsPage.this.ctlTargetFolder.setText(path);
                    }
                }

                public void widgetDefaultSelected(SelectionEvent arg0)
                {
                }
            });

        this.lblPackageName = new Label(this.groupRequired, SWT.NONE);
        this.lblPackageName.setBounds(10, 45, 154, 15);
        this.lblPackageName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("packageName"));
        this.lblPackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.packageName"));
        
        this.ctlPackageName = new Text(this.groupRequired, SWT.BORDER);
        this.ctlPackageName.setBounds(180, 42, 250, 21);
        this.ctlPackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.packageName"));
        
        this.lblTablePackageName = new Label(this.groupRequired, SWT.NONE);
        this.lblTablePackageName.setBounds(10, 72, 154, 15);
        this.lblTablePackageName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("tablePackageName"));
        this.lblTablePackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tablePackageName"));
        
        this.ctlTablePackageName = new Text(this.groupRequired, SWT.BORDER);
        this.ctlTablePackageName.setBounds(180, 69, 250, 21);
        this.ctlTablePackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tablePackageName"));

        this.lblViewPackageName = new Label(this.groupRequired, SWT.NONE);
        this.lblViewPackageName.setBounds(10, 99, 154, 15);
        this.lblViewPackageName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("viewPackageName"));
        this.lblViewPackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewPackageName"));
        
        this.ctlViewPackageName = new Text(this.groupRequired, SWT.BORDER);
        this.ctlViewPackageName.setBounds(180, 96, 250, 21);
        this.ctlViewPackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewPackageName"));

        this.lblRecordPackageName = new Label(this.groupRequired, SWT.NONE);
        this.lblRecordPackageName.setBounds(10, 126, 154, 15);
        this.lblRecordPackageName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("recordPackageName"));
        this.lblRecordPackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.recordPackageName"));
        
        this.ctlRecordPackageName = new Text(this.groupRequired, SWT.BORDER);
        this.ctlRecordPackageName.setBounds(180, 123, 250, 21);
        this.ctlRecordPackageName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.recordPackageName"));
    }

    public void save(CodeGenConfig config)
    {
        config.setDbCatalog(this.ctlDbCatalog.getText());
        config.setDbSchema(this.ctlDbSchema.getText());
        config.setTimestampColumn(this.ctlTimestampCol.getText());
        config.setDbTablePattern(this.getCtlDbTablePattern().getText());
        config.setTargetFolder(this.ctlTargetFolder.getText());
        config.setPackageName(this.ctlPackageName.getText());
        config.setTablePackageName(this.ctlTablePackageName.getText());
        config.setViewPackageName(this.ctlViewPackageName.getText());
        config.setRecordPackageName(this.ctlRecordPackageName.getText());
    }

    public void load(CodeGenConfig config)
    {
        setControlText(this.ctlDbCatalog, config.getDbCatalog());
        setControlText(this.ctlDbSchema, config.getDbSchema());
        setControlText(this.ctlTimestampCol, config.getTimestampColumn());
        setControlText(this.getCtlDbTablePattern(), config.getDbTablePattern());
        setControlText(this.ctlTargetFolder, config.getTargetFolder());
        setControlText(this.ctlPackageName, config.getPackageName());
        setControlText(this.ctlTablePackageName, config.getTablePackageName());
        setControlText(this.ctlViewPackageName, config.getViewPackageName());
        setControlText(this.ctlRecordPackageName, config.getRecordPackageName());

    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.ctlDbCatalog.setEnabled(enabled);
        this.ctlDbSchema.setEnabled(enabled);
        this.ctlTimestampCol.setEnabled(enabled);
        this.getCtlDbTablePattern().setEnabled(enabled);
        this.ctlTargetFolder.setEnabled(enabled);
        this.ctlPackageName.setEnabled(enabled);
        this.ctlTablePackageName.setEnabled(enabled);
        this.ctlViewPackageName.setEnabled(enabled);
        this.ctlRecordPackageName.setEnabled(enabled);
        this.btnDirectoryDialog.setEnabled(enabled);
        this.btnOpenTableDialog.setEnabled(enabled);
    }

    public boolean checkRequiredFields()
    {
        return checkControlFilled(this.ctlTargetFolder, this.lblTargetFolder.getText())
               && checkControlFilled(this.ctlPackageName, this.lblPackageName.getText())
               && checkControlFilled(this.ctlTablePackageName, this.lblTablePackageName.getText())
               && checkControlFilled(this.ctlViewPackageName, this.lblViewPackageName.getText())
               && checkControlFilled(this.ctlRecordPackageName, this.lblRecordPackageName.getText());
    }

    public void addBtnTableDialogListener(SelectionListener selectionListener)
    {
        this.btnOpenTableDialog.addSelectionListener(selectionListener);
    }

    public Text getCtlDbTablePattern()
    {
        return this.ctlDbTablePattern;
    }
}
