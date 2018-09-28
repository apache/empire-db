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

    <h1>Employee-Details for {{employeeId}}</h1>

    <e-record :record="employeeRecord">
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

    <div class="rdp-weeknavbar">
      <button class="rdp-button" @click="returnToList($event)">Back</button>
      <button @click="saveChanges($event)">Save</button>
    </div>

  </div>
</template>

<script>
  import EMPAPI from '../api/emp-api'
  import eRecord from '../components/e-record.vue'
  import eControl from '../components/e-control.vue'

  export default {
    name: 'details',

    components: {
      eRecord,
      eControl
    },

    data () {
      return {
        employeeId: 0,
        employeeRecord: undefined
      }
    },

    created: function () {
      EMPAPI.assertLoggedIn(this)
      this.employeeId = this.$route.params.employeeId
      this.loadDetails()
    },

    methods: {
      loadDetails: function (event) {
        EMPAPI.debug('load employee record')
        EMPAPI.loadEmployeeRecord(this.employeeId)
          .done(response => (this.onLoadDone(response)))
        /*
          .fail(() => this.$router.push('/login'))
        */
      },
      saveChanges: function (event) {
        EMPAPI.debug('load employee record')
        EMPAPI.updateEmployee(this.employeeRecord.data)
          .done(response => (this.onSaveDone(response)))
      },
      onLoadDone (result) {
        this.employeeRecord = result
      },
      onSaveDone (result) {
        alert('Save OK!')
      },
      returnToList: function (event) {
        this.$router.push('/employeeList')
      }
    }
  }
</script>
