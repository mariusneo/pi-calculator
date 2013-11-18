/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved.
 * NTS PROPRIETARY/CONFIDENTIAL. Use is subject to NTS License Agreement.
 * Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria
 * Homepage: www.ntswincash.com
 */

package com.loadbalancing.loadbalancer;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.nodes.loadbalancer.ILoadBalancer;
import eneter.messaging.nodes.loadbalancer.ILoadBalancerFactory;

/**
 * @author mga
 *
 */
public class WeightedTaskLoadBalancerFactory implements ILoadBalancerFactory{

    private IMessagingSystemFactory duplexOutputChannelFactory;
    
    public WeightedTaskLoadBalancerFactory(IMessagingSystemFactory duplexOutputChannelFactory){
        EneterTrace trace = EneterTrace.entering();
        
        try{
            this.duplexOutputChannelFactory = duplexOutputChannelFactory;
        } finally{
            EneterTrace.leaving(trace);
        }
    }
    
    @Override
    public ILoadBalancer createLoadBalancer() {
        EneterTrace trace = EneterTrace.entering();
        
        try{
            return new WeightedTaskLoadBalancer(this.duplexOutputChannelFactory);
        }finally{
            EneterTrace.leaving(trace);
        }
    }

}
