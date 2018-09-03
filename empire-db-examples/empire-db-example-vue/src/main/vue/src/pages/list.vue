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

    <table class="employeeList">
      <colgroup>
        <col class="col-id"/>
        <col class="col-vname"/>
        <col class="col-nname"/>
        <col class="col-datum"/>
      </colgroup>

      <thead>
      <tr>
        <th>Ident</th>
        <th>Vorname</th>
        <th>Nachname</th>
        <th>Geburtsdatum</th>
      </tr>
      </thead>

      <tbody>
      <template v-for="(item, index) in employeeList">
        <tr v-bind:key="index" v-bind:class="[index % 2 == 0 ? 'row-even' : 'row-odd' ]">
          <td>
            <router-link class="hint-tile" :to="{ path: '/details/'+item.employeeId.value }">
              {{item.lastName.value}}, {{item.firstName.value}} ({{item.employeeId.value}})
            </router-link>
          </td>
          <td>{{item.firstName.value}}</td>
          <td>{{item.lastName.value}}</td>
          <td>{{item.dateOfBirth.value}}</td>
        </tr>
      </template>
      </tbody>

      <h1>Employee-count is {{employeeList.length}}</h1>

      <!--
      <button class="rdp-button" @click="doSomething">Do Something</button>
      -->

    </table>

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
      this.loadList()
    },

    data () {
      return {
        loggedIn: true,
        employeeList: {}
      }
    },

    methods: {
      loadList: function () {
        EMPAPI.debug('load employee list')
        EMPAPI.loadEmployeeList()
          .done(response => (this.employeeList = response))
          .fail(() => this.$router.push('/login'))
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
