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

    <form id="loginForm" name="loginForm" enctype="application/x-www-form-urlencoded" @submit.prevent="login">
    <p>Hint: enter any non null username and password.</p>
    <div class="formPanel">
      <table class="inputForm">
        <tr class="formButtonRow">
          <td class="eCtlLabel"><label for="inputUsername">User name</label>:</td>
          <td class="eCtlInput"><input id="inputUsername" name="username" type="text" v-model="username" tabindex="1" /></td>
        </tr>
        <tr class="formButtonRow">
          <td class="eCtlLabel"><label for="inputPassword">Password</label>:</td>
          <td class="eCtlInput"><input id="inputPassword" type="password" placeholder="Password" v-model="password" tabindex="2" />
          </td>
        </tr>
        <tr class="formButtonRow">
          <td></td>
          <td class="buttonBar" colspan="3">
            <button type="submit">Login</button>
          </td>
        </tr>
      </table>
    </div>
    </form>

  </div>
</template>

<script>
  import EMPAPI from '../api/emp-api'

  const defaultUsr = process.env.EMP_DEFAULT_USER
  const defaultPwd = process.env.EMP_DEFAULT_PWD

  export default {
    name: 'userLogin',

    data () {
      return {
        username: defaultUsr,
        password: defaultPwd
      }
    },

    methods: {
      login: function () {
        EMPAPI.login(this.username, this.password)
          .done(() => this.$parent.onLoginComplete())
          .fail(function (response) {
            var msg = 'Der Dienst ist zur Zeit nicht verfügbar.'
            if (response.responseJSON) {
              msg = response.responseJSON.error
            }
            alert(msg)
          })
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
