// Copyright 2023 The NATS Authors
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

package io.nats.examples.chaosTestApp;

import io.nats.client.Dispatcher;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.PushSubscribeOptions;
import io.nats.examples.chaosTestApp.support.CommandLine;
import io.nats.examples.chaosTestApp.support.ConsumerKind;

import java.io.IOException;

public class PushConsumer extends ConnectableConsumer {
    final Dispatcher d;
    final JetStreamSubscription sub;

    public PushConsumer(CommandLine cmd, ConsumerKind consumerKind) throws IOException, InterruptedException, JetStreamApiException {
        super(cmd, "pu", consumerKind);

        d = nc.createDispatcher();

        PushSubscribeOptions pso = PushSubscribeOptions.builder()
            .stream(cmd.stream)
            .configuration(newCreateConsumer()
                .idleHeartbeat(1000)
                .build())
            .ordered(consumerKind == ConsumerKind.Ordered)
            .build();

        sub = js.subscribe(cmd.subject, d, handler, false, pso);
        Output.controlMessage(label, sub.getConsumerName());
    }

    @Override
    public void refreshInfo() {
        updateNameAndLabel(sub.getConsumerName());
    }
}
