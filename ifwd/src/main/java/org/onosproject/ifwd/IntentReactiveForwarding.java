/*
 * Copyright 2014 Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.ifwd;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.EnumSet;

import static org.onosproject.net.flow.DefaultTrafficSelector.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * WORK-IN-PROGRESS: Sample reactive forwarding application using intent framework.
 */
@Component(immediate = true)
public class IntentReactiveForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PathService pathService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private ApplicationId appId;

    private static final int DROP_RULE_TIMEOUT = 300;

    private static final EnumSet<IntentState> WITHDRAWN_STATES = EnumSet.of(IntentState.WITHDRAWN,
                                                                            IntentState.WITHDRAWING,
                                                                            IntentState.WITHDRAW_REQ);

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.ifwd");

        packetService.addProcessor(processor, PacketProcessor.director(2));

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
            HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(dstId);
            if (dst == null) {
                flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            setUpConnectivity(context, srcId, dstId);
            forwardPacketToDst(context, dst);
        }
    }

    // Floods the specified packet if permissible.
    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    private void forwardPacketToDst(PacketContext context, Host dst) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
                                                          treatment, context.inPacket().unparsed());
        packetService.emit(packet);
        log.info("sending packet: {}", packet);
    }

    // Install a rule forwarding the packet to the specified port.
    private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId) {
        //Selectors match on the MAC addresses
        TrafficSelector selectorSrcDst = builder()
                .matchEthSrc(srcId.mac())
                .matchEthDst(dstId.mac())
                .build();
        TrafficSelector selectorDstSrc = builder()
                .matchEthSrc(dstId.mac())
                .matchEthDst(srcId.mac())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        // We need to define 2 keys since we use two PointToPoint intents
        Key keySrcDst, keyDstSrc;

        keySrcDst = Key.of(srcId.toString() + dstId.toString(), appId);
        keyDstSrc = Key.of(dstId.toString() + srcId.toString(), appId);

        ConnectivityIntent intentSrcDst =
                (ConnectivityIntent) intentService.getIntent(keySrcDst);
        ConnectivityIntent intentDstSrc =
                (ConnectivityIntent) intentService.getIntent(keyDstSrc);

        // Calculate the connect point each host is connected to
        FilteredConnectPoint filteredIngressCP = getFilteredConnectPoint(srcId);
        FilteredConnectPoint filteredEgressCP = getFilteredConnectPoint(dstId);

        submitIntent(selectorSrcDst, treatment, keySrcDst, intentSrcDst,
                     filteredIngressCP, filteredEgressCP);
        submitIntent(selectorDstSrc, treatment, keyDstSrc, intentDstSrc,
                     filteredEgressCP, filteredIngressCP);
    }

    private void submitIntent(TrafficSelector selector, TrafficTreatment treatment,
                              Key key, ConnectivityIntent intent,
                              FilteredConnectPoint filteredIngressCP,
                              FilteredConnectPoint filteredEgressCP) {
        if (intent == null || WITHDRAWN_STATES.contains(intentService.getIntentState(key))
                || intentService.getIntentState(key) == IntentState.FAILED) {
            //The intent is in the withdrawn state, we need to re-add it
            PointToPointIntent ptpIntent = PointToPointIntent.builder()
                    .appId(appId)
                    .key(key)
                    .filteredIngressPoint(filteredIngressCP)
                    .filteredEgressPoint(filteredEgressCP)
                    .selector(selector)
                    .treatment(treatment)
                    .build();
            intentService.submit(ptpIntent);
        }
        //TODO check master for IntentState.FAILED state handling with flowObjective
    }

    private FilteredConnectPoint getFilteredConnectPoint(HostId hostId) {
        Host h = hostService.getHost(hostId);
        ConnectPoint cp = pathService.getPaths(hostId, h.location().elementId())
                .iterator().next().links().get(0).dst();
        return new FilteredConnectPoint(cp);
    }

}
