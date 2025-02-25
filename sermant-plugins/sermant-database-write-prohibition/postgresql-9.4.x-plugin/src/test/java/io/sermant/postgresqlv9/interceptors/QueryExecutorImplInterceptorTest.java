/*
 *  Copyright (C) 2024-2024 Huawei Technologies Co., Ltd. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.postgresqlv9.interceptors;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.database.config.DatabaseWriteProhibitionConfig;
import io.sermant.database.config.DatabaseWriteProhibitionManager;
import io.sermant.database.utils.ThreadDatabaseUrlUtil;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.postgresql.core.Query;
import org.postgresql.core.v3.QueryExecutorImpl;
import org.postgresql.jdbc4.Jdbc4DatabaseMetaData;

import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Test class for interceptors executing SQL operations
 *
 * @author zhp
 * @since 2024-02-04
 **/
public class QueryExecutorImplInterceptorTest {
    private static final DatabaseWriteProhibitionConfig GLOBAL_CONFIG = new DatabaseWriteProhibitionConfig();

    private static Method methodMock;

    private static Object[] argument;

    private static QueryExecutorImpl queryExecutor;

    private static final String URL = "jdbc:postgresql://localhost:5432/database-test";

    private static final String WRITE_SQL =
            "INSERT INTO `test`.`students` (`id`, `name`, `age`) VALUES (19, 'lilei', 111)";

    private static final String READ_SQL = "SELECT * FROM students WHERE id = 19";

    private final QueryExecutorImplInterceptor queryExecutorImplInterceptor = new QueryExecutorImplInterceptor();

    @BeforeClass
    public static void setUp() throws SQLException {
        DatabaseWriteProhibitionManager.updateGlobalConfig(GLOBAL_CONFIG);
        methodMock = Mockito.mock(Method.class);
        queryExecutor = Mockito.mock(QueryExecutorImpl.class);
        DatabaseMetaData metaData = Mockito.mock(Jdbc4DatabaseMetaData.class);
        ThreadDatabaseUrlUtil.setDatabaseUrl(URL);
        Mockito.when(metaData.getURL()).thenReturn(URL);
        Query query = new PostSqlQuery(WRITE_SQL);
        argument = new Object[]{query, null, null, null, null, null, null};
    }

    @Test
    public void testDoBefore() throws Exception {
        // Database write prohibition switch turned off
        GLOBAL_CONFIG.setEnablePostgreSqlWriteProhibition(false);
        ExecuteContext context = ExecuteContext.forMemberMethod(queryExecutor, methodMock, argument, null, null);
        queryExecutorImplInterceptor.before(context);
        Assert.assertNull(context.getThrowableOut());

        // The database write prohibition switch is turned off, and the write prohibition database set contains the
        // intercepted database
        Set<String> databases = new HashSet<>();
        databases.add("database-test");
        GLOBAL_CONFIG.setPostgreSqlDatabases(databases);
        queryExecutorImplInterceptor.before(context);
        Assert.assertNull(context.getThrowableOut());

        // The database write prohibition switch is turned on, and the write prohibition database collection contains
        // the intercepted databases
        GLOBAL_CONFIG.setEnablePostgreSqlWriteProhibition(true);
        context = ExecuteContext.forMemberMethod(queryExecutor, methodMock, argument, null, null);
        queryExecutorImplInterceptor.before(context);
        Assert.assertEquals("Database prohibit to write, database: database-test",
                context.getThrowableOut().getMessage());

        // The database write prohibition switch is turned on, and the write prohibition database collection contains
        // the intercepted database. SQL does not perform write operations
        Query readQuery = new PostSqlQuery(READ_SQL);
        context = ExecuteContext.forMemberMethod(queryExecutor, methodMock,
                new Object[]{readQuery, null, null, null, null, null, null}, null, null);
        queryExecutorImplInterceptor.before(context);
        Assert.assertNull(context.getThrowableOut());

        // The database write prohibition switch is turned on, and the write prohibition database collection does not
        // contain the intercepted database
        GLOBAL_CONFIG.setPostgreSqlDatabases(new HashSet<>());
        context = ExecuteContext.forMemberMethod(queryExecutor, methodMock, argument, null, null);
        queryExecutorImplInterceptor.before(context);
        Assert.assertNull(context.getThrowableOut());
    }

    @AfterClass
    public static void tearDown() throws SQLException {
        Mockito.clearAllCaches();
        ThreadDatabaseUrlUtil.removeDatabaseUrl();
        DatabaseWriteProhibitionManager.updateGlobalConfig(null);
    }
}
