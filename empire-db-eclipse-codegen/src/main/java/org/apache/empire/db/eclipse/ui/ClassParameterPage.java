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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ClassParameterPage extends Page
{
    private final Text   ctlDbClassName;
    private final Text   ctlTableBaseName;
    private final Text   ctlViewBaseName;
    private final Text   ctlRecordBaseName;
    private final Text   ctlTableNamePrefix;
    private final Text   ctlTableClassPrefix;
    private final Text   ctlTableClassSuffix;
    private final Text   ctlViewNamePrefix;
    private final Text   ctlViewClassPrefix;
    private final Text   ctlViewClassSuffix;
    private final Text   ctlColumnNamePrefix;
    private final Label  lblClassParameter;
    private final Label  lblTableBaseName;
    private final Label  lblViewBaseName;
    private final Label  lblRecordBaseName;
    private final Label  lblDbClassName;

    private final Button ctlNestTables;
    private final Button ctlNestViews;
    private final Button ctlCreateRecordProperties;
    private final Button ctlPreserverCharCase;
    private final Button ctlPreserveRelationNames;

    public ClassParameterPage(Composite parent, int style)
    {
        super(parent, style);

        this.lblDbClassName = new Label(this, SWT.NONE);
        this.lblDbClassName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("dbClassName"));
        this.lblDbClassName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbClassName"));
        this.lblDbClassName.setBounds(15, 50, 145, 17);

        this.ctlDbClassName = new Text(this, SWT.BORDER);
        this.ctlDbClassName.setBounds(185, 47, 250, 21);
        this.ctlDbClassName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.dbClassName"));

        this.lblClassParameter = new Label(this, SWT.NONE);
        this.lblClassParameter.setAlignment(SWT.CENTER);
        this.lblClassParameter.setText(Plugin.getInstance().getMessageService().resolveMessageKey("classParameter"));
        this.lblClassParameter.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.BOLD));
        this.lblClassParameter.setBounds(0, 10, 418, 21);

        this.lblTableBaseName = new Label(this, SWT.NONE);
        this.lblTableBaseName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("tableBaseName"));
        this.lblTableBaseName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableBaseName"));
        this.lblTableBaseName.setBounds(15, 76, 145, 17);

        this.ctlTableBaseName = new Text(this, SWT.BORDER);
        this.ctlTableBaseName.setBounds(185, 73, 250, 21);
        this.ctlTableBaseName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableBaseName"));

        this.lblViewBaseName = new Label(this, SWT.NONE);
        this.lblViewBaseName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("viewBaseName"));
        this.lblViewBaseName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewBaseName"));
        this.lblViewBaseName.setBounds(15, 102, 145, 17);

        this.ctlViewBaseName = new Text(this, SWT.BORDER);
        this.ctlViewBaseName.setBounds(185, 99, 250, 21);
        this.ctlViewBaseName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewBaseName"));

        this.lblRecordBaseName = new Label(this, SWT.NONE);
        this.lblRecordBaseName.setText(Plugin.getInstance().getMessageService().resolveMessageKey("recordBaseName"));
        this.lblRecordBaseName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.recordBaseName"));
        this.lblRecordBaseName.setBounds(15, 128, 145, 17);

        this.ctlRecordBaseName = new Text(this, SWT.BORDER);
        this.ctlRecordBaseName.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.recordBaseName"));
        this.ctlRecordBaseName.setBounds(185, 125, 250, 21);

        Label lblTableNamePrefix = new Label(this, SWT.NONE);
        lblTableNamePrefix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("tableNamePrefix"));
        lblTableNamePrefix.setBounds(15, 154, 145, 17);
        lblTableNamePrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableNamePrefix"));

        this.ctlTableNamePrefix = new Text(this, SWT.BORDER);
        this.ctlTableNamePrefix.setBounds(185, 151, 250, 21);
        this.ctlTableNamePrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableNamePrefix"));

        Label lblTableClassPrefix = new Label(this, SWT.NONE);
        lblTableClassPrefix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("tableClassPrefix"));
        lblTableClassPrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableClassPrefix"));
        lblTableClassPrefix.setBounds(15, 180, 145, 17);

        this.ctlTableClassPrefix = new Text(this, SWT.BORDER);
        this.ctlTableClassPrefix.setBounds(185, 177, 250, 21);
        this.ctlTableClassPrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableClassPrefix"));

        Label lblTableClassSuffix = new Label(this, SWT.NONE);
        lblTableClassSuffix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("tableClassSuffix"));
        lblTableClassSuffix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableClassSuffix"));
        lblTableClassSuffix.setBounds(15, 206, 145, 17);

        this.ctlTableClassSuffix = new Text(this, SWT.BORDER);
        this.ctlTableClassSuffix.setBounds(185, 203, 250, 21);
        this.ctlTableClassSuffix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.tableClassSuffix"));

        Label lblViewNamePrefix = new Label(this, SWT.NONE);
        lblViewNamePrefix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("viewNamePrefix"));
        lblViewNamePrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewNamePrefix"));
        lblViewNamePrefix.setBounds(15, 232, 145, 17);

        this.ctlViewNamePrefix = new Text(this, SWT.BORDER);
        this.ctlViewNamePrefix.setBounds(185, 229, 250, 21);
        this.ctlViewNamePrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewNamePrefix"));

        Label lblViewClassPrefix = new Label(this, SWT.NONE);
        lblViewClassPrefix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("viewClassPrefix"));
        lblViewClassPrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewClassPrefix"));
        lblViewClassPrefix.setBounds(15, 258, 145, 17);

        this.ctlViewClassPrefix = new Text(this, SWT.BORDER);
        this.ctlViewClassPrefix.setBounds(185, 255, 250, 21);
        this.ctlViewClassPrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewClassPrefix"));

        Label lblViewClassSuffix = new Label(this, SWT.NONE);
        lblViewClassSuffix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("viewClassSuffix"));
        lblViewClassSuffix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewClassSuffix"));
        lblViewClassSuffix.setBounds(15, 285, 145, 17);

        this.ctlViewClassSuffix = new Text(this, SWT.BORDER);
        this.ctlViewClassSuffix.setBounds(185, 282, 250, 21);
        this.ctlViewClassSuffix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.viewClassSuffix"));

        Label lblColumnNamePrefix = new Label(this, SWT.NONE);
        lblColumnNamePrefix.setText(Plugin.getInstance().getMessageService().resolveMessageKey("columnNamePrefix"));
        lblColumnNamePrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.columnNamePrefix"));
        lblColumnNamePrefix.setBounds(15, 312, 145, 17);

        this.ctlColumnNamePrefix = new Text(this, SWT.BORDER);
        this.ctlColumnNamePrefix.setBounds(185, 309, 250, 21);
        this.ctlColumnNamePrefix.setToolTipText(Plugin.getInstance().getMessageService().resolveMessageKey("tooltip.columnNamePrefix"));

        Group grpFlag = new Group(this, SWT.NONE);
        grpFlag.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.BOLD));
        grpFlag.setText(Plugin.getInstance().getMessageService().resolveMessageKey("flagGroup"));
        grpFlag.setBounds(5, 335, 440, 144);

        this.ctlNestTables = new Button(grpFlag, SWT.CHECK);
        this.ctlNestTables.setBounds(180, 22, 93, 16);

        Label lblNestTables = new Label(grpFlag, SWT.NONE);
        lblNestTables.setBounds(10, 23, 156, 15);
        lblNestTables.setText(Plugin.getInstance().getMessageService().resolveMessageKey("nestTables"));

        Label lblNestViews = new Label(grpFlag, SWT.NONE);
        lblNestViews.setText(Plugin.getInstance().getMessageService().resolveMessageKey("nestViews"));
        lblNestViews.setBounds(10, 45, 156, 15);

        this.ctlNestViews = new Button(grpFlag, SWT.CHECK);
        this.ctlNestViews.setBounds(180, 44, 93, 16);

        Label lblCreateRecordProperties = new Label(grpFlag, SWT.NONE);
        lblCreateRecordProperties.setText(Plugin.getInstance().getMessageService().resolveMessageKey("createRecordProperties"));
        lblCreateRecordProperties.setBounds(10, 67, 156, 15);

        this.ctlCreateRecordProperties = new Button(grpFlag, SWT.CHECK);
        this.ctlCreateRecordProperties.setBounds(180, 66, 93, 16);

        Label lblPreserverCharCase = new Label(grpFlag, SWT.NONE);
        lblPreserverCharCase.setText(Plugin.getInstance().getMessageService().resolveMessageKey("preserverCharCase"));
        lblPreserverCharCase.setBounds(10, 89, 156, 15);

        this.ctlPreserverCharCase = new Button(grpFlag, SWT.CHECK);
        this.ctlPreserverCharCase.setBounds(180, 88, 93, 16);

        Label lblPreserveRelationNames = new Label(grpFlag, SWT.NONE);
        lblPreserveRelationNames.setText(Plugin.getInstance().getMessageService().resolveMessageKey("preserveRelationNames"));
        lblPreserveRelationNames.setBounds(10, 111, 156, 15);

        this.ctlPreserveRelationNames = new Button(grpFlag, SWT.CHECK);
        this.ctlPreserveRelationNames.setBounds(180, 110, 93, 16);
    }

    public void save(CodeGenConfig config)
    {
        config.setDbClassName(this.ctlDbClassName.getText());
        config.setTableBaseName(this.ctlTableBaseName.getText());
        config.setViewBaseName(this.ctlViewBaseName.getText());
        config.setRecordBaseName(this.ctlRecordBaseName.getText());
        config.setTableNamePrefix(this.ctlTableNamePrefix.getText());
        config.setTableClassPrefix(this.ctlTableClassPrefix.getText());
        config.setTableClassSuffix(this.ctlTableClassSuffix.getText());
        config.setViewClassPrefix(this.ctlViewClassPrefix.getText());
        config.setViewClassSuffix(this.ctlViewClassSuffix.getText());
        config.setColumnNamePrefix(this.ctlColumnNamePrefix.getText());
        config.setNestTables(this.ctlNestTables.getSelection());
        config.setNestViews(this.ctlNestViews.getSelection());
        config.setCreateRecordProperties(this.ctlCreateRecordProperties.getSelection());
        config.setPreserverCharacterCase(this.ctlPreserverCharCase.getSelection());
        config.setPreserveRelationNames(this.ctlPreserveRelationNames.getSelection());
    }

    public void load(CodeGenConfig config)
    {
        setControlText(this.ctlDbClassName, config.getDbClassName());
        setControlText(this.ctlTableBaseName, config.getTableBaseName());
        setControlText(this.ctlViewBaseName, config.getViewBaseName());
        setControlText(this.ctlRecordBaseName, config.getRecordBaseName());
        setControlText(this.ctlTableNamePrefix, config.getTableNamePrefix());
        setControlText(this.ctlTableClassPrefix, config.getTableClassPrefix());
        setControlText(this.ctlTableClassSuffix, config.getTableClassSuffix());
        setControlText(this.ctlViewClassPrefix, config.getViewClassPrefix());
        setControlText(this.ctlViewClassSuffix, config.getViewClassSuffix());
        setControlText(this.ctlColumnNamePrefix, config.getColumnNamePrefix());
        this.ctlNestTables.setSelection(config.isNestTables());
        this.ctlNestViews.setSelection(config.isNestViews());
        this.ctlCreateRecordProperties.setSelection(config.isCreateRecordProperties());
        this.ctlPreserverCharCase.setSelection(config.isPreserverCharacterCase());
        this.ctlPreserveRelationNames.setSelection(config.isPreserveRelationNames());
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.ctlDbClassName.setEnabled(enabled);
        this.ctlTableBaseName.setEnabled(enabled);
        this.ctlViewBaseName.setEnabled(enabled);
        this.ctlRecordBaseName.setEnabled(enabled);
        this.ctlTableNamePrefix.setEnabled(enabled);
        this.ctlTableClassPrefix.setEnabled(enabled);
        this.ctlTableClassSuffix.setEnabled(enabled);
        this.ctlViewNamePrefix.setEnabled(enabled);
        this.ctlViewClassPrefix.setEnabled(enabled);
        this.ctlViewClassSuffix.setEnabled(enabled);
        this.ctlColumnNamePrefix.setEnabled(enabled);
        this.ctlNestTables.setEnabled(enabled);
        this.ctlNestViews.setEnabled(enabled);
        this.ctlCreateRecordProperties.setEnabled(enabled);
        this.ctlPreserverCharCase.setEnabled(enabled);
        this.ctlPreserveRelationNames.setEnabled(enabled);
    }

    public boolean checkRequiredFields()
    {
        return checkControlFilled(this.ctlDbClassName, this.lblDbClassName.getText())
               && checkControlFilled(this.ctlTableBaseName, this.lblTableBaseName.getText())
               && checkControlFilled(this.ctlViewBaseName, this.lblViewBaseName.getText())
               && this.checkControlFilled(this.ctlRecordBaseName, this.lblRecordBaseName.getText());
    }
}
