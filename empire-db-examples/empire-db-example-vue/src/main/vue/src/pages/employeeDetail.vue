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

    <table class="inputForm" v-if="record" style="width:400px">
      <colgroup>
        <col width="120px"/>
        <col/>
      </colgroup>
      <tr><e-control :data="record.data" :column="record.meta.salutation" /></tr>
      <tr><e-control :data="record.data" :column="record.meta.firstName" /></tr>
      <tr><e-control :data="record.data" :column="record.meta.lastName" /></tr>
      <tr><e-control :data="record.data" :column="record.meta.dateOfBirth" format="[yyyy-MM-dd]"/></tr>
      <tr><e-control :data="record.data" :column="record.meta.departmentId" /></tr>
      <tr><e-control :data="record.data" :column="record.meta.gender"   /> </tr>
      <tr><e-control :data="record.data" :column="record.meta.phoneNumber" /></tr>
      <tr><e-control :data="record.data" :column="record.meta.email" /></tr>
      <tr><e-control :data="record.data" :column="record.meta.retired" /></tr>
    </table>

    <div class="rdp-weeknavbar">
      <button class="rdp-button" @click="showList($event)">Back</button>
      <button @click="saveChanges($event)">Save</button>
    </div>

  </div>
</template>

<script>
  import EMPAPI from '../api/emp-api'
  import eControl from '../components/e-control.vue'

  export default {
    name: 'details',

    components: {
      eControl
    },

    data () {
      return {
        employeeId: 0,
        record: {}
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
          .done(response => (this.record = response))
        /*
        this.datum = this.$route.params.datum
        this.bereichId = this.$route.params.bereichId
        RDPAPI.loadHints(this.datum, this.bereichId)
          .done(response => (this.info = response))
          .fail(() => this.$router.push('/login'))
        */
      },
      saveChanges: function (event) {
        EMPAPI.debug('load employee record')
        EMPAPI.updateEmployee(this.record.data)
          .done(response => (this.setResult(response)))
      },
      setResult (result) {
        alert('Save OK!')
      },
      showList: function (event) {
        this.$router.push('/employeeList')
      }
    }
  }
</script>
