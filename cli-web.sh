#!/bin/bash

usage(){
    echo "Usage: start.sh ( commands ... )"
    echo "commands: "
    echo "  jars        copy dependencies to target"
    echo "  petstore    generate petstore project"
}

jars(){
	mvn -P cli-web dependency:copy-dependencies -DincludeScope=provided -DoutputDirectory=target
}

petstore(){
	java -jar target/codegen-cli-1.6.47.jar -f openapi -c https://gitee.com/lightgrp/light-service/raw/master/model-config/petstore/config.json -m https://gitee.com/lightgrp/light-service/raw/master/model-config/petstore/openapi.yaml -o target/petstore
}

if [ $# -eq 0 ]; then 
    usage
else
	case $1 in
	jars) jars ;;
	petstore) petstore ;;
	*) usage ;;
	esac
fi
