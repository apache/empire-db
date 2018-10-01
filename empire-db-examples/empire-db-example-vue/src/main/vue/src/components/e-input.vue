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
    <template v-if="meta.readOnly">
      <e-value :column="meta" :data="_recordData"/>
    </template>
    <template v-else-if="meta.controlType === 'select'">
      <select :id="'CTL_' + meta.name" :name="meta.name" @change="updateData(inputValue($event))"
              v-bind:class="styleClass('eSelect', meta)" v-bind:disabled="meta.disabled">
        <template v-if="(meta.required === false || isValueEqualTo(null)) && meta.options[''] === undefined">
          <option value="" :selected="isValueEqualTo(null)"></option>
        </template>
        <template v-for="(value, key) in meta.options">
          <option :value="key" :selected="isValueEqualTo(key)">{{value}}</option>
        </template>
      </select>
    </template>
    <template v-else-if="meta.controlType === 'checkbox'">
      <input :id="'CTL_' + meta.name" :name="meta.name" lang="en" type="checkbox"
             v-bind:class="styleClass('eCheckbox', meta)" v-bind:readonly="meta.disabled"
             :checked="dataValue" @input="updateData(checkboxValue($event))">
    </template>
    <template v-else>
      <input :id="'CTL_' + meta.name" :name="meta.name" lang="en" type="text"
             v-bind:class="styleClass('eInpText', meta)" v-bind:readonly="meta.disabled"
             :maxlength="meta.maxLength" :value="dataValue" @input="updateData(inputValue($event))">
    </template>
  </div>
</template>
<script>
  import EMPAPI from '../api/emp-api'
  import eValue from '../components/e-value'
  import $ from 'jquery'

  export default {
    name: 'e-input',

    components: {
      eValue
    },

    props: {
      column: {
        required: true
      },
      record: {
        type: Object
      },
      data: {
        type: Object
      }
    },

    computed: {
      _record: function () {
        // find record
        let record = this.record
        if (record === undefined) {
          let parent = this.$parent
          while (parent) {
            if (parent.record) {
              record = parent.record
              break
            }
            parent = parent.$parent
          }
        }
        // check record
        if (record === undefined) {
          throw new TypeError('e-input: No data or record provided!')
        }
        if (record.meta === undefined) {
          throw new TypeError('e-input: Invalid record param: no meta property!')
        }
        if (record.data === undefined) {
          throw new TypeError('e-input: Invalid record param: no data property!')
        }
        return record
      },
      _recordData: function () {
        if (this.data === undefined) {
          // get column from meta
          return this._record.data
        }
        return this.data
      },
      meta: function () {
        // get column from meta
        if (typeof this.column === 'string' || this.column instanceof String) {
          // from record
          return this._record.meta[this.column]
        }
        if (this.column.dataType === undefined) {
          throw new TypeError('e-input: Invalid column param!')
        }
        return this.column
      },
      dataValue: {
        get: function () {
          // find record
          const prop = this.meta.property
          return this._recordData[prop]
        },
        set: function (value) {
          const prop = this.meta.property
          this._recordData[prop] = value
        }
      }
    },

    /*
    created: function () {
      EMPAPI.debug('Input for ' + this.meta.name + ' created!')
    },
    */

    methods: {
      isValueEqualTo (value) {
        const inp = this.dataValue
        if (value === '') {
          value = null
        }
        return (inp === value)
      },
      styleClass: function (type, meta) {
        var cls = 'eInput ' + type
        cls += ' eType' + meta.dataType
        if (meta.disabled) {
          cls += ' eInpDis'
        }
        if (meta.required) {
          cls += ' eInpReq'
        }
        return cls
      },
      inputValue (event) {
        const inp = $(event.currentTarget)
        return inp.val()
      },
      checkboxValue (event) {
        const cb = $(event.currentTarget)
        // var un_val = cb.attr('data-unchecked');
        return cb.prop('checked')
      },
      updateData (value) {
        // this.$emit('input', val)
        this.dataValue = value
        // debug
        EMPAPI.debug('Value for: "' + this.meta.name + '" has been set to: ' + value)
      }
    }
  }
</script>
