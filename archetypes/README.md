### 使用light-example-4j项目创建archetype

git clone https://github.com/networknt/light-example-4j

##### 使用1.6.x分支（JAVA8），或者release分支（JAVA11），使用最新稳定版，跳过SNAPSHOT版本

git checkout origin/1.6.x

git log

git checkout a954c86275a18a58498ad36d05f86c4e053fc58c

cd light-example-4j/webserver/api-simple-web

mvn archetype:create-from-project

mvn install -f target/generated-sources/archetype/pom.xml

mvn archetype:generate -DarchetypeCatalog=local

##### 项目说明

 - api-simple-web：简单web站点项目，支持/public静态资源和/api/json、/api/text两个接口
