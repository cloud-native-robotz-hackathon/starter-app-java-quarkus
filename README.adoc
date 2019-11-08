= Robot Controller Application Template - Quarkus  Java

IMPORTANT: This application requires Java 8 JDK or greater and Maven 3.5.3 or greater.



== Running the App Locally

To run this application on your local host:

[source,bash,options="nowrap",subs="attributes+"]
----
$ cd starter-app-java-quarkus

$ mvn compile quarkus:dev
----

== Interacting with the Application Locally

To interact with your booster while it's running locally, use the form at `http://localhost:8080` or the `curl` command:

[source,bash,options="nowrap",subs="attributes+"]
----
$ curl http://localhost:8080/api/my_robot/ping


$ curl -X POST http://localhost:8080/api/my_robot/run

----


== Updating the Application
To update your application:

. Stop your application.
+
NOTE: To stop your running application in a Linux or macOS terminal, use `CTRL+C`. In a Windows command prompt, you can use `CTRL + Break(pause)`.

. Make your change .
. Restart your application.
. Confirm your change appears.

HINT: Quarkus is able to reload code changes without having to stop it when running in dev mode. If you need to add any dipendency, use ./mvnw quarkus:add-extension -Dextensions=<extention>. From inside a Quarkus project, you can obtain a list of the available extensions with: ./mvnw quarkus:list-extensions


== Running the Application on the OpenShift Cluster

To deploy your booster to a running OpenShift cluster:
[source,bash,options="nowrap",subs="attributes+"]
----
Build the ube jar 
$ mvn clean package -DuberJar

It produces an executable jar file in the target/ director, starter-app-java-quarkus-1.0-SNAPSHOT-runner.jar, an executable jar that can be run with java -jar

$ oc login -u <username> -p <password>

$ oc project <teamname>-robot-app

$ oc new-build registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift:1.5 --binary --name=<teamname>-robot-app -l app=starter-app-java-quarkus
$ oc start-build <teamname>-robot-app --from-file target/*-runner.jar --follow
$ oc expose service <teamname>-robot-app
Finally, make sure it's actually done rolling out:
$ oc rollout status -w dc/<teamname>-robot-app  
----

== More Information
You can learn more about this booster and rest of the Quarkus  runtime in the https://quarkus.io/guides/].
