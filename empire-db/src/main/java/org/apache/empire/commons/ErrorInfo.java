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
package org.apache.empire.commons;

/**
 * This interface allows access to the last error that occured on a object.
 * 
 */
public interface ErrorInfo
{
  /**
   * Returns wether or not an error is set.
   * 
   * @return true if an error has been set, or otherwise false
   */
  boolean hasError();

  /**
   * Returns the last error code.
   * 
   * @return the error code of ERR_NONE if no error has been set.
   */
  ErrorType getErrorType();

  /**
   * Returns the parameters used to build the error text.
   * 
   * @return the error message or null if no error has been set.
   */
  Object[] getErrorParams();

  /**
   * Returns the source object's class name that raised the error
   * 
   * @return the error message or null if no error has been set.
   */
  String getErrorSource();

  /**
   * Returns the message associated with the last error.
   *
   * @return the error message or an empty string if no error has been set.
   */
  String getErrorMessage();
}