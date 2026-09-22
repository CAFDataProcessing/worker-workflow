/*
 * Copyright 2017-2026 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.workers.workflow.otel;

import io.opentelemetry.api.trace.Span;
import java.nio.charset.StandardCharsets;
import net.bytebuddy.asm.Advice;

/**
 * ByteBuddy advice for {@code DefaultRabbitConsumer.getDeliverEvent(Envelope, byte[], Map)}.
 * <p>
 * This advice is inlined at instrumentation time — the advice class is not referenced at runtime.
 * The inlined code runs at method-enter and uses {@code Span.current()}, which resolves to the
 * active OTel consumer span because the method is called from within
 * {@code TracedDelegatingConsumer.handleDelivery()} while the span scope is open.
 * <p>
 * The body is truncated to 4096 bytes before conversion to a UTF-8 string to keep attribute
 * size reasonable for a POC. No masking or redaction is applied.
 */
public final class RabbitBodyCaptureAdvice
{
    private RabbitBodyCaptureAdvice()
    {
    }

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(1) final byte[] body)
    {
        if (body == null || body.length == 0) {
            return;
        }
        final int len = Math.min(body.length, 4096);
        final String payload = new String(body, 0, len, StandardCharsets.UTF_8);
        Span.current().setAttribute("messaging.rabbitmq.message.body", payload);
    }
}
