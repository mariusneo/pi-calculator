/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved. NTS PROPRIETARY/CONFIDENTIAL. Use is
 * subject to NTS License Agreement. Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria Homepage:
 * www.ntswincash.com
 */

package com.loadbalancing.loadbalancer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.infrastructure.attachable.internal.AttachableDuplexInputChannelBase;
import eneter.messaging.messagingsystems.messagingsystembase.DuplexChannelMessageEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.messagingsystembase.ResponseReceiverEventArgs;
import eneter.messaging.nodes.loadbalancer.ILoadBalancer;
import eneter.net.system.Event;
import eneter.net.system.EventHandler;
import eneter.net.system.EventImpl;
import eneter.net.system.IFunction1;
import eneter.net.system.linq.internal.EnumerableExt;

/**
 * @author mga
 * 
 */
public class WeightedTaskLoadBalancer extends AttachableDuplexInputChannelBase implements ILoadBalancer {

    private static class TReceiver {
        private String channelId;
        private int weight;
        private int freeQuota;

        private HashSet<TConnection> openConnections = new HashSet<TConnection>();

        public static class TConnection {
            private String responseReceiverId;
            private IDuplexOutputChannel duplexOutputChannel;

            public TConnection(String responseReceiverId, IDuplexOutputChannel duplexOutputChannel) {
                this.responseReceiverId = responseReceiverId;
                this.duplexOutputChannel = duplexOutputChannel;
            }

            public String getResponseReceiverId() {
                return responseReceiverId;
            }

            public IDuplexOutputChannel getDuplexOutputChannel() {
                return this.duplexOutputChannel;
            }
        }

        public TReceiver(String duplexOutputChannelId, int weight) {
            if (weight <= 0) {
                throw new IllegalArgumentException("The receiver's weight must be positive");
            }
            this.channelId = duplexOutputChannelId;
            this.weight = weight;
            this.freeQuota = weight;
        }

        public void decreaseFreeQuota() {
            if (freeQuota > 0) {
                this.freeQuota--;
            }
        }

        public void resetFreeQuota() {
            freeQuota = weight;
        }

        public int getWeight() {
            return weight;
        }

        public int getFreeQuota() {
            return freeQuota;
        }

        public String getChannelId() {
            return this.channelId;
        }

        public HashSet<TConnection> getOpenConnections() {
            return this.openConnections;
        }

    }

    private static final int DEFAULT_RECEIVER_WEIGHT = 1;

    private IMessagingSystemFactory messagingSystemFactory;
    private List<TReceiver> availableReceivers = new ArrayList<TReceiver>();
    private List<TReceiver> receivers = new ArrayList<TReceiver>();
    private EventImpl<ResponseReceiverEventArgs> responseReceiverConnectedEventImpl =
            new EventImpl<ResponseReceiverEventArgs>();
    private EventImpl<ResponseReceiverEventArgs> responseReceiverDisconnectedEventImpl =
            new EventImpl<ResponseReceiverEventArgs>();
    private EventHandler<DuplexChannelMessageEventArgs> onResponseMessageReceivedHandler =
            new EventHandler<DuplexChannelMessageEventArgs>() {

                @Override
                public void onEvent(Object sender, DuplexChannelMessageEventArgs e) {
                    WeightedTaskLoadBalancer.this.onResponseMessageReceived(sender, e);
                }

            };

    public WeightedTaskLoadBalancer(IMessagingSystemFactory outputMessagingFactory) {
        EneterTrace trace = EneterTrace.entering();

        try {
            this.messagingSystemFactory = outputMessagingFactory;
        } finally {
            EneterTrace.leaving(trace);
        }
    }

    @Override
    public void addDuplexOutputChannel(String channelId) throws Exception {
        addDuplexOutputChannel(channelId, DEFAULT_RECEIVER_WEIGHT);
    }

    public void addDuplexOutputChannel(String channelId, int weight) throws Exception {
        EneterTrace trace = EneterTrace.entering();
        try {
            synchronized (receivers) {
                TReceiver receiver = new TReceiver(channelId, weight);
                this.receivers.add(receiver);

                synchronized (availableReceivers) {
                    this.availableReceivers.add(receiver);
                }
            }
        } finally {
            EneterTrace.leaving(trace);
        }

    }

    @Override
    public void removeAllDuplexOutputChannels() {
        EneterTrace trace = EneterTrace.entering();
        try {
            synchronized (receivers) {
                synchronized (availableReceivers) {
                    for (TReceiver receiver : receivers) {
                        for (WeightedTaskLoadBalancer.TReceiver.TConnection connection : receiver
                                .getOpenConnections()) {
                            try {
                                connection.getDuplexOutputChannel().closeConnection();
                            } catch (Exception exception) {
                                EneterTrace.error(TracedObject() + " failed to close connection to "
                                        + receiver.getChannelId(), exception);
                            }
                            connection.getDuplexOutputChannel().responseMessageReceived()
                                    .unsubscribe(this.onResponseMessageReceivedHandler);
                        }
                    }
                    receivers.clear();
                    availableReceivers.clear();
                }
            }
        } finally {
            EneterTrace.leaving(trace);
        }
    }

