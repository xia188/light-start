#!/bin/bash

usage(){
    echo "Usage: start.sh ( commands ... )"
    echo "commands: "
    echo "  jars        copy dependencies to target"
    echo "  cli         generate petstore using codegen_cli"
    echo "  web         generate petstore using codegen_web"
    echo "  start       generate petstore using light_start"
}

jars(){
	mvn -P cli-web dependency:copy-dependencies -DincludeScope=provided -DoutputDirectory=target
}

cli(){
	java -jar target/codegen-cli-1.6.47.jar -f openapi -c https://gitee.com/lightgrp/light-service/raw/master/model-config/petstore/config.json -m https://gitee.com/lightgrp/light-service/raw/master/model-config/petstore/openapi.yaml -o target/petstore
}

# java -jar target/codegen-web-1.6.47.jar
web(){
	curl http://localhost:8080/codegen -H "Content-Type:application/json" -d "@cli-web.json" -o web.zip
}

start(){
	curl http://localhost:8080/api/json -d "framework=openapi&config=https://gitee.com/lightgrp/light-service/raw/master/model-config/petstore/config.json&model=https://gitee.com/lightgrp/light-service/raw/master/model-config/petstore/openapi.yaml"
}

if [ $# -eq 0 ]; then 
    usage
else
	case $1 in
	jars) jars ;;
	cli) cli ;;
	web) web ;;
	start) start ;;
	*) usage ;;
	esac
fi
