#!/bin/bash

java -Xss2m -ea -cp "$(dirname $0)/../build/libs/FuzzPEG.jar" \
  i2.act.main.RandomStrings "$@"
