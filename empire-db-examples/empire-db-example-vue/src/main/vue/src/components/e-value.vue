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
<script>
  export default {
    functional: true,
    name: 'e-value',

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

    render (createElement, context) {
      /*
       * function to get the record (if nesessary)
       */
      function _record (context) {
        // find record
        let record = context.props.record
        if (record === undefined) {
          let parent = context.parent
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
      }
      /*
       * function to get the column meta data
       */
      function _meta (column, context) {
        // get column from meta
        if (typeof column === 'string' || column instanceof String) {
          // from record
          return _record(context).meta[column]
        }
        if (column.dataType === undefined) {
          throw new TypeError('e-value: Invalid column param!')
        }
        return column
      }
      /*
       * function to get the raw data value
       */
      function _value (prop, context) {
        // find record
        if (context.props.data === undefined) {
          // get column from meta
          return _record(context).data[prop]
        }
        return context.props.data[prop]
      }
      /*
       * render implementation
       */
      const meta = _meta(context.props.column, context)
      let value = _value(meta.property, context)
      if (meta.options) {
        value = meta.options[value]
      }
      // create span with value
      return createElement('span', {class: 'eVal'}, value)
    }

  }
</script>
