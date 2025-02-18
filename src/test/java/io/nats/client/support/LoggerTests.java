// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerTests {

    @Test
    public void testLoggerFacade() {
        NatsLoggerFacade LOG = NatsLoggerFacade.getLogger(LoggerTests.class);
        //NoOP Test
        testLogger();
        System.out.println("-------------------------------");
        //STDOUT TEST default level
        LOG.setNatsLogger(new StdOutLogger());
        testLogger();
        System.out.println("-------------------------------");
        //STDOUT TEST default level
        LOG.setNatsLogger(new StdOutLogger(Level.ALL));
        testLogger();
    }

    @Test
    public void testLoggerFacadeWithStaticLogger() {
        TestLogger testLogger = new TestLogger();
        NatsLoggerFacade.setDefaultNatsLogger(testLogger);
        NatsLoggerFacade LOG = NatsLoggerFacade.getLogger(LoggerTests.class);
        LOG.trace("TRACE");
        LOG.severe("SEVERE");
        assertEquals(2, testLogger.getLogEvents().size());

        NatsLoggerFacade.setDefaultNatsLogger(null);
        LOG.severe("SEVERE");
        assertEquals(3, testLogger.getLogEvents().size());

        NatsLoggerFacade.setDefaultNatsLogger(new StdOutLogger());
        NatsLoggerFacade LOG2 = NatsLoggerFacade.getLogger(LoggerTests.class, testLogger);
        LOG.trace("TRACE");
        assertEquals(4, testLogger.getLogEvents().size());

    }

    private void testLogger() {
        NatsLoggerFacade LOGGER = NatsLoggerFacade.getLogger(LoggerTests.class);
        LOGGER.severe("SEVERE TEST MESSAGE");
        LOGGER.severe("SEVERE TEST MESSAGE", new RuntimeException("SEVERE Test Exception"));
        LOGGER.severe(() -> createTestMessage("SEVERE MSG"), new RuntimeException("SEVERE Test Exception"));
        LOGGER.severe(() -> createTestMessage("SEVERE MSG"));

        LOGGER.warning("WARN TEST MESSAGE");
        LOGGER.warning("WARN TEST MESSAGE", new RuntimeException("WARN Test Exception"));
        LOGGER.warning(() -> createTestMessage("WARN MSG"), new RuntimeException("WARN Test Exception"));
        LOGGER.warning(() -> createTestMessage("WARN MSG"));

        LOGGER.info("INFO TEST MESSAGE");
        LOGGER.info("INFO TEST MESSAGE", new RuntimeException("INFO Test Exception"));
        LOGGER.info(() -> createTestMessage("INFO MSG"), new RuntimeException("INFO Test Exception"));
        LOGGER.info(() -> createTestMessage("INFO MSG"));

        LOGGER.trace("TRACE TEST MESSAGE");
        LOGGER.trace("TRACE TEST MESSAGE", new RuntimeException("TRACE Test Exception"));
        LOGGER.trace(() -> createTestMessage("TRACE MSG"), new RuntimeException("TRACE Test Exception"));
        LOGGER.trace(() -> createTestMessage("TRACE MSG"));

        LOGGER.log(Level.FINER, "FINER TEST MESSAGE");
        LOGGER.log(Level.FINER, "FINER TEST MESSAGE", new RuntimeException("FINER Test Exception"));
        LOGGER.log(Level.FINER, () -> createTestMessage("FINER MSG"), new RuntimeException("FINER Test Exception"));
        LOGGER.log(Level.FINER, () -> createTestMessage("FINER MSG"));
    }

    @Test
    public void testLoggerFacadeWithCustomLogger() {
        NatsLoggerFacade LOGGER = NatsLoggerFacade.getLogger(LoggerTests.class);
        TestLogger testLogger = new TestLogger();
        LOGGER.setNatsLogger(testLogger);

        Instant before = Instant.now();
        LOGGER.severe("SEVERE TEST MESSAGE");
        LOGGER.info("INFO TEST MESSAGE");
        LOGGER.warning("SEVERE TEST MESSAGE");
        LOGGER.trace("TRACE TEST MESSAGE");
        LOGGER.log(Level.ALL,"ALL TEST MESSAGE");
        LOGGER.log(Level.OFF, createTestMessage("OFF MSG"));
        Instant after = Instant.now();

        assertions(testLogger, before, after);
        testLogger.clearLogs();

        before = Instant.now();
        LOGGER.severe("SEVERE TEST MESSAGE", null);
        LOGGER.info("INFO TEST MESSAGE", null);
        LOGGER.warning("SEVERE TEST MESSAGE", null);
        LOGGER.trace("TRACE TEST MESSAGE", null);
        LOGGER.log(Level.ALL,"ALL TEST MESSAGE", null);
        LOGGER.log(Level.OFF, createTestMessage("OFF MSG"), null);
        after = Instant.now();
        assertions(testLogger, before, after);
        testLogger.clearLogs();

        before = Instant.now();
        LOGGER.severe(() -> createTestMessage("SEVERE TEST MESSAGE"), null);
        LOGGER.info(() -> createTestMessage("SEVERE TEST MESSAGE"), null);
        LOGGER.warning(() -> createTestMessage("SEVERE TEST MESSAGE"), null);
        LOGGER.trace(() -> createTestMessage("SEVERE TEST MESSAGE"), null);
        LOGGER.log(Level.ALL, () -> createTestMessage("ALL TEST MESSAGE"), null);
        LOGGER.log(Level.OFF,() -> createTestMessage("OFF MSG"), null);
        after = Instant.now();
        assertions(testLogger, before, after);
        testLogger.clearLogs();

        before = Instant.now();
        LOGGER.severe(() -> createTestMessage("SEVERE TEST MESSAGE"));
        LOGGER.info(() -> createTestMessage("SEVERE TEST MESSAGE"));
        LOGGER.warning(() -> createTestMessage("SEVERE TEST MESSAGE"));
        LOGGER.trace(() -> createTestMessage("SEVERE TEST MESSAGE"));
        LOGGER.log(Level.ALL, () -> createTestMessage("ALL TEST MESSAGE"));
        LOGGER.log(Level.OFF,() -> createTestMessage("OFF MSG"));
        after = Instant.now();
        assertions(testLogger, before, after);
        testLogger.clearLogs();
    }

    @Test
    public void testStdOutLogger() {
        //Assert no exception when providing nulls...
        StdOutLogger stdOutLogger = new StdOutLogger();
        stdOutLogger.log(new NatsLogEvent(Level.INFO, LoggerTests.class.getName(), "Test"));
        stdOutLogger.log(new NatsLogEvent(Level.INFO, LoggerTests.class.getName(), "Test", null));
        stdOutLogger.log(new NatsLogEvent(Level.INFO, LoggerTests.class.getName(), "Test", new RuntimeException("Exception")));

        assertEquals(Level.INFO, stdOutLogger.getMinLevel());
        stdOutLogger.setMinLevel(Level.OFF);
        assertEquals(Level.OFF, stdOutLogger.getMinLevel());
        stdOutLogger.setMinLevel(null);
        assertEquals(Level.OFF, stdOutLogger.getMinLevel());
    }

    @Test
    public void testLogEvent() {
        //Assert no exception when providing nulls...
        NatsLogEvent event = new NatsLogEvent(Level.FINE, LoggerTests.class.getName(), "Test");
        assertNotNull(event.toString());
        assertTrue(event.toString().startsWith("NatsLogEvent"));

        event = new NatsLogEvent(null, null, (String) null);
        assertNotNull(event.toString());
        assertTrue(event.toString().startsWith("NatsLogEvent"));
        assertNotNull(event.getFormattedEventTime());

        event = new NatsLogEvent(null, null, (Supplier<String>) null);
        assertNotNull(event.toString());
        assertTrue(event.toString().startsWith("NatsLogEvent"));
        assertNotNull(event.getFormattedEventTime());
    }

    private void assertions(final TestLogger testLogger, final Instant before, final Instant after) {
        assertEquals(6, testLogger.getLogEvents().size());
        assertEquals(Level.SEVERE, testLogger.getLogEvents().get(0).getLogLevel());
        assertEquals(Level.INFO, testLogger.getLogEvents().get(1).getLogLevel());
        assertEquals(Level.WARNING, testLogger.getLogEvents().get(2).getLogLevel());
        assertEquals(Level.FINEST, testLogger.getLogEvents().get(3).getLogLevel());
        assertEquals(Level.ALL, testLogger.getLogEvents().get(4).getLogLevel());
        assertEquals(Level.OFF, testLogger.getLogEvents().get(5).getLogLevel());
        assertEquals(LoggerTests.class.getName(), testLogger.getLogEvents().get(5).getClassName());
        assertEquals(createTestMessage("OFF MSG"), testLogger.getLogEvents().get(5).getMessage());
        assertNull(testLogger.getLogEvents().get(5).getThrowable());
        assertTrue(testLogger.getLogEvents().get(5).getEventTime().toInstant().toEpochMilli() >= before.toEpochMilli());
        assertTrue(testLogger.getLogEvents().get(5).getEventTime().toInstant().toEpochMilli() <= after.toEpochMilli());
    }

    private String createTestMessage(final String content) {
        return "message " + content;
    }

    public static class TestLogger extends NatsLogger {
        private final List<NatsLogEvent> logEvents = new ArrayList<>();

        public TestLogger() {
            super();
            minLevel = Level.ALL;
        }

        @Override
        public void log(final NatsLogEvent natsLogEvent) {
            logEvents.add(natsLogEvent);
        }

        public List<NatsLogEvent> getLogEvents() {
            return logEvents;
        }

        public void clearLogs() {
            logEvents.clear();
        }
    }
}
