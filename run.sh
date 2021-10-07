#!/bin/bash

java -Xss8m -ea -jar $(dirname $0)/build/libs/FuzzPEG.jar "$@"
