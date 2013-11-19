/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved. NTS PROPRIETARY/CONFIDENTIAL. Use is
 * subject to NTS License Agreement. Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria Homepage:
 * www.ntswincash.com
 */

package com.loadbalancing.loadbalancer;

import java.util.Scanner;

import com.loadbalancing.model.WeightedServiceRegistration;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.ITypedMessageReceiver;
import eneter.messaging.endpoints.typedmessages.ITypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.TypedMessageReceivedEventArgs;
import eneter.messaging.endpoints.typedmessages.TypedMessagesFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

/**
 * @author mga
 * 
 */
public class WeightedLoadBalancer {

    public static void main(String[] args) throws Exception {
        // Create TCP messaging for the communication with the client
        // and with services performing requests.
        IMessagingSystemFactory messagingFactory = new TcpMessagingSystemFactory();

        // Create load balancer.
        // It receives requests from the client and forwards them to available services
        // to balance the workload.
        final WeightedTaskLoadBalancer loadBalancer = new WeightedTaskLoadBalancer(messagingFactory);

        // NOTE : There is no more need to register the services statically because now the
        // services can be registered dynamically.
        // String[] availableServers =
        // new String[] {"tcp://127.0.0.1:8061", "tcp://127.0.0.1:8062", "tcp://127.0.0.1:8063"};
        // loadBalancer.addDuplexOutputChannel("tcp://127.0.0.1:8061", 10);
        // loadBalancer.addDuplexOutputChannel("tcp://127.0.0.1:8062", 2);
        // loadBalancer.addDuplexOutputChannel("tcp://127.0.0.1:8063", 1);

        String serviceRegistrationLoadBalancerAddress = "tcp://127.0.0.1:8061";
        IInputChannel serviceRegistrationInputChannel =
                messagingFactory.createInputChannel(serviceRegistrationLoadBalancerAddress);

        // Create typed message receiver to receive requests.
        // It receives request messages of type WeightedServiceRegistration and uses their values (tcp duplex
        // output channel address and weight)
        // to register it to the load balancer
        ITypedMessagesFactory serviceRegistrationReceiverFactory = new TypedMessagesFactory();
        ITypedMessageReceiver<WeightedServiceRegistration> serviceRegistrationReceiver =
                serviceRegistrationReceiverFactory
                        .createTypedMessageReceiver(WeightedServiceRegistration.class);

        serviceRegistrationReceiver.messageReceived().subscribe(
                new EventHandler<TypedMessageReceivedEventArgs<WeightedServiceRegistration>>() {
                    private int registrationCount = 0;

                    @Override
                    public void onEvent(Object sender,
                            TypedMessageReceivedEventArgs<WeightedServiceRegistration> e) {
                        String channelId = e.getMessageData().getChannelId();
                        int weight = e.getMessageData().getWeight();
                        System.out.println("Service registration request " + (++registrationCount)
                                + " channel: " + channelId + " weight : " + weight);

                        try {
                            loadBalancer.addDuplexOutputChannel(channelId, weight);
                        } catch (Exception exception) {
                            EneterTrace.warning("Exception occured when adding service with channel "
                                    + channelId, exception);
                        }
                        System.out.println("Service registration for channel " + channelId + " completed");
                    }
                });

        serviceRegistrationReceiver.attachInputChannel(serviceRegistrationInputChannel);

        // Create input channel that will listen to requests from clients.
        String clientRequestsLoadBalancerAddress = "tcp://127.0.0.1:8060";
        IDuplexInputChannel clientRequestsInputChannel =
                messagingFactory.createDuplexInputChannel(clientRequestsLoadBalancerAddress);

        loadBalancer.attachDuplexInputChannel(clientRequestsInputChannel);

        System.out.println("Load balancer listening to " + clientRequestsLoadBalancerAddress
                + " is running.\r\nPress ENTER to stop.");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        // Detach the input channel and stop listening.
        loadBalancer.detachDuplexInputChannel();
    }
}
