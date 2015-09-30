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
package org.apache.empire.db.eclipse.service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageServiceImpl implements MessageService
{
    private final Locale       locale;

    public static List<Locale> supportedLocales = new ArrayList<Locale>();

    static
    {
        MessageServiceImpl.supportedLocales.add(Locale.ENGLISH);
    }

    public MessageServiceImpl(Locale locale)
    {
        this.locale = locale;
    }

    public String resolveMessageKey(String key)
    {
        return ResourceBundle.getBundle("lang.messages", this.locale).getString(key);
    }

    public String resolveMessageKey(String key, Object... params)
    {
        return MessageFormat.format(resolveMessageKey(key), params);
    }
}
