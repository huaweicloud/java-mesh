/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.dynamic.config.source;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * test disable configuration
 *
 * @author zhouss
 * @since 2022-09-05
 */
public class OriginConfigDisableSourceTest {
    private final OriginConfigDisableSource source = new OriginConfigDisableSource("test");

    @Test
    public void getConfigNames() {
        final Set<String> configNames = source.getConfigNames();
        Assert.assertFalse(configNames.isEmpty());
    }

    @Test
    public void getConfig() {
        final Object config = source.getConfig(OriginConfigDisableSource.ZK_CONFIG_CENTER_ENABLED);
        Assert.assertNotNull(config);
        Assert.assertTrue(config instanceof Boolean);
        Assert.assertEquals(config, false);
    }
}
