#!/bin/sh
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
echo "Apache Empire-db Checksum script"
echo "----------------------------"
echo "Generating md5/sha checksum files..."
for ext in "*.zip" "*.bz2" "*.gz"; do
  for filename in `find . -type f -name "$ext"`; do
    md5sum $filename > $filename.md5
    sha1sum $filename > $filename.sha
  done
done
# gpg --print-md MD5 $filename > $filename.md5
# gpg --print-md SHA1 $filename > $filename.sha

echo
echo "Copying rat report..."
cp ../target/rat.txt ./target
echo
echo "All Done. Files available in ./target"
