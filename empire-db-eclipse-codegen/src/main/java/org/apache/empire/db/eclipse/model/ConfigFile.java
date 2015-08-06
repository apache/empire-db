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
package org.apache.empire.db.eclipse.model;

import org.apache.empire.db.eclipse.CodeGenConfig;

public class ConfigFile {

	private String uuid;

	private String filename;
	
	private CodeGenConfig codeGenConfig;
	
	public ConfigFile(CodeGenConfig codeGenConfig) {
		this.codeGenConfig = codeGenConfig;
	}
	
	public ConfigFile(String filename, String uuid, CodeGenConfig pluginCodeGenConfig) {
		this.filename = filename;
		this.uuid = uuid;
		this.codeGenConfig = pluginCodeGenConfig;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public CodeGenConfig getCodeGenConfig() {
		return codeGenConfig;
	}

	public void setCodeGenConfig(CodeGenConfig codeGenConfig) {
		this.codeGenConfig = codeGenConfig;
	}
}