    @Override
    public void removeDuplexOutputChannel(final String channelId) throws Exception {
        EneterTrace trace = EneterTrace.entering();
        try {
            synchronized (receivers) {
                synchronized (availableReceivers) {

                    TReceiver receiver =
                            EnumerableExt.firstOrDefault(receivers, new IFunction1<Boolean, TReceiver>() {

                                @Override
                                public Boolean invoke(TReceiver aReceiver) throws Exception {
                                    return Boolean.valueOf(aReceiver.getChannelId().equals(channelId));
                                }

                            });
                    if (receiver != null) {
                        for (WeightedTaskLoadBalancer.TReceiver.TConnection connection : receiver
                                .getOpenConnections()) {
                            try {
                                connection.getDuplexOutputChannel().closeConnection();
                            } catch (Exception exception) {
                                EneterTrace.error(TracedObject() + " failed to close connection to "
                                        + channelId, exception);
                            }
                            connection.getDuplexOutputChannel().responseMessageReceived()
                                    .unsubscribe(this.onResponseMessageReceivedHandler);
                        }

                        receivers.remove(receiver);
                        availableReceivers.remove(receiver);
                    }
                }
            }
        } finally {
            EneterTrace.leaving(trace);
        }

    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverConnected() {
        return this.responseReceiverConnectedEventImpl.getApi();
    }

    @Override
    public Event<ResponseReceiverEventArgs> responseReceiverDisconnected() {
        return this.responseReceiverDisconnectedEventImpl.getApi();
    }

    @Override
    protected String TracedObject() {
        return getClass().getSimpleName() + " ";
    }

    @Override
    protected void onRequestMessageReceived(final Object sender, final DuplexChannelMessageEventArgs e) {
        EneterTrace trace = EneterTrace.entering();

        try {
            synchronized (receivers) {
                if (this.receivers.size() == 0) {
                    EneterTrace
                            .warning(TracedObject()
                                    + " could not forward the request because there are no attached duplex output channels.");
                } else {
                    synchronized (availableReceivers) {
                        for (int i = 0; i < this.availableReceivers.size(); i++) {
                            TReceiver receiver = this.availableReceivers.get(i);
                            WeightedTaskLoadBalancer.TReceiver.TConnection connection = null;
                            try {

                                connection =
                                        EnumerableExt
                                                .firstOrDefault(
                                                        receiver.getOpenConnections(),
                                                        new IFunction1<Boolean, WeightedTaskLoadBalancer.TReceiver.TConnection>() {
                                                            public
                                                                    Boolean
                                                                    invoke(WeightedTaskLoadBalancer.TReceiver.TConnection x) {
                                                                return Boolean.valueOf(x
                                                                        .getResponseReceiverId().equals(
                                                                                e.getResponseReceiverId()));
                                                            }
                                                        });
                            } catch (Exception exception) {
                                EneterTrace.error(TracedObject()
                                        + " failed during EnumberableExt.firstOrDefault()", exception);
                            }

                            if (connection == null) {
                                try {
                                    IDuplexOutputChannel outputChannel =
                                            this.messagingSystemFactory.createDuplexOutputChannel(receiver
                                                    .getChannelId());
                                    connection =
                                            new WeightedTaskLoadBalancer.TReceiver.TConnection(
                                                    e.getResponseReceiverId(), outputChannel);
                                    connection.getDuplexOutputChannel().responseMessageReceived()
                                            .subscribe(this.onResponseMessageReceivedHandler);
                                    connection.getDuplexOutputChannel().openConnection();

                                    receiver.getOpenConnections().add(connection);
                                } catch (Exception exception) {
                                    connection.getDuplexOutputChannel().responseMessageReceived()
                                            .unsubscribe(onResponseMessageReceivedHandler);
                                    EneterTrace.warning(
                                            TracedObject() + " failed to open connection to receiver "
                                                    + receiver.getChannelId(), exception);
                                }
                            }

                            try {
                                connection.getDuplexOutputChannel().sendMessage(e.getMessage());
                            } catch (Exception exception) {
                                EneterTrace
                                        .warning(TracedObject() + " failed to send the message", exception);

                                try {
                                    connection.getDuplexOutputChannel().closeConnection();
                                } catch (Exception exception2) {
                                    EneterTrace.warning(TracedObject()
                                            + " failed to send the message that the connection was closed."
                                            + exception2);
                                }

                                connection.getDuplexOutputChannel().responseMessageReceived()
                                        .unsubscribe(onResponseMessageReceivedHandler);
                                receiver.getOpenConnections().remove(connection);

                                continue;
                            }

                            availableReceivers.remove(i);

                            receiver.decreaseFreeQuota();
                            if (receiver.getFreeQuota() > 0) {
                                // round-robin technique : remove the current receiver
                                // from the list and add it to the end of the list.
                                availableReceivers.add(receiver);
                            }else if (availableReceivers.size() == 0){
                                // if all the receiver capacities have been completely exhausted
                                // then reset the list of available receivers.
                                for (TReceiver aReceiver : receivers){
                                    aReceiver.resetFreeQuota();
                                    availableReceivers.add(aReceiver);
                                }
                            }
                            
                            break;
                        }
                    }
                }

            }
        } finally {
            EneterTrace.leaving(trace);
        }
    }

    private void onResponseMessageReceived(final Object sender, final DuplexChannelMessageEventArgs e) {
        EneterTrace trace = EneterTrace.entering();

        try {
            String responseReceiverId = null;
            synchronized (receivers) {
                TReceiver receiver = null;
                try {
                    receiver =
                            EnumerableExt.firstOrDefault(receivers,
                                    new IFunction1<Boolean, WeightedTaskLoadBalancer.TReceiver>() {
                                        public Boolean invoke(WeightedTaskLoadBalancer.TReceiver x) {
                                            return Boolean.valueOf(x.getChannelId().equals(e.getChannelId()));
                                        }
                                    });

                } catch (Exception exception) {
                    EneterTrace.error(TracedObject() + " failed to retrieve the receiver for channel "
                            + e.getChannelId());
                }

                if (receiver != null) {
                    WeightedTaskLoadBalancer.TReceiver.TConnection connection = null;
                    try {

                        connection =
                                EnumerableExt
                                        .firstOrDefault(
                                                receiver.openConnections,
                                                new IFunction1<Boolean, WeightedTaskLoadBalancer.TReceiver.TConnection>() {
                                                    public Boolean invoke(
                                                            WeightedTaskLoadBalancer.TReceiver.TConnection x) {
                                                        return Boolean.valueOf(x.getDuplexOutputChannel()
                                                                .getResponseReceiverId()
                                                                .equals(e.getResponseReceiverId()));
                                                    }
                                                });
                    } catch (Exception exception) {
                        EneterTrace.error(TracedObject() + " failed during EnumberableExt.firstOrDefault()",
                                exception);
                    }

                    if (connection != null) {
                        responseReceiverId = connection.getResponseReceiverId();
                    }
                }
            }

            if (responseReceiverId == null) {
                EneterTrace.warning(TracedObject()
                        + " could not find the receiver for the incoming response message "
                        + e.getResponseReceiverId());
                return;
            }

            synchronized (this.myDuplexInputChannelManipulatorLock) {
                IDuplexInputChannel aDuplexInputChannel = getAttachedDuplexInputChannel();
                if (aDuplexInputChannel != null) {
                    try {
                        aDuplexInputChannel.sendResponseMessage(responseReceiverId, e.getMessage());
                    } catch (Exception err) {
                        EneterTrace.error(TracedObject()
                                + " failed to send the response message to receiver " + responseReceiverId,
                                err);
                    }
                } else {
                    EneterTrace
                            .error(TracedObject()
                                    + "cannot send the response message when the duplex input channel is not attached.");
                }
            }
        } finally {
            EneterTrace.leaving(trace);
        }
    }

    @Override
    protected void onResponseReceiverConnected(Object sender, ResponseReceiverEventArgs e) {
        EneterTrace trace = EneterTrace.entering();
        try {
            if (this.responseReceiverConnectedEventImpl.isSubscribed()) {
                try {
                    this.responseReceiverConnectedEventImpl.raise(this, e);
                } catch (Exception exception) {
                    EneterTrace.warning(TracedObject() + " detected an exception.", exception);
                }
            }
        } finally {
            EneterTrace.leaving(trace);
        }
    }

    @Override
    protected void onResponseReceiverDisconnected(Object sender, ResponseReceiverEventArgs e) {
        EneterTrace trace = EneterTrace.entering();
        try {
            if (this.responseReceiverDisconnectedEventImpl.isSubscribed()) {
                try {
                    this.responseReceiverDisconnectedEventImpl.raise(this, e);
                } catch (Exception exception) {
                    EneterTrace.warning(TracedObject() + " detected an exception.", exception);
                }
            }
        } finally {
            EneterTrace.leaving(trace);
        }
    }

}
