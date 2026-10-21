/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.liveness.demo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.mosip.registration.controller.liveness.DemoPanes;
import io.mosip.registration.controller.liveness.LivenessStatusPane;
import io.mosip.registration.liveness.api.DeviceEvent;
import io.mosip.registration.liveness.api.LivenessEngine;
import io.mosip.registration.liveness.api.LivenessEvent;
import io.mosip.registration.liveness.api.LivenessEventListener;
import io.mosip.registration.liveness.api.LivenessPolicy;
import io.mosip.registration.liveness.api.LivenessSession;
import io.mosip.registration.liveness.api.LivenessUpdate;
import io.mosip.registration.liveness.api.ModelIntegrityException;
import io.mosip.registration.liveness.api.SessionState;
import io.mosip.registration.liveness.api.Workflow;
import io.mosip.registration.liveness.platform.desktop.ImageIoFrameDecoder;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Face liveness demo: the frames of the simulated SBI device go to the liveness engine, and the status pane of the
 * modified Registration Client shows each update, in the workflow and the language chosen. After a verified liveness,
 * the face is captured with RCAPTURE, as in the client. Everything runs on this workstation.
 *
 * <p>Optional arguments: {@code --workflow=RESIDENT|OPERATOR|SUPERVISOR}, {@code --language=en|fr|ar}, and
 * {@code --start} to start a session as soon as the engine is ready.
 */
public final class DemoApp extends Application {

	private static final int FIRST_PORT = 4501;
	private static final int LAST_PORT = 4510;
	private static final double PREVIEW_WIDTH = 400;
	private static final double PREVIEW_HEIGHT = 300;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();

	static {
		LANGUAGES.put("en", "English");
		LANGUAGES.put("fr", "Français");
		LANGUAGES.put("ar", "العربية");
	}

