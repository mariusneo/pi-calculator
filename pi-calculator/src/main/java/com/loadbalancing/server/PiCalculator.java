/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved. NTS PROPRIETARY/CONFIDENTIAL. Use is
 * subject to NTS License Agreement. Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria Homepage:
 * www.ntswincash.com
 */

package com.loadbalancing.server;

import java.util.Scanner;

import com.loadbalancing.model.Range;
import com.loadbalancing.model.WeightedServiceRegistration;

import eneter.messaging.endpoints.typedmessages.DuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessageReceiver;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.ITypedMessageSender;
import eneter.messaging.endpoints.typedmessages.ITypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.TypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.TypedRequestReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.messagingsystembase.IOutputChannel;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

/**
 * 
 * PI is calculated as the integral. The code takes an interval and calculates its value based on the formula
 * described here <a
 * href="http://en.wikipedia.org/wiki/Pi#Geometry_and_trigonometry">http://en.wikipedia.org/wiki
 * /Pi#Geometry_and_trigonometry</a>
 * 
 * @author mga
 * 
 */
public class PiCalculator {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: PiCalculator port weight");
        }

        String serverAddress = "tcp://127.0.0.1:" + args[0];
        int weight = Integer.parseInt(args[1]);

        // Create TCP messaging for sending and receiving requests.
        IMessagingSystemFactory messagingFactory = new TcpMessagingSystemFactory();

        String loadBalancerAddress = "tcp://127.0.0.1:8061";
        // Register the service to the load balancer
        IOutputChannel serviceRegistrationOutputChannel =
                messagingFactory.createOutputChannel(loadBalancerAddress);
        ITypedMessagesFactory senderFactory = new TypedMessagesFactory();

        ITypedMessageSender<WeightedServiceRegistration> sender =
                senderFactory.createTypedMessageSender(WeightedServiceRegistration.class);

        sender.attachOutputChannel(serviceRegistrationOutputChannel);
        sender.sendMessage(new WeightedServiceRegistration(serverAddress, weight));
        sender.detachOutputChannel();

        // Create typed message receiver to receive requests.
        // It receives request messages of type Range and sends back
        // response messages of type double.
        IDuplexTypedMessagesFactory receiverFactory = new DuplexTypedMessagesFactory();
        IDuplexTypedMessageReceiver<Double, Range> receiver =
                receiverFactory.createDuplexTypedMessageReceiver(Double.class, Range.class);

        receiver.messageReceived().subscribe(new EventHandler<TypedRequestReceivedEventArgs<Range>>() {
            private int calculationCount = 0;

            @Override
            public void onEvent(Object sender, TypedRequestReceivedEventArgs<Range> e) {
                double from = e.getRequestMessage().getFrom();
                double to = e.getRequestMessage().getTo();
                System.out.println("Request " + (++calculationCount) + " calculate from: " + from + " to: "
                        + to);
                double result = 0;
                double aDx = 0.000_000_001;
                for (double x = e.getRequestMessage().getFrom(); x < e.getRequestMessage().getTo(); x += aDx) {
                    result += 2 * Math.sqrt(1 - x * x) * aDx;
                }

                // Response back the result.
                IDuplexTypedMessageReceiver<Double, Range> aReceiver =
                        (IDuplexTypedMessageReceiver<Double, Range>) sender;
                try {
                    aReceiver.sendResponseMessage(e.getResponseReceiverId(), result);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        IDuplexInputChannel inputChannel = messagingFactory.createDuplexInputChannel(serverAddress);
        receiver.attachDuplexInputChannel(inputChannel);

        System.out.println("Root Square Calculator listening to " + serverAddress
                + " is running.\r\nPress ENTER to stop.");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        // Detach the input channel and stop listening.
        receiver.detachDuplexInputChannel();
    }
}
