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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.javax.annotation.Nullable;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import brooklyn.location.jclouds.BrooklynImageChooser;

public class AmbariImageChooserTest {

    @Test
    public void testImageWithNoOsReturnsDefaultScore() {
        Image image = mockImage(null);

        double scoreBrooklyn = new BrooklynImageChooser().score(image);
        double scoreAmbari = new AmbariImageChooser("2.1.0").score(image);

        assertEquals(scoreBrooklyn, scoreAmbari);
    }

    @Test
    public void testImageWithNoOsFamilyReturnsDefaultScore() {
        Image image = mockImage(new OperatingSystem(null, null, null, null, "os-description", true));

        double scoreBrooklyn = new BrooklynImageChooser().score(image);
        double scoreAmbari = new AmbariImageChooser("2.1.0").score(image);

        assertEquals(scoreBrooklyn, scoreAmbari);
    }

    @Test
    public void testImageWithOsFamilyNotSupportedReturnsNegativeScore() {
        final ImmutableList<OsFamily> supportedOsFamily = ImmutableList.of(
                OsFamily.CENTOS,
                OsFamily.UBUNTU,
                OsFamily.DEBIAN,
                OsFamily.SUSE
        );

        for (OsFamily osFamily : OsFamily.values()) {
            if (supportedOsFamily.contains(osFamily)) {
                continue;
            }

            Image image = mockImage(new OperatingSystem(osFamily, null, null, null, "os-description", true));

            double scoreAmbari = new AmbariImageChooser("2.1.0").score(image);

            assertEquals(-50D, scoreAmbari);
        }
    }

    @Test
    public void testImageWithOsVersionNotSupportedReturnsBadScore() {
        final ImmutableList<Image> images = ImmutableList.of(
                mockImage(new OperatingSystem(OsFamily.CENTOS, null, "4", null, "os-description", true)),
                mockImage(new OperatingSystem(OsFamily.UBUNTU, null, "11.02", null, "os-description", true)),
                mockImage(new OperatingSystem(OsFamily.DEBIAN, null, "6", null, "os-description", true)),
                mockImage(new OperatingSystem(OsFamily.SUSE, null, "10.2", null, "os-description", true))
        );

        for (Image image : images) {
            double scoreBrooklyn = new BrooklynImageChooser().score(image);
            double scoreAmbari = new AmbariImageChooser("2.1.0").score(image);

            assertEquals(scoreBrooklyn - 15, scoreAmbari, String.format(
                    "Image %s version %s does not get the expected score",
                    image.getOperatingSystem().getFamily(),
                    image.getOperatingSystem().getVersion()));
        }
    }

    @Test
    public void testImageWithOsVersionSupported() {
        final ImmutableMap<Image, ImmutableMap<String, Integer>> imagesToVersionDelta = ImmutableMap.<Image, ImmutableMap<String, Integer>>builder()
                .put(
                        mockImage(new OperatingSystem(OsFamily.CENTOS, null, "5", null, "os-description", true)),
                        ImmutableMap.of(
                                "2.0.1", 10,
                                "2.1.0", -10,
                                "2.1.2", -10
                        )
                )
                .put(
                        mockImage(new OperatingSystem(OsFamily.CENTOS, null, "6", null, "os-description", true)),
                        ImmutableMap.of(
                                "2.0.1", 10,
                                "2.1.0", 10,
                                "2.1.2", 10
                        )
                )
                .put(
                        mockImage(new OperatingSystem(OsFamily.CENTOS, null, "7", null, "os-description", true)),
                        ImmutableMap.of(
                                "2.0.1", 10,
                                "2.1.0", 10,
                                "2.1.2", 10
                        )
                )
                .put(
                        mockImage(new OperatingSystem(OsFamily.UBUNTU, null, "12.02", null, "os-description", true)),
                        ImmutableMap.of(
                                "1.5.0", -10,
                                "1.7.0", 10,
                                "2.1.2", 10
                        )
                )
                .put(
                        mockImage(new OperatingSystem(OsFamily.UBUNTU, null, "14.04", null, "os-description", true)),
                        ImmutableMap.of(
                                "1.7.0", -10,
                                "2.1.2", 10,
                                "2.2", 10
                        )
                )
                .put(
                        mockImage(new OperatingSystem(OsFamily.DEBIAN, null, "7", null, "os-description", true)),
                        ImmutableMap.of(
                                "1.7.0", -10,
                                "2.1.2", 10,
                                "2.2", 10
                        )
                )
                .put(
                        mockImage(new OperatingSystem(OsFamily.SUSE, null, "11", null, "os-description", true)),
                        ImmutableMap.of(
                                "1.5.0", 10,
                                "1.7.0", 10,
                                "2.1.2", 10,
                                "2.2", 10
                        )
                )
                .build();

        for (Map.Entry<Image, ImmutableMap<String, Integer>> imageToVersionDelta : imagesToVersionDelta.entrySet()) {
            for (Map.Entry<String, Integer> versionToDelta : imageToVersionDelta.getValue().entrySet()) {
                double scoreBrooklyn = new BrooklynImageChooser().score(imageToVersionDelta.getKey());
                double scoreAmbari = new AmbariImageChooser(versionToDelta.getKey()).score(imageToVersionDelta.getKey());

                assertEquals(scoreBrooklyn + versionToDelta.getValue(), scoreAmbari, String.format(
                        "Image %s version %s does not get the expected score",
                        imageToVersionDelta.getKey().getOperatingSystem().getFamily(),
                        imageToVersionDelta.getKey().getOperatingSystem().getVersion()));
            }

        }
    }

    private Image mockImage(@Nullable OperatingSystem os) {
        Image image = mock(Image.class);
        when(image.getId()).thenReturn("test-id");
        when(image.getStatus()).thenReturn(Image.Status.AVAILABLE);
        if (os != null) {
            when(image.getOperatingSystem()).thenReturn(os);
        }

        return image;
    }
}
