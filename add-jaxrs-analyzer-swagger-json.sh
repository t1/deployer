cd target/
mkdir -p WEB-INF/classes/doc/
cp jaxrs-analyzer/swagger.json WEB-INF/classes/doc/
jar uf deployer.war WEB-INF/classes/doc/swagger.json
cd -

