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
package org.apache.empire.db.codegen;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.codegen.util.FileUtils;
import org.junit.After;
import org.junit.Test;

public class CodeGenTest
{
	@After
	public void cleanup(){
		File generated = new File("target/generated");
		FileUtils.deleteDirectory(generated);
	}

    @Test
    public void testCodeGen()
    {
        CodeGenConfig config = new CodeGenConfig();
        config.init("testconfig.xml", true);
        CodeGenWriter codeGen = new CodeGenWriter(config);
        
        DBDatabase db = new DBDatabase()
        {
        };
        
        List<File> files = codeGen.generateCodeFiles(db);
        assertEquals(4, files.size());
        for(File file:files){
        	System.out.println(file);
        }
        
        // TODO try to compile the resulting files???
        
        // Commons jci 1.0 seems not able to handle java 5
//        List<String> arguments = new ArrayList<String>();
//        String strFile;
//        for(File file:files){
//        	strFile = file.getPath().replace(config.getTargetFolder()+"/", "");
//            arguments.add(strFile);
//            System.out.println(strFile);
//        }
//        JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");
//        JavaCompilerSettings settings = new JavaCompilerSettings();
//        settings.setSourceVersion("1.5");
//        settings.setTargetVersion("1.5");
//        settings.setVerbose(true);
//        CompilationResult result = compiler.compile(
//        		arguments.toArray(new String[arguments.size()]), 
//        		new FileResourceReader(new File(config.getTargetFolder())), 
//        		new FileResourceStore(new File("target/compiledsources.test")),
//        		getClass().getClassLoader(),
//        		settings);
//
//        System.out.println( result.getErrors().length + " errors");
//        for(CompilationProblem problem:result.getErrors()){
//        	System.out.println( problem);
//        }
//        System.out.println( result.getWarnings().length + " warnings");
//        for(CompilationProblem warning:result.getWarnings()){
//        	System.out.println( warning);
//        }
        
        // ONLY for java 6
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        arguments.add("-classpath");
//        arguments.add(System.getProperty("java.class.path"));
//        for(File file:files){
//            System.out.println(file);
//            arguments.add(file.getAbsolutePath());
//        }
//
//        int compilationResult = compiler.run(null, null, null, arguments.toArray(new String[arguments.size()]));
//        assertEquals("Compilation Failed", 0, compilationResult);
        
    }

}
