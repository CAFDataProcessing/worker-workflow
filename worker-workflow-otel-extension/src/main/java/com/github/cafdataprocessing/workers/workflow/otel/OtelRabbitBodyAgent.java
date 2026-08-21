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

import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Java premain agent that captures the raw RabbitMQ message body onto the OTel consumer span.
 * <p>
 * The CAF worker framework uses raw {@code com.rabbitmq.client.Consumer} with an asynchronous
 * event-queue pattern. The OTel Java agent wraps the registered consumer in a
 * {@code TracedDelegatingConsumer}, which creates and ends the consumer span entirely within
 * {@code handleDelivery()}. The framework's {@code RabbitConsumer.handleDelivery()} is
 * {@code final} and just enqueues a {@code ConsumerDeliverEvent}; actual processing happens on
 * a separate thread long after the span has ended.
 * <p>
 * The only synchronous call-site inside the live consumer span is
 * {@code DefaultRabbitConsumer.getDeliverEvent(Envelope, byte[], Map)}, which receives the raw
 * body bytes and is called from within {@code RabbitConsumer.handleDelivery()}, which is itself
 * called from within {@code TracedDelegatingConsumer.handleDelivery()} while the span is active.
 * <p>
 * This agent instruments {@code getDeliverEvent} via ByteBuddy advice to read {@code body} and
 * set {@code messaging.rabbitmq.message.body} on {@code Span.current()} before the method body
 * executes (and before the span ends).
 */
public final class OtelRabbitBodyAgent
{
    private OtelRabbitBodyAgent()
    {
    }

    public static void premain(final String args, final Instrumentation inst)
    {
        new AgentBuilder.Default()
            .type(named("com.github.workerframework.util.rabbitmq.DefaultRabbitConsumer"))
            .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                // Guard: only instrument if the OTel API Span class is resolvable from the
                // target classloader. If the OTel Java agent is absent, Span is not on the
                // bootstrap classloader and the instrumented bytecode would fail class
                // verification with NoClassDefFoundError.
                final ClassLoader effectiveLoader = classLoader != null
                    ? classLoader
                    : ClassLoader.getSystemClassLoader();
                try {
                    Class.forName("io.opentelemetry.api.trace.Span", false, effectiveLoader);
                } catch (final ClassNotFoundException ignored) {
                    return builder; // OTel API not available — skip instrumentation
                }
                return builder.visit(
                    Advice.to(RabbitBodyCaptureAdvice.class)
                        .on(named("getDeliverEvent"))
                );
            })
            .installOn(inst);
    }
}
