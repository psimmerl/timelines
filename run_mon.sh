#!/bin/bash

tar -xf ana.tar
export JYPATH=analysis_code
run-groovy run_mon.groovy $*
