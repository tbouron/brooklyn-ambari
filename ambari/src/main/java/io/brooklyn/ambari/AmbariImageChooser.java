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

package io.brooklyn.ambari;

import java.util.Map;

import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import brooklyn.location.jclouds.BrooklynImageChooser;
import brooklyn.util.text.ComparableVersion;

public class AmbariImageChooser extends BrooklynImageChooser {

    private static final Map<OsFamily, Map<String, String>> validRanges = ImmutableMap.<OsFamily, Map<String, String>>of(
            OsFamily.CENTOS, ImmutableMap.of(
                    "[5,6)", "(,2.1.0)",
                    "[6,)", "(,)"
            ),
            OsFamily.UBUNTU, ImmutableMap.of(
                    "[12,14)", "[1.7.0,)",
                    "[14,)", "[2.1.2,)"
            ),
            OsFamily.DEBIAN, ImmutableMap.of(
                    "[7,)", "[2.1.2,)"
            ),
            OsFamily.SUSE, ImmutableMap.of(
                    "[11,)", "(,)"
            )
    );

    private final ComparableVersion comparableAmbariVersion;

    public AmbariImageChooser(String ambariVersion) {
        this.comparableAmbariVersion = new ComparableVersion(ambariVersion);
    }

    @Override
    public AmbariImageChooser clone() {
        return new AmbariImageChooser(this.comparableAmbariVersion.version);
    }

    @Override
    public double score(Image img) {
        final OperatingSystem os = img.getOperatingSystem();

        double score = super.score(img);

        if (os == null) {
            return score;
        }
        if (os.getFamily() == null) {
            return score;
        }

        // At that point, let do our custom logic here
        if (!validRanges.containsKey(os.getFamily())) {
            return -50;
        }

        boolean foundOsRange = false;
        for (Map.Entry<String, String> ranges : validRanges.get(os.getFamily()).entrySet()) {
            if (new ComparableVersion(os.getVersion()).isInRange(ranges.getKey())) {
                foundOsRange = true;
                score += comparableAmbariVersion.isInRange(ranges.getValue()) ? 10 : -10;
                break;
            }
        }
        if (!foundOsRange) {
            score -= 15;
        }

        return score;
    }

    @Override
    public Function<Iterable<? extends Image>, Image> chooser() {
        return imageChooserFromOrdering(orderingScoredWithoutDefaults());
    }
}
