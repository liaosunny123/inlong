package org.apache.inlong.sort.tests.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

public class StarRocksManager {

    // ----------------------------------------------------------------------------------------
    // StarRocks Variables
    // ----------------------------------------------------------------------------------------
    public static final String INTER_CONTAINER_STAR_ROCKS_ALIAS = "starrocks";
    private static final String NEW_STARROCKS_REPOSITORY = "inlong-starrocks";
    private static final String NEW_STARROCKS_TAG = "latest";
    private static final String STAR_ROCKS_IMAGE_NAME = "starrocks/allin1-ubi:3.0.4";
    public static final Logger STAR_ROCKS_LOG = LoggerFactory.getLogger(StarRocksContainer.class);
    public static void buildStarRocksImage() {
        GenericContainer oldStarRocks = new GenericContainer(STAR_ROCKS_IMAGE_NAME);
        Startables.deepStart(Stream.of(oldStarRocks)).join();
        oldStarRocks.copyFileToContainer(MountableFile.forClasspathResource("/docker/starrocks/start_fe_be.sh"),
                "/data/deploy/");
        try {
            oldStarRocks.execInContainer("chmod", "+x", "/data/deploy/start_fe_be.sh");
        } catch (Exception e) {
            e.printStackTrace();
        }
        oldStarRocks.getDockerClient()
                .commitCmd(oldStarRocks.getContainerId())
                .withRepository(NEW_STARROCKS_REPOSITORY)
                .withTag(NEW_STARROCKS_TAG).exec();
        oldStarRocks.stop();
    }

    public static String getNewStarRocksImageName() {
        return NEW_STARROCKS_REPOSITORY + ":" + NEW_STARROCKS_TAG;
    }

    public static void initializeStarRocksTable(StarRocksContainer STAR_ROCKS) {
        try (Connection conn =
                     DriverManager.getConnection(STAR_ROCKS.getJdbcUrl(), STAR_ROCKS.getUsername(),
                             STAR_ROCKS.getPassword());
             Statement stat = conn.createStatement()) {
            stat.execute("CREATE TABLE IF NOT EXISTS test_output1 (\n"
                    + "       id INT NOT NULL,\n"
                    + "       name VARCHAR(255) NOT NULL DEFAULT 'flink',\n"
                    + "       description VARCHAR(512)\n"
                    + ")\n"
                    + "PRIMARY KEY(id)\n"
                    + "DISTRIBUTED by HASH(id) PROPERTIES (\"replication_num\" = \"1\");");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
