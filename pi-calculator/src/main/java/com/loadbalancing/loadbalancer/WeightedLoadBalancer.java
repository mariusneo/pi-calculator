/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved. NTS PROPRIETARY/CONFIDENTIAL. Use is
 * subject to NTS License Agreement. Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria Homepage:
 * www.ntswincash.com
 */

package com.loadbalancing.loadbalancer;

import java.util.Scanner;

import eneter.messaging.messagingsystems.messagingsystembase.IDuplexInputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;

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
        WeightedTaskLoadBalancer loadBalancer = new WeightedTaskLoadBalancer(messagingFactory);

        String[] availableServers =
                new String[] {"tcp://127.0.0.1:8061", "tcp://127.0.0.1:8062", "tcp://127.0.0.1:8063"};

        
        loadBalancer.addDuplexOutputChannel("tcp://127.0.0.1:8061", 10);
        loadBalancer.addDuplexOutputChannel("tcp://127.0.0.1:8062", 2);
        loadBalancer.addDuplexOutputChannel("tcp://127.0.0.1:8063", 1);

        // Create input channel that will listen to requests from clients.
        String loadBalancerAddress = "tcp://127.0.0.1:8060";
        IDuplexInputChannel inputChannel = messagingFactory.createDuplexInputChannel(loadBalancerAddress);

        loadBalancer.attachDuplexInputChannel(inputChannel);

        System.out.println("Load balancer listening to " + loadBalancerAddress
                + " is running.\r\nPress ENTER to stop.");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        // Detach the input channel and stop listening.
        loadBalancer.detachDuplexInputChannel();
    }
}
