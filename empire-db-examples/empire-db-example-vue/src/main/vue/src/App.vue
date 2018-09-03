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

  	<div class="titleDiv">Employee Management Application</div>
    <button class="rdp-button" @click="logout">Logout</button>

    <router-view></router-view>

  </div>
</template>

<script>
  import EMPAPI from './api/emp-api'

  export default {
    name: 'app',

    methods: {
      isLoggedIn: function () {
        if (EMPAPI.loggedIn === undefined) {
          EMPAPI.debug('EMPAPI.loggedIn is undefined!')
          EMPAPI.loggedIn = true // assume true
        }
        return EMPAPI.loggedIn
      },
      logout: function () {
        if (!confirm('Do you really want to logout?')) {
          return
        }
        EMPAPI.logout().done(() => this.onLogoutComplete())
      },
      onLoginComplete: function () {
        EMPAPI.loggedIn = true
        this.$router.push('/list')
      },
      onLogoutComplete: function () {
        EMPAPI.loggedIn = false
        this.$router.push('/login')
      }
    }
  }

</script>

<style>
  @import './assets/css/layout.css';
</style>

</style>
