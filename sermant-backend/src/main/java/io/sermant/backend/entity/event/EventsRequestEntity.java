/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.backend.entity.event;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Event query request entity
 *
 * @author xuezechao
 * @since 2023-03-02
 */
@Getter
@Setter
public class EventsRequestEntity {
    /**
     * service name
     */
    private List<String> service = new ArrayList<>();

    /**
     * event name
     */
    private List<String> name = new ArrayList<>();

    /**
     * address
     */
    private List<String> ip = new ArrayList<>();

    /**
     * scope
     */
    private List<String> scope = new ArrayList<>();

    /**
     * type
     */
    private List<String> type = new ArrayList<>();

    /**
     * level
     */
    private List<String> level = new ArrayList<>();

    /**
     * start time
     */
    private long startTime;

    /**
     * end time
     */
    private long endTime;

    /**
     * session id
     */
    private String sessionId;

    /**
     * instance id
     */
    private List<String> instanceIds = new ArrayList<>();
}
