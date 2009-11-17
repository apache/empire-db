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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * File utilities
 * TODO would be better to let these methods throw IOExceptions!
 *
 */
public class FileUtils {
	
	private static final Log log =  LogFactory.getLog(FileUtils.class);
	
	private FileUtils()
	{
		// Utility class
	}
	
	/**
	 * Recursively cleans (removes) all files under the given 
	 * directory.  Note that this removes all sub-directories
	 * but not the parent directory.  
	 * @param directory
	 */
	public static void cleanDirectory(File directory) {
		boolean success;
		if (directory.isDirectory()) 
		{
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) 
            {
            	File child = new File(directory, children[i]);
            	if (child.isDirectory())
            	{
            		success = deleteDirectory(child);
            	}
            	else 
            	{
            		success = child.delete();
            	}
            	if(!success){
            		// TODO throw IO Exception or return false?
            	}
            }
        }
	}
	
	/**
	 * Recursively deletes a directory and everything under it.
	 * 
	 * @param directory
	 * @return true on success
	 */
	public static boolean deleteDirectory(File directory) {
		boolean globalSuccess = true;
		boolean success;
        if (directory.isDirectory()) 
        {
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) 
            {
            	success = deleteDirectory(new File(directory, children[i]));
            	globalSuccess = success && globalSuccess;
            }
        }   
        // The directory is now empty so delete it
        success = directory.delete();
        globalSuccess = success && globalSuccess;
        return globalSuccess;
	}
	
	/**
	 * Non-recursive delete for all files in the given directory.
	 * Files in sub-directories not deleted.
	 * @param directory
	 */
	// TODO remove as not in use?
	public static void deleteFiles(File directory) {
		if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i=0; i<children.length; i++) {
            	new File(directory, children[i]).delete();
            }
        }
	}
	
	// TODO remove as not in use?
	public static Object readObject(String fileName) {
		Object o = null;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(
				new FileInputStream(fileName));
			o = ois.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			close(ois);
		}
		return o;
	}
	
	// TODO remove as not in use?	
	public static void writeObject(String fileName, Object o) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(
				new FileOutputStream(fileName));
			oos.writeObject(o);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close(oos);
		}
	}
	
	/**
	 * Reads a file to String
	 * 
	 * @param file the file to read
	 * @return the file contents as String
	 */
	// TODO remove as not in use?
	public static String getFileAsString(File file) {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader( new FileReader(file));
			String line;
			while ( (line = br.readLine()) != null) 
			{
				sb.append(line).append("\n");
			}
		} 
		catch (IOException e) 
		{
			log.error(e.getMessage(), e);
		}
		finally
		{
			close(br);
		}
		return sb.toString();
	}

	// TODO remove as not in use?
	public static void writeStringToFile(File file, String contents) {
		BufferedWriter bw = null;
		try {
			if (!file.exists()) 
			{
				file.createNewFile();
			}

			bw = new BufferedWriter(new FileWriter(file));
			bw.write(contents);
		} 
		catch (IOException e) 
		{
			log.error(e.getMessage(), e);
		} 
		finally
		{
			close(bw);
		}

	}

	// TODO remove as not in use?
	public static void replaceAll(File file, String input, String output)
	{
		String fileText = getFileAsString(file);
		StringBuilder sb = new StringBuilder(fileText);
		int index = -1;
		int length = input.length();
		while ( (index = sb.indexOf(input)) != -1 )
		{
			sb.replace(index, index + length, output);
		}
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(
					new FileWriter(file));
			bw.write(sb.toString());
		} 
		catch (IOException e) 
		{
			log.error(e.getMessage(), e);
		}
		finally
		{
			close(bw);
		}
	}
	
	/**
	 * Closes a closeable and logs exceptions
	 * @param closeable
	 */
	public static void close(Closeable closeable)
	{
		if(closeable != null)
		{
			try 
			{
				closeable.close();
			} 
			catch (IOException e) 
			{
				log.warn(e.getMessage());
			}
		}
	}

}
