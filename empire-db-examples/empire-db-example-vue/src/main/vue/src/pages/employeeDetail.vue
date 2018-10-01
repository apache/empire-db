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

    <h1>Employee-Details</h1>

    <e-record styleClass="formPanel" :record="employeeRecord">
      <table class="inputForm" style="width:400px">
        <colgroup>
          <col width="120px"/>
          <col/>
        </colgroup>
        <tr><e-control column="salutation" /></tr>
        <tr><e-control column="firstName" /></tr>
        <tr><e-control column="lastName" /></tr>
        <tr><e-control column="dateOfBirth" format="[yyyy-MM-dd]"/></tr>
        <tr><e-control column="departmentId" /></tr>
        <tr><e-control column="gender"   /> </tr>
        <tr><e-control column="phoneNumber" /></tr>
        <tr><e-control column="email" /></tr>
        <tr><e-control column="retired" /></tr>
      </table>
    </e-record>

    <div class="buttonBar">
      <button @click="doReturnToList($event)">Back</button>
      <button @click="doDelete($event)" v-if="allowDelete">Delete</button>
      <button @click="doSave($event)">Save</button>
    </div>

  </div>
</template>

<script>
  import EMPAPI from '../api/emp-api'
  import eRecord from '../components/e-record.vue'
  import eControl from '../components/e-control.vue'

  export default {
    name: 'employeeDetail',

    components: {
      eRecord,
      eControl
    },

    props: {
      employeeId: {
        required: true
      }
    },

    data () {
      return {
        employeeRecord: undefined
      }
    },

    computed: {
      allowDelete: function () {
        return (this.employeeRecord && !this.employeeRecord.data._newRecord)
      }
    },

    created: function (empid) {
      EMPAPI.assertLoggedIn(this)
      if (this.employeeId === 0) {
        this.addNew()
      } else {
        this.loadDetails()
      }
    },

    methods: {
      addNew: function () {
        EMPAPI.debug('create employee record')
        EMPAPI.createEmployeeRecord(this.employeeId)
          .done(response => (this.onLoadDone(response)))
        /*
          .fail(() => this.$router.push('/login'))
        */
      },
      loadDetails: function () {
        EMPAPI.debug('load employee record')
        EMPAPI.readEmployeeRecord(this.employeeId)
          .done(response => (this.onLoadDone(response)))
        /*
          .fail(() => this.$router.push('/login'))
        */
      },
      doSave: function (event) {
        EMPAPI.debug('load employee record')
        EMPAPI.updateEmployee(this.employeeRecord.data)
          .done(response => (this.onUpdateDone(response)))
      },
      doDelete: function (event) {
        if (!confirm('Do you really want to delete this employee?')) {
          return
        }
        EMPAPI.debug('load employee record')
        EMPAPI.deleteEmployee(this.employeeId)
          .done(response => (this.onUpdateDone(response)))
        /*
          .fail(() => this.$router.push('/login'))
        */
      },
      doReturnToList: function (event) {
        this.$router.push('/employeeList')
      },
      onLoadDone (result) {
        this.employeeRecord = result
      },
      onUpdateDone (result) {
        EMPAPI.debug('employee record successfully updated')
        this.$router.push('/employeeList')
      }
    }
  }
</script>
