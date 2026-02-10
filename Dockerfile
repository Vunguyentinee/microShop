FROM tomcat:11.0-jdk21
RUN rm -rf /usr/local/tomcat/webapps/*
RUN mkdir -p /tmp/microshop_uploads && chmod 777 /tmp/microshop_uploads
COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
