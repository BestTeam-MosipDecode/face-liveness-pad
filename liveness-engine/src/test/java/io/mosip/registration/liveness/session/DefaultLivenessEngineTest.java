/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.LivenessEngine;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessSession;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.platform.desktop.ImageIoFrameDecoder;

/** With the real models: no face image is used, only a grey frame. */
class DefaultLivenessEngineTest {

    static byte[] greyJpeg() throws IOException {
        BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 320, 240);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    static LivenessEventListener into(BlockingQueue<LivenessUpdate> queue) {
        return new LivenessEventListener() {
            @Override
            public void onUpdate(LivenessUpdate update) {
                queue.add(update);
            }
        };
    }

    @Test
    void startsAndReportsThatNoFaceIsSeen() throws Exception {
        try (LivenessEngine engine = LivenessEngine.create(new ImageIoFrameDecoder())) {
            assertEquals("1.1.0", engine.modelSetVersion());
            BlockingQueue<LivenessUpdate> updates = new LinkedBlockingQueue<>();
            LivenessSession session = engine.newSession(Workflow.OPERATOR, LivenessPolicy.defaults(), into(updates));
            session.submit(greyJpeg(), 100);
            LivenessUpdate update = updates.poll(10, TimeUnit.SECONDS);
            assertNotNull(update, "no update within 10 s");
            assertEquals(SessionState.PASSIVE_CHECK, update.state());
            assertEquals(FeedbackCode.NO_FACE, update.feedback());
            assertEquals(1, engine.metrics().framesAnalysed());
            assertEquals(1, engine.metrics().sessions());
        }
    }

    @Test
    void aNewSessionClosesThePreviousOne() throws Exception {
        try (LivenessEngine engine = LivenessEngine.create(new ImageIoFrameDecoder())) {
            BlockingQueue<LivenessUpdate> first = new LinkedBlockingQueue<>();
            BlockingQueue<LivenessUpdate> second = new LinkedBlockingQueue<>();
            LivenessSession a = engine.newSession(Workflow.RESIDENT, LivenessPolicy.defaults(), into(first));
            LivenessSession b = engine.newSession(Workflow.RESIDENT, LivenessPolicy.defaults(), into(second));
            a.submit(greyJpeg(), 100);
            b.submit(greyJpeg(), 100);
            assertNotNull(second.poll(10, TimeUnit.SECONDS));
            assertNull(first.poll(200, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void aClosedEngineRefusesSessions() throws Exception {
        LivenessEngine engine = LivenessEngine.create(new ImageIoFrameDecoder());
        engine.close();
        engine.close();
        assertThrows(IllegalStateException.class,
            () -> engine.newSession(Workflow.RESIDENT, LivenessPolicy.defaults(), new LivenessEventListener() {
            }));
    }
}
