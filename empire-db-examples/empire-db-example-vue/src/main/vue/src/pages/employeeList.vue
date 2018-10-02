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
  <div class="page-content">

    <h1>Employee-List</h1>

    <e-record styleClass="formPanel" :record="filter">
      <table class="inputForm">
        <tr>
          <e-control column="firstName"/>
          <e-control column="lastName"/>
        </tr>
        <tr>
          <e-control column="departmentId"/>
          <e-control column="gender"/>
        </tr>
        <!-- debug
        <tr>
          <td class="eCtlLabel"><label class="eLabel" for="DEPARTMENT_ID">Info:</label></td>
          <td class="eCtlInput">firstname: {{filter.data.firstName}}, lastname: {{filter.data.lastName}}</td>
        </tr>
        -->
        <tr class="formButtonRow">
          <td></td>
          <td class="buttonBar" colspan="3">
            <button @click="doReset()" :disabled="!searchDone">Search reset</button>
            <button @click="doSearch()">Search</button>
          </td>
        </tr>
      </table>
    </e-record>

    <div class="searchResult" v-if="searchDone">
      <h1>Search found {{employeeList.data.length}} Employees</h1>
      <table class="employeeList">
        <colgroup>
          <col class="col-id"/>
          <col class="col-name"/>
          <col class="col-department"/>
          <col class="col-gender"/>
          <col class="col-dateOfBirth"/>
          <col class="col-Retired"/>
        </colgroup>
        <!-- head -->
        <thead>
          <tr>
            <th>ID</th>
            <th><e-label :column="meta.name"/></th>
            <th><e-label :column="meta.department"/></th>
            <th><e-label :column="meta.gender"/></th>
            <th><e-label :column="meta.dateOfBirth"/></th>
            <th>Retired</th>
          </tr>
        </thead>
        <!-- body -->
        <tbody>
        <template v-for="(item, index) in employeeList.data">
          <tr v-bind:key="index" v-bind:class="[index % 2 == 0 ? 'row-even' : 'row-odd' ]">
            <td>{{item.employeeId}}</td>
            <td>
              <router-link class="eLink" :to="{ path: '/employeeDetail/'+item.employeeId }">
                <e-value :column="meta.name" :data="item"/>
              </router-link>
            </td>
            <td><e-value :data="item" :column="meta.department"/></td>
            <td><e-value :data="item" :column="meta.gender"/></td>
            <td><e-value :data="item" :column="meta.dateOfBirth"/></td>
            <td><e-value :data="item" :column="meta.retired"/></td>
          </tr>
        </template>
        </tbody>
      </table>
    </div>

    <div class="buttonBar">
      <button @click="$parent.doLogout()">Logout</button>
      <button @click="doAddNew()">Add new employee</button>
    </div>

  </div>
</template>

<script>
  import EMPAPI from '../api/emp-api'
  import eRecord from '../components/e-record.vue'
  import eControl from '../components/e-control.vue'
  import eInput from '../components/e-input'
  import eLabel from '../components/e-label'
  import eValue from '../components/e-value'
  // import $ from 'jquery'

  export default {
    name: 'employeeList',

    components: {
      eRecord,
      eControl,
      eInput,
      eLabel,
      eValue
    },

    data () {
      return {
        filter: undefined,
        searchDone: false,
        employeeList: undefined
      }
    },

    computed: {
      meta: function () {
        return this.employeeList.meta
      }
    },

    created: function () {
      EMPAPI.assertLoggedIn(this)
      if (this.$parent.employeeFilter) {
        this.filter = this.$parent.employeeFilter
        this.doSearch()
      } else {
        EMPAPI.getEmployeeFilter()
          .done(response => (this.initSearch(response)))
      }
    },

    methods: {
      initSearch: function (response) {
        this.filter = response
        this.searchDone = false
        this.employeeList = undefined
      },
      doReset: function () {
        EMPAPI.debug('resetting search filter')
        EMPAPI.getEmployeeFilter()
          .done(response => (this.initSearch(response)))
        this.$parent.employeeFilter = undefined
      },
      doSearch: function () {
        // Find all employees matching filter criteria
        EMPAPI.debug('load employee list')
        EMPAPI.findEmployees(this.filter.data)
          .done(response => (this.onSearchComplete(response)))
      },
      doAddNew: function () {
        // Add a new employee by passing an employeeId of 0
        this.$router.push('/employeeDetail/0')
      },
      onSearchComplete (result) {
        this.employeeList = result
        this.searchDone = true
        // copy filter data (do not simply assign!)
        this.$parent.employeeFilter = { meta: this.filter.meta, data: Object.assign({}, this.filter.data) }
      }
      /*
      updateValue (event) {
        var inp = $(event.currentTarget)
        var val = inp.val()
        this.$emit('input', { val })
      }
      */
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
