$CURRENT_DATE = GET-DATE -Format "yyyy/dd/MM"
mvn clean package "-Dmaven.test.skip=true";

$JAR_PATH = Resolve-Path ./target/demo-schedule-0.0.1-SNAPSHOT.jar
java -jar $JAR_PATH "run.date(date)=$CURRENT_DATE"
pause;