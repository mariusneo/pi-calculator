/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved. NTS PROPRIETARY/CONFIDENTIAL. Use is
 * subject to NTS License Agreement. Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria Homepage:
 * www.ntswincash.com
 */

package com.loadbalancing.client;

import java.util.Scanner;

import com.loadbalancing.model.Range;

import eneter.messaging.dataprocessing.serializing.ISerializer;
import eneter.messaging.endpoints.typedmessages.DuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessageSender;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.TypedResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;

/**
 * @author mga
 * 
 */
public class Client {
    public static void main(String[] args) throws Exception {
        // Create TCP messaging for the communication.
        // Note: Requests are sent to the balancer that will forward them
        // to available services.
        IMessagingSystemFactory messagingFactory = new TcpMessagingSystemFactory();
        IDuplexOutputChannel outputChannel =
                messagingFactory.createDuplexOutputChannel("tcp://127.0.0.1:8060");

        // Create sender to send requests
        IDuplexTypedMessagesFactory senderFactory = new DuplexTypedMessagesFactory();

        IDuplexTypedMessageSender<Double, Range> sender =
                senderFactory.createDuplexTypedMessageSender(Double.class, Range.class);

        sender.responseReceived().subscribe(new EventHandler<TypedResponseReceivedEventArgs<Double>>() {
            Double calculatedPi = new Double(0);

            @Override
            public void onEvent(Object o, TypedResponseReceivedEventArgs<Double> e) {
                // Receive responses (calculations for ranges) and calculate PI.
                calculatedPi += e.getResponseMessage();
                System.out.println(calculatedPi);
            }
        });

        sender.attachDuplexOutputChannel(outputChannel);

        calculatePi(sender);
        
        System.out.println("Press ENTER to stop.");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        // Detach the input channel and stop listening.
        sender.detachDuplexOutputChannel();

    }

    public static void calculatePi(IDuplexTypedMessageSender<Double, Range> sender) throws Exception {
        // Split calculation of PI to 400 ranges and sends them for the calculation.
        for (double i = -1.0; i < 1.0; i += 0.005) {
            Range anInterval = new Range(i, i + 0.005);

            System.out.println("Sending interval from : " + anInterval.getFrom() + " to: " + anInterval.getTo());
            try {
                // simulate a network delay
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sender.sendRequestMessage(anInterval);
        }
    }
}
