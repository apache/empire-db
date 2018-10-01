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
  <label v-if="forInput" class="eLabel" :for="'CTL_' + meta.name">{{meta.title}}:<span v-if="meta.required" class="required">*</span></label>
  <span v-else class="eLabel" >{{meta.title}}</span>
</template>
<script>
  export default {
    name: 'e-label',

    props: {
      column: {
        required: true
      },
      record: {
        type: Object
      },
      forInput: {
        type: Boolean,
        default: false
      }
    },

    computed: {
      meta: function () {
        // get column from meta
        if (typeof this.column === 'string' || this.column instanceof String) {
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
            throw new TypeError('e-label: No data or record provided!')
          }
          if (record.meta === undefined) {
            throw new TypeError('e-label: Invalid record param: no meta property!')
          }
          // find in record.meta
          return record.meta[this.column]
        }
        // column provided directly
        if (this.column.dataType === undefined) {
          throw new TypeError('e-label: Invalid column param!')
        }
        return this.column
      }
    }
  }
</script>
