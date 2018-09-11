<!--
      Licensed to the Apache Software Foundation (ASF) under one or more
      contributor license agreements. See the NOTICE file distributed with
      this work for additional information regarding copyright ownership.
      The ASF licenses this file to You under the Apache License, Version
      2.0 (the "License"); you may not use this file except in compliance
      with the License. You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0 Unless required by
      applicable law or agreed to in writing, software distributed under the
      License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
      CONDITIONS OF ANY KIND, either express or implied. See the License for
      the specific language governing permissions and limitations under the
      License.
  -->
<template>
  <div class="rdp-content">

    <h1>Employee-List</h1>

    <div class="formPanel">
      <table class="inputForm">
        <tr>
          <td class="eCtlLabel"><label class="eLabel" for="FIRST_NAME">Firstname:</label></td>
          <td class="eCtlInput"><input name="FIRST_NAME" class="eInput eTypeText" id="FIRST_NAME" lang="en" type="text" maxlength="40" value=""></td>
          <td class="eCtlLabel"><label class="eLabel" for="LAST_NAME">Lastname:</label></td>
          <td class="eCtlInput"><input name="LAST_NAME" class="eInput eTypeText" id="LAST_NAME" lang="en" type="text" maxlength="40" value=""></td>
        </tr>
        <tr>
          <td class="eCtlLabel"><label class="eLabel" for="DEPARTMENT_ID">Department:</label></td>
          <td class="eCtlInput"><select name="DEPARTMENT_ID" class="eInput eTypeNumber" id="DEPARTMENT_ID" size="1">	<option selected="selected" value=""></option>	<option value="1">Procurement</option>	<option value="2">Development</option>	<option value="3">Sales</option></select></td>
          <td class="eCtlLabel"><label class="eLabel" for="GENDER">Gender:</label></td>
          <td class="eCtlInput"><select name="GENDER" class="eInput eTypeText" id="GENDER" size="1">	<option selected="selected" value=""></option>	<option value="M">Male</option>	<option value="F">Female</option></select></td>
        </tr>
        <tr class="formButtonRow">
          <td></td>
          <td class="buttonBar" colspan="3">
            <button @click="doReset()" :disabled="!searchDone">Search reset</button>
            <button @click="doSearch()">Search</button>
          </td>
        </tr>
      </table>
    </div>

    <div class="searchResult" v-if="searchDone">
      <table class="employeeList">
        <colgroup>
          <col class="col-id"/>
          <col class="col-name"/>
          <col class="col-department"/>
          <col class="col-gender"/>
          <col class="col-dateOfBirth"/>
          <col class="col-Retired"/>
        </colgroup>

        <thead>
          <tr>
            <th>Id</th>
            <th>Name</th>
            <th>Department</th>
            <th>Gender</th>
            <th>Date of birth</th>
            <th>Retired</th>
          </tr>
        </thead>

        <tbody>
        <template v-for="(item, index) in employeeList">
          <tr v-bind:key="index" v-bind:class="[index % 2 == 0 ? 'row-even' : 'row-odd' ]">
            <td>{{item.employeeId.value}}</td>
            <td>
              <router-link class="eLink" :to="{ path: '/employeeDetail/'+item.employeeId.value }">
                {{item.lastName.value}}, {{item.firstName.value}} ({{item.employeeId.value}})
              </router-link>
            </td>
            <td>{{item.firstName.value}}</td>
            <td>{{item.lastName.value}}</td>
            <td>{{item.dateOfBirth.value}}</td>
            <td>?</td>
          </tr>
        </template>
        </tbody>
      </table>
      <h1>Employee-count is {{employeeList.length}}</h1>
    </div>


    <!--
    <div class="rdp-weeknavbar">
      <button class="rdp-button" @click="weekOffset -= 1">&lt</button>
      <button class="rdp-button" @click="weekOffset = 0">heute</button>
      <button class="rdp-button" @click="weekOffset += 1">&gt</button>
    </div>

    <div class="rdp-weekinfo">
      <strong>KW {{info.kw}}</strong> vom <strong>{{info.von}}</strong> bis <strong>{{info.bis}}</strong>
      <span> / </span>
      <strong>{{info.name}}</strong>
    </div>

    <rdp-plan :week-offset="weekOffset"></rdp-plan>
    -->

  </div>
</template>

<script>
  import EMPAPI from '../api/emp-api'

  export default {
    name: 'list',

    components: {
    },

    created: function () {
      EMPAPI.assertLoggedIn(this)
      this.initSearch()
    },

    data () {
      return {
        loggedIn: true,
        searchDone: false,
        employeeList: {}
      }
    },

    methods: {
      initSearch: function () {
        this.searchDone = false
      },
      doReset: function () {
        this.employeeList = {}
        this.searchDone = false
      },
      doSearch: function () {
        EMPAPI.debug('load employee list')
        EMPAPI.loadEmployeeList()
          .done(response => (this.setResult(response)))
      },
      setResult (result) {
        this.employeeList = result
        this.searchDone = true
      },
      doSomething: function () {
        EMPAPI.debug('list contains ' + this.employeeList.length + ' items')
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>