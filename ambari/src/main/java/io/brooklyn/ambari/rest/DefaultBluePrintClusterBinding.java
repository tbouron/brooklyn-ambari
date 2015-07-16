/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.brooklyn.ambari.rest;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import brooklyn.util.collections.Jsonya;
import io.brooklyn.ambari.rest.RecommendationResponse.BlueprintClusterBinding;
import io.brooklyn.ambari.rest.RecommendationResponse.ClusterBindingHostGroup;

/**
 * @author duncangrant
 */
public class DefaultBluePrintClusterBinding implements Mappable {
    private List<Mappable> hostGroups = new LinkedList<Mappable>();
    private String bluePrintName;

    private DefaultBluePrintClusterBinding(BlueprintClusterBinding blueprintClusterBinding) {
        for (ClusterBindingHostGroup hostGroup : blueprintClusterBinding.host_groups) {
            hostGroups.add(new HostGroup(hostGroup));
        }
    }

    private DefaultBluePrintClusterBinding(AmbariConfig config) {
        Map<String, List<String>> hostGroupsToHosts = config.getHostGroupsToHosts();
        Set<Map.Entry<String, List<String>>> entries = hostGroupsToHosts.entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            hostGroups.add(new HostGroup(entry.getKey(), entry.getValue()));
        }
    }

    public static DefaultBluePrintClusterBinding createFromConfig(AmbariConfig config) {
        return new DefaultBluePrintClusterBinding(config);
    }

    public static DefaultBluePrintClusterBinding createFromRecommendation(BlueprintClusterBinding blueprintClusterBinding) {
        return new DefaultBluePrintClusterBinding(blueprintClusterBinding);
    }

    public String toJson() {
        return Jsonya.newInstance().add(this.asMap()).root().toString();
    }

    @Override
    public Map<?,?> asMap() {
        return ImmutableMap.of("blueprint", bluePrintName,
                "default_password", "admin",
                "host_groups", Mappables.toMaps(hostGroups));
    }

    public void setBluePrintName(String bluePrintName) {
        this.bluePrintName = bluePrintName;
    }

    private static class HostGroup implements Mappable {

        private final String name;
        private final List<Host> hosts = new LinkedList<Host>();

        public HostGroup(ClusterBindingHostGroup hostGroup) {
            name = hostGroup.name;
            for (Map<?,?> host : hostGroup.hosts) {
                hosts.add(new Host(host));
            }
        }

        public HostGroup(String name, List<String> hosts) {
            this.name = name;
            for (String host : hosts) {
                this.hosts.add(new Host(ImmutableMap.of("fqdn", host)));
            }

        }

        @Override
        public Map<?,?> asMap() {
            return ImmutableMap.of("name", name, "hosts", Mappables.toMaps(hosts));

        }
    }

    private static class Host implements Mappable {

        private final Map<?,?> hostParams;

        public Host(Map<?,?> host) {
            hostParams = ImmutableMap.copyOf(host);
        }

        @Override
        public Map<?,?> asMap() {
            return hostParams;
        }
    }
}