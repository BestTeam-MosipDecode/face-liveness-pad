/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.api;

import java.util.Optional;

import io.mosip.registration.liveness.image.BgrImage;

/**
 * Turns an encoded frame (JPEG or PNG bytes from the device) into pixels. It is the platform adapter of the engine:
 * {@code ImageIoFrameDecoder} on the desktop, an adapter over the camera image on Android. The engine core never
 * decodes images itself.
 */
@FunctionalInterface
public interface FrameDecoder {

    /**
     * Decodes one frame and applies its orientation, so that the face is upright.
     *
     * @param encoded the encoded bytes, possibly null or empty
     * @return the decoded image, or empty when the bytes are not a readable image. Never throws.
     */
    Optional<BgrImage> decode(byte[] encoded);
}
