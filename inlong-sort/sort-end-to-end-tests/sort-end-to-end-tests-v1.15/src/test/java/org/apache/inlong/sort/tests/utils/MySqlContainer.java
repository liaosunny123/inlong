/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.tests.utils;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Set;

/**
 * Docker container for MySQL. The difference between this class and {@link
 * org.testcontainers.containers.MySQLContainer} is that TC MySQLContainer has problems when
 * overriding mysql conf file, i.e. my.cnf.
 */
@SuppressWarnings("rawtypes")
public class MySqlContainer extends JdbcDatabaseContainer {

    public static final String IMAGE = "mysql";
    public static final Integer MYSQL_PORT = 3306;

    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "MY_CNF";
    private static final String SETUP_SQL_PARAM_NAME = "SETUP_SQL";
    private static final String MYSQL_ROOT_USER = "root";

    private String databaseName = "test";
    private String username = "inlong";
    private String password = "inlong";

    public MySqlContainer() {
        this(MySqlVersion.V5_7);
    }

    public MySqlContainer(MySqlVersion version) {
        super(DockerImageName.parse(IMAGE + ":" + version.getVersion()));
        addFixedExposedPort(33306, 3306);
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(MYSQL_PORT));
    }

    @Override
    protected void configure() {
        // HERE is the difference, copy to /etc/mysql/, if copy to /etc/mysql/conf.d will be wrong

        if (parameters.containsKey(SETUP_SQL_PARAM_NAME)) {
            optionallyMapResourceParameterAsVolume(
                    SETUP_SQL_PARAM_NAME, "/docker-entrypoint-initdb.d/", "N/A");
        }

        addEnv("MYSQL_DATABASE", databaseName);
        addEnv("MYSQL_USER", username);
        if (password != null && !password.isEmpty()) {
            addEnv("MYSQL_PASSWORD", password);
            addEnv("MYSQL_ROOT_PASSWORD", password);
        } else if (MYSQL_ROOT_USER.equalsIgnoreCase(username)) {
            addEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes");
        } else {
            throw new ContainerLaunchException(
                    "Empty password can be used only with the root user");
        }
        withCommand("--default-authentication-plugin=mysql_native_password");
        setStartupAttempts(3);
    }

    @Override
    public String getDriverClassName() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return "com.mysql.cj.jdbc.Driver";
        } catch (ClassNotFoundException e) {
            return "com.mysql.jdbc.Driver";
        }
    }

    public String getJdbcUrl(String databaseName) {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:mysql://"
                + getHost()
                + ":"
                + getDatabasePort()
                + "/"
                + databaseName
                + additionalUrlParams
                + "?useSSL=false&allowPublicKeyRetrieval=true";
    }

    @Override
    public String getJdbcUrl() {
        return getJdbcUrl(databaseName);
    }

    public int getDatabasePort() {
        return getMappedPort(MYSQL_PORT);
    }

    @Override
    protected String constructUrlForConnection(String queryString) {
        String url = super.constructUrlForConnection(queryString);

        if (!url.contains("useSSL=")) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "useSSL=false";
        }

        if (!url.contains("allowPublicKeyRetrieval=")) {
            url = url + "&allowPublicKeyRetrieval=true";
        }

        return url;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    @SuppressWarnings("unchecked")
    public MySqlContainer withConfigurationOverride(String s) {
        parameters.put(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, s);
        return this;
    }

    @SuppressWarnings("unchecked")
    public MySqlContainer withSetupSQL(String sqlPath) {
        parameters.put(SETUP_SQL_PARAM_NAME, sqlPath);
        return this;
    }

    @Override
    public MySqlContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    @Override
    public MySqlContainer withUsername(final String username) {
        this.username = username;
        return this;
    }

    @Override
    public MySqlContainer withPassword(final String password) {
        this.password = password;
        return this;
    }

    /** MySql version enum. */
    public enum MySqlVersion {

        V5_5("5.5"),
        V5_6("5.6"),
        V5_7("5.7"),
        V8_0("8.0");

        private String version;

        MySqlVersion(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "MySqlVersion{" + "version='" + version + '\'' + '}';
        }
    }
}
