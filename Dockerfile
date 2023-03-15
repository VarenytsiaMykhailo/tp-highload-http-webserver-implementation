FROM openjdk:17-oracle
WORKDIR /MyWebServer
COPY . .
ENV OPTS="-Duser.timezone=GMT -Dfile.encoding=UTF-8 -Denvironment.type=production"

RUN javac -sourcepath src -d bin -classpath bin/MyWebServer.jar src/Main.java
CMD exec java $OPTS -classpath bin:bin/MyWebServer.jar Main
EXPOSE 80