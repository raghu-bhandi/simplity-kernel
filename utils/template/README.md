# OVERVIEW
- To generate the records for the tables in the configured database.
	
# USAGE
- Use the command "mvn install" at template folder to install template into local repository.

- Configure the DB and template details in pom.xml of your project as follows

		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<version>1.3.171</version>
		</dependency>
		<dependency>
			<groupId>org.simplity.utils</groupId>
			<artifactId>template</artifactId>
			<version>1.2-SNAPSHOT</version>
		</dependency>
		<build>
			<plugins>
				<plugin>
					<groupId>org.apache.torque</groupId>
					<artifactId>torque-maven-plugin	</artifactId>
					<version>4.0</version>
					<executions>
						<execution>
							<id>generate-schema-from-jdbc</id>
							<phase>generate-test-sources</phase>
							<goals>
								<goal>generate</goal>
							</goals>
							<configuration>
								<packaging>classpath</packaging>
								<configPackage>com.infosys.qreuse.torque.simplity</configPackage>
								<newFileTargetDir>target/generated-schema</newFileTargetDir>
								<newFileTargetDirUsage>none</newFileTargetDirUsage>
								<options>
									<torque.jdbc2schema.driver>org.h2.Driver</torque.jdbc2schema.driver>
									<torque.jdbc2schema.url>jdbc:h2://<host>:<port>/<database name></torque.jdbc2schema.url>
									<torque.jdbc2schema.user>username</torque.jdbc2schema.user>
									<torque.jdbc2schema.password>password</torque.jdbc2schema.password>
									<torque.jdbc2schema.schema>schema name</torque.jdbc2schema.schema>
								</options>
							</configuration>
						</execution>
					</executions>
					<dependencies>
						<dependency>
							<groupId>com.h2database</groupId>
							<artifactId>h2</artifactId>
							<version>1.3.171</version>
						</dependency>
						<dependency>
							<groupId>com.infosys.qreuse.torque.simplity</groupId>
							<artifactId>template</artifactId>
							<version>0.0.1</version>
						</dependency>
					</dependencies>
				</plugin>
			</plugins>
		</build>
		
- To generate the records use the command "mvn test"

- Generated records are present under "target/generated-sources" folder.
