// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import $ from 'jquery'

function isDebug () {
  return process.env.DEBUG_MODE
}

const AJAX = {
  initialized: false,
  baseURL: process.env.EMP_SERVICE_URL,
  // default options
  defaultOptions: {
    xhrFields: {
      withCredentials: true
    },
    statusCode: {
      401: function () {
        // Unauthorized
        EMPAPI.loggedIn = false
      },
      404: function () {
        alert('The requested service is not available!')
      }
    },
    /*
    beforeSend: function (jqXHR) {
      //
      // Add additional header values e.g.
      //   jqXHR.setRequestHeader('X-RDPLAN-????', 'some value')
      //
      EMPAPI.debug('Sensing request!' + this.url)
    },
    */
    error: function (status) {
      var msg = `${status.statusText} (${status.status})`
      if (status.responseText) {
        msg = msg + ': ' + status.responseText
      }
      EMPAPI.debug('Ajax error: ' + msg)
    }
  },
  // init ajax
  setup: function () {
    // set Ajax defaults
    $.ajaxSetup(AJAX.defaultOptions)
    AJAX.initialized = true
  },
  call: function (options) {
    // check initialized
    if (AJAX.initialized !== true) {
      EMPAPI.debug('AJAX not initialized. Initialing now...')
      AJAX.setup()
    }
    // extend the url
    options.url = AJAX.baseURL + options.url
    // extend options
    // alert('URL=' + options.url)
    // options = $.extend({}, AJAX.defaultOptions, options)
    // call
    return $.ajax(options)
  },
  get: function (target) {
    return AJAX.call({type: 'GET', url: target})
  },
  post: function (target, data) {
    return AJAX.call({type: 'POST', url: target, data: data})
  },
  postJSON: function (target, json) {
    return AJAX.call({type: 'POST', url: target, contentType: 'application/json', data: JSON.stringify(json)})
  }
}
// setup
AJAX.setup()

// The EMPAPI
const EMPAPI = {

  loggedIn: true, // assume true

  debug: function (msg) {
    if (isDebug()) {
      alert('Debug: ' + msg)
    } else {
      console.log(msg)
    }
  },

  login: function (username, password) {
    const data = {username, password}
    return AJAX.post('/auth/login', data)
  },

  logout: function () {
    return AJAX.post('/auth/logout')
  },

  loadEmployeeList: function () {
    return AJAX.get(`/employee/list`)
  },

  loadEmployeeDetails: function (employeeId) {
    return AJAX.get(`/employee/get/${employeeId}`)
  },

  updateEmployee: function (employeeData) {
    return AJAX.postJSON('/employee/set', employeeData)
  }
}

// export
export default EMPAPI