	private final ExecutorService worker = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "liveness-demo");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
	private final AtomicBoolean captureRequested = new AtomicBoolean();
	private final AtomicBoolean streaming = new AtomicBoolean();
	private final Map<String, Properties> texts = new HashMap<>();

	private volatile LivenessEngine engine;
	private volatile SbiClient device;
	private volatile LivenessSession session;
	private volatile byte[] lastStreamed;
	private volatile boolean running;

	private final ComboBox<Workflow> workflowBox = new ComboBox<>();
	private final ComboBox<String> languageBox = new ComboBox<>();
	private final Button start = new Button("Start");
	private final Button stop = new Button("Stop");
	private final ImageView preview = new ImageView();
	private final StackPane paneHolder = new StackPane();
	private final Label info = new Label();
	private final Label status = new Label("Verifying the models...");
	private final ListView<String> audit = new ListView<>();
	private final CheckBox activeBox = new CheckBox("Active check (challenges)");
	private final CheckBox passivePassBox = new CheckBox("Allow a passive pass (0.85)");
	private final Spinner<Integer> challengesSpinner = new Spinner<>(1, 4, 2);
	private final Spinner<Integer> attemptsSpinner = new Spinner<>(1, 10, 3);
	private final CheckBox exceptionBox = new CheckBox("Exception process after the last attempt (resident)");
	private final CheckBox diagnosticBox = new CheckBox("Diagnostic mode");
	private final TextArea configView = new TextArea();

	private LivenessStatusPane pane;
	private boolean exceptionAllowed;
	private boolean exceptionCaptured;
	private boolean startOnLoad;

	@Override
	public void start(Stage stage) {
		stage.setTitle("Face liveness demo, MOSIP Registration Client");
		// Maximized, the window fits the work area of the screen, including a laptop screen with 150 % scaling.
		stage.setScene(new Scene(layout(), 1180, 640));
		stage.setMaximized(true);
		stage.show();
		Map<String, String> options = getParameters().getNamed();
		for (Workflow workflow : Workflow.values()) {
			if (workflow.name().equalsIgnoreCase(options.get("workflow"))) {
				workflowBox.setValue(workflow);
			}
		}
		if (LANGUAGES.containsKey(options.get("language"))) {
			languageBox.setValue(options.get("language"));
		}
		startOnLoad = getParameters().getUnnamed().contains("--start");
		showPane(newPane(workflowBox.getValue(), currentSettings().exceptionAllowed(workflowBox.getValue())));
		updateConfiguration();
		new AnimationTimer() {
			@Override
			public void handle(long now) {
				byte[] frame = latestFrame.getAndSet(null);
				if (frame != null) {
					preview.setImage(new Image(new ByteArrayInputStream(frame)));
				}
			}
		}.start();
		worker.execute(this::loadEngine);
	}

	@Override
	public void stop() {
		stopSession();
		LivenessEngine loaded = engine;
		if (loaded != null) {
			loaded.close();
		}
		worker.shutdownNow();
	}

	private BorderPane layout() {
		workflowBox.getItems().addAll(Workflow.RESIDENT, Workflow.OPERATOR, Workflow.SUPERVISOR);
		workflowBox.setValue(Workflow.RESIDENT);
		workflowBox.setConverter(converter(DemoApp::workflowName));
		languageBox.getItems().addAll(LANGUAGES.keySet());
		languageBox.setValue("en");
		languageBox.setConverter(converter(LANGUAGES::get));
		languageBox.valueProperty().addListener((observable, before, after) -> switchLanguage());
		start.setDisable(true);
		stop.setDisable(true);
		start.setOnAction(event -> startSession());
		stop.setOnAction(event -> stopSession());
		HBox top = new HBox(10, new Label("Workflow"), workflowBox, new Label("Language"), languageBox, start, stop);
		top.setAlignment(Pos.CENTER_LEFT);
		top.setPadding(new Insets(10));

		preview.setFitWidth(PREVIEW_WIDTH);
		preview.setFitHeight(PREVIEW_HEIGHT);
		preview.setPreserveRatio(true);
		StackPane previewFrame = new StackPane(preview);
		previewFrame.setMinSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
		previewFrame.setMaxSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
		previewFrame.setStyle("-fx-background-color: #20262e;");
		Label previewCaption = new Label("Preview: STREAM of the SBI face device. No frame is stored.");
		previewCaption.setStyle("-fx-text-fill: #666666;");
		VBox left = new VBox(8, previewFrame, previewCaption, settings());

		info.setWrapText(true);
		info.setMaxWidth(560);
		audit.setPrefHeight(130);
		TitledPane auditPane = new TitledPane("Audit events (no image, no score)", audit);
		auditPane.setCollapsible(false);
		configView.setEditable(false);
		configView.setPrefRowCount(8);
		configView.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
		TitledPane configPane = new TitledPane("Configuration keys of the client for these settings", configView);
		configPane.setCollapsible(false);
		paneHolder.setAlignment(Pos.TOP_LEFT);
		paneHolder.setPrefWidth(560);
		VBox right = new VBox(10, paneHolder, info, auditPane, configPane);
		VBox.setVgrow(auditPane, Priority.ALWAYS);
		HBox center = new HBox(16, left, right);
		center.setPadding(new Insets(0, 10, 10, 10));
		HBox.setHgrow(right, Priority.ALWAYS);

		status.setPadding(new Insets(6, 10, 6, 10));
		BorderPane root = new BorderPane(center, top, null, status, null);
		root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI';");
		return root;
	}

	private TitledPane settings() {
		activeBox.setSelected(true);
		exceptionBox.setSelected(true);
		passivePassBox.disableProperty().bind(activeBox.selectedProperty().not());
		challengesSpinner.disableProperty().bind(activeBox.selectedProperty().not());
		challengesSpinner.setPrefWidth(70);
		attemptsSpinner.setPrefWidth(70);
		for (CheckBox box : new CheckBox[] { activeBox, passivePassBox, exceptionBox, diagnosticBox }) {
			box.selectedProperty().addListener((observable, before, after) -> updateConfiguration());
		}
		challengesSpinner.valueProperty().addListener((observable, before, after) -> updateConfiguration());
		attemptsSpinner.valueProperty().addListener((observable, before, after) -> updateConfiguration());
		workflowBox.valueProperty().addListener((observable, before, after) -> updateConfiguration());
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(6);
		grid.add(activeBox, 0, 0, 2, 1);
		grid.add(passivePassBox, 0, 1, 2, 1);
		grid.add(new Label("Challenges per attempt"), 0, 2);
		grid.add(challengesSpinner, 1, 2);
		grid.add(new Label("Attempts"), 0, 3);
		grid.add(attemptsSpinner, 1, 3);
		grid.add(exceptionBox, 0, 4, 2, 1);
		grid.add(diagnosticBox, 0, 5, 2, 1);
		Label note = new Label("Applied at the next start. Without a passive pass, every session asks for the challenges.");
		note.setStyle("-fx-text-fill: #666666;");
		grid.add(note, 0, 6, 2, 1);
		TitledPane settings = new TitledPane("Settings", grid);
		settings.setCollapsible(false);
		settings.setMaxWidth(PREVIEW_WIDTH);
		return settings;
	}

	private DemoSettings currentSettings() {
		return new DemoSettings().activeEnabled(activeBox.isSelected()).passivePass(passivePassBox.isSelected())
				.minChallenges(challengesSpinner.getValue()).maxAttempts(attemptsSpinner.getValue())
				.exceptionAfterLastAttempt(exceptionBox.isSelected()).diagnostic(diagnosticBox.isSelected());
	}

	private void updateConfiguration() {
		configView.setText(String.join("\n", currentSettings().configurationLines(workflowBox.getValue())));
	}

	private void loadEngine() {
		try {
			LivenessEngine loaded = LivenessEngine.create(new ImageIoFrameDecoder());
			engine = loaded;
			Platform.runLater(() -> {
				status.setText(statusLine());
				start.setDisable(false);
				if (startOnLoad) {
					startSession();
				}
			});
		} catch (ModelIntegrityException | RuntimeException exception) {
			Platform.runLater(() -> status.setText("The engine did not start: " + exception.getMessage()));
		}
	}

	private String statusLine() {
		LivenessEngine loaded = engine;
		SbiClient client = device;
		String models = loaded == null ? "models not loaded" : "model set " + loaded.modelSetVersion() + ", verified at start";
		String sbi = client == null ? "face device not found yet" : "face device on port " + client.port() + ", SBI " + client.specVersion();
		return models + "  |  " + sbi + "  |  All processing on this workstation: frames, scores and images never leave it.";
	}

	private void startSession() {
		stopSession();
		DemoSettings settings = currentSettings();
		LivenessPolicy policy;
		try {
			policy = settings.policy();
		} catch (IllegalArgumentException exception) {
			info.setText(exception.getMessage());
			return;
		}
		Workflow workflow = workflowBox.getValue();
		showPane(newPane(workflow, settings.exceptionAllowed(workflow)));
		info.setText("");
		audit.getItems().clear();
		setRunning(true);
		captureRequested.set(false);
		LivenessSession created = engine.newSession(workflow, policy, new Listener());
		session = created;
		worker.execute(() -> openStream(created));
	}

	/** Finds the device if needed, then feeds its stream to the session until the stream ends or is stopped. */
	private void openStream(LivenessSession target) {
		if (!streaming.compareAndSet(false, true)) {
			return;
		}
		try {
			SbiClient client = device;
			if (client == null) {
				client = SbiClient.discover(FIRST_PORT, LAST_PORT);
				device = client;
				Platform.runLater(() -> status.setText(statusLine()));
			}
			client.stream(frame -> {
				latestFrame.set(frame);
				lastStreamed = frame;
				target.submit(frame, nowMs());
			});
			deviceStopped(target, DeviceEvent.STREAM_INTERRUPTED, "The stream of the face device ended.");
		} catch (IOException exception) {
			boolean found = device != null;
			deviceStopped(target, found ? DeviceEvent.STREAM_INTERRUPTED : DeviceEvent.UNAVAILABLE, found
					? "The stream of the face device stopped: " + exception.getMessage()
					: "No face device answers on ports " + FIRST_PORT + " to " + LAST_PORT
							+ ". Start mock-sbi in webcam mode (demo/README.md), then try again.");
		} finally {
			streaming.set(false);
		}
	}

	/** Reports an unexpected end of the stream to the session, as the client does, unless the capture was requested. */
	private void deviceStopped(LivenessSession target, DeviceEvent event, String message) {
		if (running && session == target && !captureRequested.get()) {
			target.deviceEvent(event, nowMs());
			Platform.runLater(() -> {
				info.setText(message);
				status.setText(statusLine());
			});
		}
	}

	private void stopSession() {
		setRunning(false);
		SbiClient client = device;
		if (client != null) {
			client.stopStream();
		}
		LivenessSession current = session;
		session = null;
		if (current != null) {
			current.close();
		}
		latestFrame.set(null);
		lastStreamed = null;
		preview.setImage(null);
	}

	private void setRunning(boolean value) {
		running = value;
		start.setDisable(value || engine == null);
		stop.setDisable(!value);
		workflowBox.setDisable(value);
	}

	/**
	 * RCAPTURE after liveness passed on the stream, then the check of the captured image, which makes the decision
	 * final; or RCAPTURE under the exception process, without a liveness decision. Then the end of the session.
	 */
	private void capture(boolean underException) {
		SbiClient client = device;
		LivenessSession current = session;
		if (client == null || current == null) {
			return;
		}
		String text;
		try {
			// The simulated device encodes the frame it streamed last into the record: the same picture as this one.
			byte[] streamed = lastStreamed;
			SbiClient.Capture captured = client.capture();
			text = String.format("Face captured with RCAPTURE: %s of %d KB, quality %s, signed by the device%s.",
					captured.isFaceRecord() ? "ISO/IEC 19794-5 record" : "record", captured.size() / 1024,
					captured.qualityScore(), captured.hashed() ? ", with its hash" : "");
			if (underException) {
				text += " Captured without a liveness decision: the registration is marked, the event is audited and a "
						+ "supervisor must authenticate before it is submitted.";
			} else if (current.checkCapture(streamed).accepted()) {
				text += " Captured image checked: one face, quality, score and the same face as on the stream.";
			} else {
				text = "The captured image did not pass the check.";
			}
		} catch (IOException exception) {
			text = "The capture failed: " + exception.getMessage();
		}
		String result = text;
		Platform.runLater(() -> {
			info.setText(result);
			stopSession();
		});
	}

	private void retry() {
		LivenessSession current = session;
		if (current == null) {
			return;
		}
		worker.execute(() -> {
			captureRequested.set(false);
			if (current.retry() && !streaming.get()) {
				openStream(current);
			}
		});
	}

	private LivenessStatusPane newPane(Workflow workflow, boolean exceptionAfterLastAttempt) {
		Properties messages = texts("messages", languageBox.getValue());
		Properties labels = texts("labels", languageBox.getValue());
		LivenessStatusPane created = DemoPanes.create(workflow, key -> messages.getProperty(key, key),
				key -> labels.getProperty(key, key));
		if ("ar".equals(languageBox.getValue())) {
			created.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		}
		created.setExceptionAllowed(exceptionAfterLastAttempt);
		created.setOnRetry(this::retry);
		created.setOnContinueUnderException(() -> {
			exceptionCaptured = true;
			pane.showExceptionCaptured();
			worker.execute(() -> capture(true));
		});
		created.setOnOtherSignIn(() -> {
			info.setText("Back to the sign-in screen, which offers the other methods of the role: password, "
					+ "fingerprint or iris, as configured on the server.");
			stopSession();
		});
		exceptionAllowed = exceptionAfterLastAttempt;
		exceptionCaptured = false;
		return created;
	}

	private void showPane(LivenessStatusPane created) {
		pane = created;
		paneHolder.getChildren().setAll(created);
	}

	/** Same state in another language: a new pane, given the last update of the session. */
	private void switchLanguage() {
		boolean captured = exceptionCaptured;
		LivenessStatusPane created = newPane(workflowBox.getValue(), exceptionAllowed);
		showPane(created);
		LivenessSession current = session;
		LivenessUpdate last = current == null ? null : current.current();
		if (captured) {
			exceptionCaptured = true;
			created.showExceptionCaptured();
		} else if (last != null) {
			created.show(last);
		}
	}

	private Properties texts(String kind, String language) {
		return texts.computeIfAbsent(kind + "_" + language, name -> {
			Properties properties = new Properties();
			try (InputStream in = DemoApp.class.getResourceAsStream("/" + name + ".properties")) {
				if (in == null) {
					throw new IllegalStateException(name + ".properties is not on the classpath");
				}
				properties.load(in);
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
			return properties;
		});
	}

	private final class Listener implements LivenessEventListener {
		@Override
		public void onUpdate(LivenessUpdate update) {
			Platform.runLater(() -> pane.show(update));
			if (update.state() == SessionState.PASSED && captureRequested.compareAndSet(false, true)) {
				worker.execute(() -> capture(false));
			}
		}

		@Override
		public void onEvent(LivenessEvent event) {
			String line = describe(event);
			Platform.runLater(() -> audit.getItems().add(line));
		}
	}

	/** One line per audit event, with the fields the client writes to its audit trail. */
	static String describe(LivenessEvent event) {
		StringBuilder line = new StringBuilder(LocalTime.now().format(TIME)).append("  ").append(event.type())
				.append("  ").append(event.workflow()).append(", attempt ").append(event.attempt());
		event.challenge().ifPresent(challenge -> line.append(", ").append(challenge));
		event.failureReason().ifPresent(reason -> line.append(", ").append(reason));
		event.deviceEvent().ifPresent(device -> line.append(", device ").append(device));
		return line.append(", ").append(event.durationMs()).append(" ms, models ").append(event.modelSetVersion())
				.toString();
	}

	static String workflowName(Workflow workflow) {
		switch (workflow) {
		case RESIDENT:
			return "Resident face capture";
		case OPERATOR:
			return "Operator sign-in";
		default:
			return "Supervisor authentication";
		}
	}

	private static <T> StringConverter<T> converter(Function<T, String> name) {
		return new StringConverter<T>() {
			@Override
			public String toString(T value) {
				return value == null ? "" : name.apply(value);
			}

			@Override
			public T fromString(String text) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private static long nowMs() {
		return System.nanoTime() / 1_000_000;
	}
}
