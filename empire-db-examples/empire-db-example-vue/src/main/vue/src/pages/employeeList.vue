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

    <div class="formPanel" v-if="filter">
      <!--
      <div style="border:1px red solid">
        <e-input :column="meta.FIRSTNAME" :data="filter"/>
      </div>
      -->
      <table class="inputForm">
      <tr>
        <e-control :column="filter.meta.firstName" :data="filter.data"/>
        <e-control :column="filter.meta.lastName"  :data="filter.data"/>
      </tr>
      <tr>
        <e-control :column="filter.meta.departmentId" :data="filter.data"/>
        <e-control :column="filter.meta.gender"  :data="filter.data"/>
      </tr>
      <tr>
        <td class="eCtlLabel"><label class="eLabel" for="DEPARTMENT_ID">Info:</label></td>
        <td class="eCtlInput">
          firstname: {{filter.data.firstName}}, lastname: {{filter.data.lastName}}
          <!--
          <e-input column="test" :value="filter.lastname"/>
            <input v-model="filter.lastname"/>
          -->
          </td>
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
            <th>ID</th>
            <th>{{employeeList.meta['name'].title}}</th>
            <th>{{employeeList.meta['department'].title}}</th>
            <th>{{employeeList.meta['gender'].title}}</th>
            <th>{{employeeList.meta['dateOfBirth'].title}}</th>
            <th>Retired</th>
          </tr>
        </thead>

        <tbody>
        <template v-for="(item, index) in employeeList.data">
          <tr v-bind:key="index" v-bind:class="[index % 2 == 0 ? 'row-even' : 'row-odd' ]">
            <td>{{item.employeeId}}</td>
            <td>
              <router-link class="eLink" :to="{ path: '/employeeDetail/'+item.employeeId }">
                {{item.name}}
              </router-link>
            </td>
            <td>{{item.department}}</td>
            <td><e-value :column="employeeList.meta.gender" :data="item"/></td>
            <td><e-value :column="employeeList.meta.dateOfBirth" :data="item"/></td>
            <td><e-value :column="employeeList.meta.retired" :data="item"/></td>
          </tr>
        </template>
        </tbody>
      </table>
      <h1>Employee-count is {{employeeList.data.length}}</h1>
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
  import eControl from '../components/e-control.vue'
  import eInput from '../components/e-input'
//  import eLabel from '../components/e-label'
  import eValue from '../components/e-value'
  import $ from 'jquery'

  export default {
    name: 'list',

    components: {
      eControl,
      eInput,
//    eLabel,
      eValue
    },

    data () {
      return {
        filter: undefined,
        searchDone: false,
        employeeList: undefined
      }
    },

    created: function () {
      EMPAPI.assertLoggedIn(this)
      if (this.$parent.employeeFilter) {
        this.filter = this.$parent.employeeFilter
        this.doSearch()
      } else {
        EMPAPI.loadEmployeeFilter()
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
        EMPAPI.loadEmployeeFilter()
          .done(response => (this.initSearch(response)))
        this.$parent.employeeFilter = undefined
      },
      doSearch: function () {
        // alert('firstname: ' + this.filter.data.firstName + ' lastname: ' + this.filter.data.lastName)
        EMPAPI.debug('load employee list')
        EMPAPI.loadEmployeeList(this.filter.data)
          .done(response => (this.setResult(response)))
      },
      setResult (result) {
        this.employeeList = result
        this.searchDone = true
        this.$parent.employeeFilter = this.filter
      },
      doSomething: function () {
        EMPAPI.debug('list contains ' + this.employeeList.data.length + ' items')
      },
      updateValue (event) {
        var inp = $(event.currentTarget)
        var val = inp.val()
        this.$emit('input', { val })
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
