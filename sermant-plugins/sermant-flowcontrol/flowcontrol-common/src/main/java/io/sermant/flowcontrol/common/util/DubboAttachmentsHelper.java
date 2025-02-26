/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.flowcontrol.common.util;

import io.sermant.core.utils.ReflectUtils;

import org.apache.dubbo.common.Version;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Request body conversion tool class, mainly based on reflection
 *
 * @author zhouss
 * @since 2022-09-19
 */
public class DubboAttachmentsHelper {
    private static final String APACHE_RPC_CONTEXT = "org.apache.dubbo.rpc.RpcContext";

    private static final String ALIBABA_RPC_CONTEXT = "com.alibaba.dubbo.rpc.RpcContext";

    private static final String ATTACHMENTS_FIELD = "attachments";

    private static final String GET_CONTEXT_METHOD = "getContext";

    private static final String GET_SERVER_ATTACHMENT_METHOD = "getServerAttachment";

    private static final String DUBBO_3X_PREFIX = "3.";

    private DubboAttachmentsHelper() {
    }

    /**
     * get attachments
     *
     * @param invocation call information
     * @param isApache whether it is apache dubbo
     * @return Map
     */
    public static Map<String, String> resolveAttachments(Object invocation, boolean isApache) {
        if (invocation == null) {
            return Collections.emptyMap();
        }
        String dubboVersion = invocation.getClass().getPackage().getImplementationVersion();
        if (dubboVersion == null) {
            dubboVersion = Version.getVersion();
        }
        final Map<String, String> attachments = new HashMap<>();
        if (isApache) {
            if (dubboVersion.startsWith(DUBBO_3X_PREFIX)) {
                attachments.putAll(getAttachmentsFromContext(APACHE_RPC_CONTEXT, GET_SERVER_ATTACHMENT_METHOD));
            } else {
                attachments.putAll(getAttachmentsFromContext(APACHE_RPC_CONTEXT, GET_CONTEXT_METHOD));
            }
        } else {
            attachments.putAll(getAttachmentsFromContext(ALIBABA_RPC_CONTEXT, GET_CONTEXT_METHOD));
        }
        final Optional<Object> fieldValue = ReflectUtils.getFieldValue(invocation, ATTACHMENTS_FIELD);
        if (fieldValue.isPresent() && fieldValue.get() instanceof Map) {
            attachments.putAll((Map<String, String>) fieldValue.get());
        }
        return Collections.unmodifiableMap(attachments);
    }

    private static Map<String, String> getAttachmentsFromContext(String contextClazz, String methodName) {
        final Optional<Object> context = ReflectUtils.invokeMethod(contextClazz, methodName, null,
                null);
        if (!context.isPresent()) {
            return Collections.emptyMap();
        }
        final Optional<Object> attachments = ReflectUtils.getFieldValue(context.get(), ATTACHMENTS_FIELD);
        if (attachments.isPresent() && attachments.get() instanceof Map) {
            return (Map<String, String>) attachments.get();
        }
        return Collections.emptyMap();
    }
}
