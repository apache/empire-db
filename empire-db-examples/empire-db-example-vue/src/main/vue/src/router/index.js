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
import Vue from 'vue'
import Router from 'vue-router'
import userLogin from '@/pages/login'
import employeeList from '@/pages/employeeList'
import employeeDetail from '@/pages/employeeDetail'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/login',
      component: userLogin
    },
    {
      path: '/employeeList',
      component: employeeList
    },
    {
      path: '/employeeDetail/:employeeId',
      component: employeeDetail,
      props: (route) => {
        const employeeId = Number.parseInt(route.params.employeeId)
        if (Number.isNaN(employeeId)) {
          return 0
        }
        return { employeeId }
      }
    },
    {
      path: '*',
      redirect: '/login'
    }
  ]
})
