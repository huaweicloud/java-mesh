/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.router.dubbo.strategy.rule;

import io.sermant.router.common.mapper.AbstractMetadataMapper;
import io.sermant.router.config.strategy.AbstractRuleStrategy;
import io.sermant.router.dubbo.strategy.instance.MatchInstanceStrategy;
import io.sermant.router.dubbo.strategy.instance.MismatchInstanceStrategy;

/**
 * Routing rule matching strategy
 *
 * @author provenceee
 * @since 2021-10-14
 */
public class InvokerRuleStrategy extends AbstractRuleStrategy<Object> {
    /**
     * Constructor
     *
     * @param mapper metadata processing
     */
    public InvokerRuleStrategy(AbstractMetadataMapper<Object> mapper) {
        super("dubbo", new MatchInstanceStrategy(), new MismatchInstanceStrategy(), mapper);
    }
}
