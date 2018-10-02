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
  <div id="app">

    <div class="titleDiv">
      <span>Employee Management Application</span>
      <button class="rdp-button" @click="doLogout" v-if="this.isLoggedIn()">Logout</button>
    </div>

    <div id="messages" v-if="messages">
      <ul>
        <template v-for="item in messages">
          <li>{{item}}</li>
        </template>
      </ul>
    </div>

    <router-view></router-view>

  </div>
</template>

<script>
  import Vue from 'vue'
  import EMPAPI from './api/emp-api'

  export default {
    name: 'app',

    data () {
      return {
        employeeFilter: undefined,
        messages: undefined,
        redirectWithError: false
      }
    },

    created: function () {
      this.startup()
    },

    watch: {
      $route (to, from) {
        if (this.redirectWithError) {
          this.redirectWithError = false
        } else {
          this.clearMessages()
        }
      }
    },

    methods: {
      startup: function () {
        EMPAPI.debug('Application startup! Vue version is ' + Vue.version)
      },
      clearData: function () {
        this.employeeFilter = undefined
      },
      clearMessages: function () {
        this.messages = undefined
      },
      addMessages: function (msgs) {
        if (this.messages) {
          this.messages = this.messages.concat(msgs)
        } else {
          this.messages = msgs
        }
      },
      redirect: function (target) {
        this.clearMessages()
        this.$router.push(target)
      },
      handleError: function (error, target) {
        // redirect to target
        if (target) {
          this.redirectWithError = true
          this.$router.push(target)
        }
        // set message
        if (error.responseJSON) {
          this.addMessages(error.responseJSON)
        } else {
          this.addMessages([ error.statusText ])
        }
      },
      isLoggedIn: function () {
        if (EMPAPI.loggedIn === undefined) {
          EMPAPI.debug('EMPAPI.loggedIn is undefined!')
          EMPAPI.loggedIn = true // assume true
        }
        return EMPAPI.loggedIn
      },
      doLogout: function () {
        if (!confirm('Do you really want to logout?')) {
          return
        }
        EMPAPI.logout().done(() => this.onLogoutComplete())
      },
      onLoginComplete: function () {
        EMPAPI.loggedIn = true
        this.clearData()
        this.$router.push('/employeeList')
      },
      onLogoutComplete: function () {
        EMPAPI.loggedIn = false
        this.clearData()
        this.$router.push('/login')
      }
    }
  }

</script>

<style>
  @import './assets/css/layout.css';
</style>

</style>
