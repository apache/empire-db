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
  <div class="eInpWrap">
    <template v-if="column.options">
      <select :id="'CTL_' + column.name" :name="column.name" class="eInput eTypeSelect" @change="updateValue($event)">
        <template v-if="column.required === false && column.options[''] === undefined">
          <option value="" :selected="inputValue === null"></option>
        </template>
        <template v-for="(value, key) in column.options">
          <option :value="key" :selected="inputValue === key">{{value}}</option>
        </template>
      </select>
    </template>
    <template v-else>
      <input :id="'CTL_' + column.name" :name="column.name" class="eInput eTypeText" lang="en" type="text" :maxlength="column.maxLength" :value="inputValue" @input="updateValue($event)">
    </template>
  </div>
</template>
<script>
  import EMPAPI from '../api/emp-api'
  import $ from 'jquery'

  export default {
    name: 'e-input',

    props: {
      column: {
        required: true
      },
      data: {
        required: true
      }
    },

    computed: {
      inputValue: function () {
        return this.data[this.column.property]
      }
    },

    created: function () {
      // alert('column=' + this.column.name + ' is ' + this.data[this.column.property])
    },

    methods: {
      updateValue (event) {
        var inp = $(event.currentTarget)
        var val = inp.val()
        // this.$emit('input', val)
        this.data[this.column.property] = val
        // debug
        EMPAPI.debug('Value for: "' + this.column.name + '" has been set to: ' + this.data[this.column.property])
      }
    }
  }
</script>
