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
  // import $ from 'jquery'
  import Label from './e-label.vue'
  import Input from './e-input.vue'

  export default {
    functional: true,
    name: 'e-control',

    props: {
      column: {
        required: true
      },
      data: {
        required: true
      }
    },
    render (createElement, context) {
      const label = createElement('td', {class: 'eCtlLabel'}
        , [ createElement(Label, { props: Object.assign({column: context.props.column}) }) ])
      const input = createElement('td', {class: 'eCtlInput'}
        , [ createElement(Input, { props: context.props }) ])
      /*
      const input = createElement('td', {class: 'eCtlInput'}
        , [ createElement('input', {
          on: { input: e => context.data.on.input(e.target.value) },
          domProps: { value: context.props.value },
          attrs: { name: 'TEST' }}) ])
      */
      return [label, input]
    }

  }
</script>
