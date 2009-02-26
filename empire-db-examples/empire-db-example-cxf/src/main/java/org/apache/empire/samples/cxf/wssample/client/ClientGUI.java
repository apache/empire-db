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

package org.apache.empire.samples.cxf.wssample.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.empire.samples.cxf.wssample.common.Department;
import org.apache.empire.samples.cxf.wssample.common.Employee;

public class ClientGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JButton _btnSearch = null;
	private JTextField _txtSearchValue = null;
	private EmployeeManagementProxy proxy = null;
	private JList _searchResult = null;
	private EmployeeListModel _employeeListModel = null;
	private ClientGUI me;
	private JPanel _actionPanel = null;
	private JPanel _pnlContent = null;
	private JButton _btnAdd = null;
	private JTextField _txtName = null;
	private JTextField _txtLastname = null;
	private JLabel _lblName = null;
	private JLabel lblLastname = null;
	private JComboBox _cbxDepartment = null;
	private JLabel _lblDepartment = null;
	private List<Department> _departments = new ArrayList<Department>();

	/**
	 * This method initializes _actionPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel get_actionPanel() {
		if (_actionPanel == null) {
			_actionPanel = new JPanel();
			_actionPanel.setLayout(new BorderLayout());
			_actionPanel.add(get_btnSearch(), BorderLayout.EAST);
			_actionPanel.add(get_txtSearchValue(), BorderLayout.CENTER);
		}
		return _actionPanel;
	}

	/**
	 * This method initializes _pnlContent
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel get_pnlContent() {
		if (_pnlContent == null) {
			_lblDepartment = new JLabel();
			_lblDepartment.setText("department");
			_lblDepartment.setSize(new Dimension(100, 20));
			_lblDepartment.setLocation(new Point(10, 200));
			_lblDepartment.setPreferredSize(new Dimension(100, 20));
			lblLastname = new JLabel();
			lblLastname.setPreferredSize(new Dimension(100, 20));
			lblLastname.setLocation(new Point(280, 200));
			lblLastname.setSize(new Dimension(100, 20));
			lblLastname.setText("last Name");
			_lblName = new JLabel();
			_lblName.setText("first Name");
			_lblName.setSize(new Dimension(100, 20));
			_lblName.setPreferredSize(new Dimension(100, 20));
			_lblName.setLocation(new Point(120, 200));
			_pnlContent = new JPanel();
			_pnlContent.setLayout(null);
			_pnlContent.add(get_searchResult(), null);
			_pnlContent.add(get_btnAdd(), null);
			_pnlContent.add(get_txtName(), null);
			_pnlContent.add(get_txtLastname(), null);
			_pnlContent.add(_lblName, null);
			_pnlContent.add(lblLastname, null);
			_pnlContent.add(get_cbxDepartment(), null);
			_pnlContent.add(_lblDepartment, null);
		}
		return _pnlContent;
	}

	/**
	 * This method initializes _btnAdd
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton get_btnAdd() {
		if (_btnAdd == null) {
			_btnAdd = new JButton();
			_btnAdd.setLocation(new Point(434, 225));
			_btnAdd.setText("add");
			_btnAdd.setPreferredSize(new Dimension(75, 20));
			_btnAdd.setSize(new Dimension(75, 20));
			_btnAdd.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					try {
						Employee emp = proxy.createEmmployee();
						emp.setFirstname(_txtName.getText());
						emp.setLastname(_txtLastname.getText());
						emp.setDepartmentId(_departments.get(
								_cbxDepartment.getSelectedIndex())
								.getDepartmentId());
						proxy.saveEmployee(emp);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(me, ex.getMessage());
					}
				}
			});
		}
		return _btnAdd;
	}

	/**
	 * This method initializes _txtName
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField get_txtName() {
		if (_txtName == null) {
			_txtName = new JTextField();
			_txtName.setLocation(new Point(120, 225));
			_txtName.setPreferredSize(new Dimension(150, 20));
			_txtName.setSize(new Dimension(150, 20));
		}
		return _txtName;
	}

	/**
	 * This method initializes _txtLastname
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField get_txtLastname() {
		if (_txtLastname == null) {
			_txtLastname = new JTextField();
			_txtLastname.setLocation(new Point(280, 225));
			_txtLastname.setPreferredSize(new Dimension(150, 20));
			_txtLastname.setSize(new Dimension(150, 20));
		}
		return _txtLastname;
	}

	/**
	 * This method initializes _cbxDepartment
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox get_cbxDepartment() {
		if (_cbxDepartment == null) {
			_cbxDepartment = new JComboBox(_departments.toArray());
			_cbxDepartment.setLocation(new Point(10, 225));
			_cbxDepartment.setPreferredSize(new Dimension(100, 20));
			_cbxDepartment.setSize(new Dimension(100, 20));
		}
		return _cbxDepartment;
	}

	/**
	 * This is the default constructor
	 */
	public ClientGUI(EmployeeManagementProxy prxy) {
		super();
		proxy = prxy;
		try {
		    _departments = proxy.getDepartments();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(me, ex.getMessage());
        }
		initialize();
		me = this;
		setVisible(true);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(529, 299);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setContentPane(getJContentPane());
		this.setTitle("Employee Management Client");
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(get_actionPanel(), BorderLayout.NORTH);
			jContentPane.add(get_pnlContent(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes _btnSearch
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton get_btnSearch() {
		if (_btnSearch == null) {
			_btnSearch = new JButton();
			_btnSearch.setText("search");
			_btnSearch.setPreferredSize(new Dimension(80, 20));
			_btnSearch.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					search();
				}
			});
		}
		return _btnSearch;
	}

	/**
	 * This method initializes _txtSearchValue
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField get_txtSearchValue() {
		if (_txtSearchValue == null) {
			_txtSearchValue = new JTextField();
			_txtSearchValue
					.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							search();
						}

					});
		}
		return _txtSearchValue;
	}

	private void search() {
		try {
			List<Employee> list = null;
			// Search for Employees with the given information
			list = proxy.searchEmployee(null,_txtSearchValue.getText(),"",null);
			_employeeListModel.setListData(list);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(me, ex.getMessage());
		}
	}

	/**
	 * This method initializes _searchResult
	 * 
	 * @return javax.swing.JList
	 */
	private JList get_searchResult() {
		if (_searchResult == null) {
			_employeeListModel = new EmployeeListModel();
			_searchResult = new JList(_employeeListModel);
			_searchResult.setSize(new Dimension(500, 180));
			_searchResult.setLocation(new Point(10, 10));
		}
		return _searchResult;
	}

} // @jve:decl-index=0:visual-constraint="10,10"
