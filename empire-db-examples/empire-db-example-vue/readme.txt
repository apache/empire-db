Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

--------------------------------------------------------

In order to run the Vue example please do the following:

1. Start the REST-Service via "Debug on Server" using Tomcat

2. Browse to: 	http://localhost:8080/empvue

3. API-Doc: 	http://localhost:8080/empvue/api/swagger.yaml

----------------------------------------------------

Here is how to to debug and develop the Vue example:

1. Go to the directory "src/main/vue" (or open in WebStorm)

2. Rename file "_eslintrc.js.bak" to ".eslintrc.js" (must start with .)

3. Run the command:	npm install

4. Run the command:	npm run dev

5. Open Url:  http://localhost:8088/
