$CURRENT_DATE = GET-DATE -Format "yyyy-dd-MM HH:mm:ss"
#$CURRENT_DATE = GET-DATE -Format "yyyy-dd-MM"
mvn clean package "-Dmaven.test.skip=true";

$JAR_PATH = Resolve-Path ./target/demo-chunk-0.0.1-SNAPSHOT.jar
java -jar "-Dspring.profiles.active=QA" $JAR_PATH "run.date=$CURRENT_DATE";
pause;