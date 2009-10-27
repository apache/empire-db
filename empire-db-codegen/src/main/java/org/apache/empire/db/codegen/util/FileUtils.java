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
package org.apache.empire.db.codegen.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileUtils {
	/**
	 * Recursively cleans (removes) all files under the given 
	 * directory.  Note that this removes all sub-directories
	 * but not the parent directory.  
	 * @param directory
	 */
	public static void cleanDirectory(File directory) {
		if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) {
            	File child = new File(directory, children[i]);
            	if (child.isDirectory()) deleteDirectory(child);
            	else child.delete();
            }
        }
	}
	
	/**
	 * Recursively deletes a directory and everything under it.
	 * @param directory
	 */
	public static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) {
            	deleteDirectory(new File(directory, children[i]));
            }
        }   
        // The directory is now empty so delete it
        directory.delete();
	}
	
	/**
	 * Non-recursive delete for all files in the given directory.
	 * Files in sub-directories not deleted.
	 * @param directory
	 */
	public static void deleteFiles(File directory) {
		if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) {
            	new File(directory, children[i]).delete();
            }
        }
	}
	public static Object readObject(String fileName) {
		Object o = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(
				new FileInputStream(fileName));
			o = ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return o;
	}
	
	public static void writeObject(String fileName, Object o) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
				new FileOutputStream(fileName));
			oos.writeObject(o);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String getFileAsString(File file) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(file));
			String line;
			while ( (line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void writeStringToFile(File file, String contents) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter bw;
			bw = new BufferedWriter(new FileWriter(file));
			bw.write(contents);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void replaceAll(File file, String input, String output) {
		String fileText = getFileAsString(file);
		StringBuilder sb = new StringBuilder(fileText);
		int index = -1;
		int length = input.length();
		while ( (index = sb.indexOf(input)) != -1 ) {
			sb.replace(index, index + length, output);
		}
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(
					new FileWriter(file));
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
