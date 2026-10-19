/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.controller.liveness;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import io.mosip.registration.liveness.active.ChallengeType;
import io.mosip.registration.liveness.api.FailureReason;
import io.mosip.registration.liveness.api.FeedbackCode;
import io.mosip.registration.liveness.api.GuidanceTip;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

/**
 * Renders the face liveness status pane of the Registration Client in each state, in English, French and Arabic, to
 * PNG files. The pane is the real component of the client, with the real message and label files; the camera preview
 * is not part of it, so no face appears. Run by render_pane.ps1.
 *
 * <p>Arguments: the resources folder of the client module, then the output folder.
 */
public final class RenderPane {

	private static final String[] LANGUAGES = { "en", "fr", "ar" };

	private RenderPane() {
	}

	public static void main(String[] args) throws Exception {
		Path resources = Paths.get(args[0]);
		Path output = Paths.get(args[1]);
		CompletableFuture<Void> started = new CompletableFuture<>();
		Platform.startup(() -> started.complete(null));
		started.get();
		for (String language : LANGUAGES) {
			Properties messages = load(resources.resolve("messages_" + language + ".properties"));
			Properties labels = load(resources.resolve("labels_" + language + ".properties"));
			Path folder = Files.createDirectories(output.resolve(language));
			CompletableFuture<Void> done = new CompletableFuture<>();
			Platform.runLater(() -> {
				try {
					renderAll(messages, labels, "ar".equals(language), folder);
					done.complete(null);
				} catch (RuntimeException exception) {
					done.completeExceptionally(exception);
				}
			});
			done.get();
			System.out.println(language + ": written to " + folder);
		}
		Platform.exit();
	}

	private static void renderAll(Properties messages, Properties labels, boolean rightToLeft, Path folder) {
		Renderer r = new Renderer(messages, labels, rightToLeft, folder);
		r.render("p01-checking", Workflow.RESIDENT, p -> p.show(passive(FeedbackCode.CHECKING, null, 0.4)));
		r.render("p02-guidance-lighting", Workflow.RESIDENT,
				p -> p.show(passive(FeedbackCode.LOW_QUALITY, GuidanceTip.LIGHTING, 0.2)));
		r.render("p03-hold-still", Workflow.RESIDENT,
				p -> p.show(active(FeedbackCode.HOLD_STILL, ChallengeType.BLINK, 0.1, 1, 2)));
		r.render("p04-challenge-blink", Workflow.RESIDENT,
				p -> p.show(active(FeedbackCode.CHALLENGE, ChallengeType.BLINK, 0.3, 1, 2)));
		r.render("p05-challenge-turn", Workflow.RESIDENT,
				p -> p.show(active(FeedbackCode.CHALLENGE, ChallengeType.TURN_LEFT, 0.6, 2, 2)));
		r.render("p06-success", Workflow.RESIDENT, p -> p.show(update(SessionState.PASSED, FeedbackCode.SUCCESS, null,
				null, 1, null, false, null, 0, 0)));
		r.render("p07-failed-retry", Workflow.RESIDENT,
				p -> p.show(failed(FailureReason.CHALLENGE_TIMEOUT, null, true)));
		r.render("p08-attack-generic", Workflow.RESIDENT,
				p -> p.show(failed(FailureReason.PAD_ATTACK_DETECTED, null, true)));
		r.render("p09-max-attempts-exception", Workflow.RESIDENT, p -> p.showMaxAttempts(true));
		r.render("p10-device-unavailable", Workflow.RESIDENT, p -> p.show(update(SessionState.FAILED,
				FeedbackCode.DEVICE_ERROR, null, null, 0, FailureReason.DEVICE_ERROR, true, null, 0, 0)));
		r.render("p11-diagnostic", Workflow.RESIDENT, p -> p.show(update(SessionState.PASSIVE_CHECK,
				FeedbackCode.CHECKING, null, null, 0.4, null, false, 0.412, 0, 0)));
		r.render("p12-exception-captured", Workflow.RESIDENT, LivenessStatusPane::showExceptionCaptured);
		r.render("p13-max-attempts-blocked", Workflow.RESIDENT, p -> p.showMaxAttempts(false));
		r.render("p14-operator-checking", Workflow.OPERATOR, p -> p.show(passive(FeedbackCode.CHECKING, null, 0.4)));
		r.render("p15-operator-challenge", Workflow.OPERATOR,
				p -> p.show(active(FeedbackCode.CHALLENGE, ChallengeType.BLINK, 0.3, 1, 1)));
		r.render("p16-supervisor-verified", Workflow.SUPERVISOR, p -> p.show(update(SessionState.PASSED,
				FeedbackCode.SUCCESS, null, null, 1, null, false, null, 0, 0)));
		r.render("p17-max-attempts-auth", Workflow.OPERATOR, p -> p.showMaxAttempts(false));
	}

	private static LivenessUpdate passive(FeedbackCode feedback, GuidanceTip tip, double progress) {
		return update(SessionState.PASSIVE_CHECK, feedback, tip, null, progress, null, false, null, 0, 0);
	}

	private static LivenessUpdate active(FeedbackCode feedback, ChallengeType challenge, double progress, int index,
			int count) {
		return update(SessionState.ACTIVE_CHALLENGE, feedback, null, challenge, progress, null, false, null, index,
				count);
	}

	private static LivenessUpdate failed(FailureReason reason, GuidanceTip tip, boolean retry) {
		return update(SessionState.FAILED, FeedbackCode.FAILED, tip, null, 0, reason, retry, null, 0, 0);
	}

	/** An update of the first of three attempts. */
	private static LivenessUpdate update(SessionState state, FeedbackCode feedback, GuidanceTip tip,
			ChallengeType challenge, double progress, FailureReason reason, boolean retry, Double score, int index,
			int count) {
		return new LivenessUpdate(state, feedback, tip, challenge, progress, 1, 3, reason, retry, false, score, index,
				count);
	}

	private static Properties load(Path file) {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			properties.load(in);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
		return properties;
	}

	private static final class Renderer {
		private final Properties messages;
		private final Properties labels;
		private final boolean rightToLeft;
		private final Path folder;

		Renderer(Properties messages, Properties labels, boolean rightToLeft, Path folder) {
			this.messages = messages;
			this.labels = labels;
			this.rightToLeft = rightToLeft;
			this.folder = folder;
		}

		void render(String name, Workflow workflow, Consumer<LivenessStatusPane> state) {
			LivenessStatusPane pane = new LivenessStatusPane(workflow,
					key -> messages.getProperty(key, key), key -> labels.getProperty(key, key));
			if (rightToLeft) {
				pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			}
			state.accept(pane);
			double width = workflow == Workflow.RESIDENT ? 720 : 520;
			pane.setPrefWidth(width);
			pane.setMaxWidth(width);
			StackPane root = new StackPane(pane);
			root.setPadding(new Insets(16));
			root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI';");
			Scene scene = new Scene(root);
			root.applyCss();
			root.layout();
			WritableImage image = scene.snapshot(null);
			write(image, folder.resolve(name + ".png"));
		}

		private static void write(WritableImage image, Path file) {
			int w = (int) image.getWidth();
			int h = (int) image.getHeight();
			BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			PixelReader reader = image.getPixelReader();
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					buffered.setRGB(x, y, reader.getArgb(x, y));
				}
			}
			try {
				ImageIO.write(buffered, "png", file.toFile());
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}
	}
}
