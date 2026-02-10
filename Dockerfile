FROM tomcat:11.0-jdk21
RUN rm -rf /usr/local/tomcat/webapps/*
RUN mkdir -p /uploads && chmod 777 /uploads
COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
