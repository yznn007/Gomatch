package com.gomatch.client.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * JavaFX version of the React Bits ElasticSlider, tuned for the settings volume row.
 */
public class ElasticVolumeSlider extends VBox {
    private static final double MAX_OVERFLOW = 50.0;
    private static final double REST_TRACK_HEIGHT = 6.0;
    private static final double ACTIVE_TRACK_HEIGHT = 12.0;

    private final DoubleProperty startingValue = new SimpleDoubleProperty(this, "startingValue", 0);
    private final DoubleProperty maxValue = new SimpleDoubleProperty(this, "maxValue", 100);
    private final DoubleProperty value = new SimpleDoubleProperty(this, "value", 50);
    private final DoubleProperty stepSize = new SimpleDoubleProperty(this, "stepSize", 1);
    private final BooleanProperty stepped = new SimpleBooleanProperty(this, "stepped", false);

    private final Label valueLabel = new Label();
    private final Label leftIcon = new Label("−");
    private final Label rightIcon = new Label("+");
    private final StackPane sliderRoot = new StackPane();
    private final StackPane trackWrapper = new StackPane();
    private final Region track = new Region();
    private final Region range = new Region();

    private String region = "middle";
    private double overflow;
    private boolean dragging;
    private Timeline currentAnimation;

    public ElasticVolumeSlider() {
        getStyleClass().add("elastic-slider");
        setAlignment(Pos.CENTER);
        setSpacing(2);
        setMinWidth(220);
        setPrefWidth(240);
        setMaxWidth(260);

        valueLabel.getStyleClass().add("elastic-slider-value");
        valueLabel.setMouseTransparent(true);

        leftIcon.getStyleClass().add("elastic-slider-icon");
        rightIcon.getStyleClass().add("elastic-slider-icon");

        HBox wrapper = new HBox(14);
        wrapper.getStyleClass().add("elastic-slider-wrapper");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setOpacity(0.72);
        wrapper.setMinHeight(34);
        wrapper.setCursor(Cursor.HAND);

        sliderRoot.getStyleClass().add("elastic-slider-root");
        sliderRoot.setMinHeight(34);
        sliderRoot.setPrefHeight(34);
        HBox.setHgrow(sliderRoot, Priority.ALWAYS);

        trackWrapper.getStyleClass().add("elastic-slider-track-wrapper");
        trackWrapper.setMinHeight(REST_TRACK_HEIGHT);
        trackWrapper.setPrefHeight(REST_TRACK_HEIGHT);
        trackWrapper.setMaxHeight(REST_TRACK_HEIGHT);
        trackWrapper.setMaxWidth(Double.MAX_VALUE);

        track.getStyleClass().add("elastic-slider-track");
        range.getStyleClass().add("elastic-slider-range");
        track.setMaxWidth(Double.MAX_VALUE);
        range.setMaxWidth(0);

        trackWrapper.getChildren().addAll(track, range);
        StackPane.setAlignment(range, Pos.CENTER_LEFT);
        sliderRoot.getChildren().add(trackWrapper);
        wrapper.getChildren().addAll(leftIcon, sliderRoot, rightIcon);
        getChildren().addAll(valueLabel, wrapper);

        value.addListener((obs, oldVal, newVal) -> updateValueDisplay());
        startingValue.addListener((obs, oldVal, newVal) -> updateValueDisplay());
        maxValue.addListener((obs, oldVal, newVal) -> updateValueDisplay());
        widthProperty().addListener((obs, oldVal, newVal) -> updateValueDisplay());
        sliderRoot.widthProperty().addListener((obs, oldVal, newVal) -> updateValueDisplay());

        wrapper.setOnMouseEntered(e -> animateHover(true));
        wrapper.setOnMouseExited(e -> {
            if (!dragging) {
                animateHover(false);
            }
        });
        wrapper.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handlePointerDown);
        wrapper.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handlePointerMove);
        wrapper.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> handlePointerUp(wrapper));
        wrapper.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (dragging && e.isPrimaryButtonDown()) {
                handlePointerMove(e);
            }
        });

        setLeftIconText("−");
        setRightIconText("+");
        updateValueDisplay();
    }

    private void handlePointerDown(MouseEvent e) {
        dragging = true;
        animateHover(true);
        handlePointerMove(e);
        e.consume();
    }

    private void handlePointerMove(MouseEvent e) {
        double width = sliderRoot.getWidth();
        if (width <= 0) {
            return;
        }

        double localX = sliderRoot.sceneToLocal(e.getSceneX(), e.getSceneY()).getX();
        double newValue = getStartingValue() + (localX / width) * (getMaxValue() - getStartingValue());
        if (isStepped() && getStepSize() > 0) {
            newValue = Math.round(newValue / getStepSize()) * getStepSize();
        }
        setValue(clamp(newValue, getStartingValue(), getMaxValue()));

        if (localX < 0) {
            applyOverflow("left", decay(-localX, MAX_OVERFLOW));
        } else if (localX > width) {
            applyOverflow("right", decay(localX - width, MAX_OVERFLOW));
        } else {
            applyOverflow("middle", 0);
        }
        e.consume();
    }

    private void handlePointerUp(HBox wrapper) {
        dragging = false;
        animateOverflowToRest();
        if (!wrapper.isHover()) {
            animateHover(false);
        }
    }

    private void updateValueDisplay() {
        double clamped = clamp(getValue(), getStartingValue(), getMaxValue());
        if (Math.abs(clamped - getValue()) > 0.001) {
            setValue(clamped);
            return;
        }

        valueLabel.setText(String.valueOf(Math.round(clamped)));

        double rootWidth = sliderRoot.getWidth();
        if (rootWidth > 0) {
            double percent = getRangePercentage() / 100.0;
            range.setMinWidth(rootWidth * percent);
            range.setPrefWidth(rootWidth * percent);
            range.setMaxWidth(rootWidth * percent);
        }
    }

    private double getRangePercentage() {
        double totalRange = getMaxValue() - getStartingValue();
        if (totalRange == 0) {
            return 0;
        }
        return ((getValue() - getStartingValue()) / totalRange) * 100.0;
    }

    private void animateHover(boolean active) {
        HBox wrapper = (HBox) getChildren().get(1);
        playTimeline(
                new KeyValue(wrapper.scaleXProperty(), active ? 1.12 : 1.0, Interpolator.EASE_OUT),
                new KeyValue(wrapper.scaleYProperty(), active ? 1.12 : 1.0, Interpolator.EASE_OUT),
                new KeyValue(wrapper.opacityProperty(), active ? 1.0 : 0.72, Interpolator.EASE_OUT),
                new KeyValue(trackWrapper.minHeightProperty(), active ? ACTIVE_TRACK_HEIGHT : REST_TRACK_HEIGHT, Interpolator.EASE_OUT),
                new KeyValue(trackWrapper.prefHeightProperty(), active ? ACTIVE_TRACK_HEIGHT : REST_TRACK_HEIGHT, Interpolator.EASE_OUT),
                new KeyValue(trackWrapper.maxHeightProperty(), active ? ACTIVE_TRACK_HEIGHT : REST_TRACK_HEIGHT, Interpolator.EASE_OUT)
        );
    }

    private void applyOverflow(String newRegion, double newOverflow) {
        if (!region.equals(newRegion) && !"middle".equals(newRegion)) {
            pulseIcon("left".equals(newRegion) ? leftIcon : rightIcon);
        }
        region = newRegion;
        overflow = newOverflow;

        double width = Math.max(1, sliderRoot.getWidth());
        double direction = "left".equals(region) ? -1 : ("right".equals(region) ? 1 : 0);
        trackWrapper.setScaleX(1 + overflow / width);
        trackWrapper.setScaleY(1 - (overflow / MAX_OVERFLOW) * 0.2);
        trackWrapper.setTranslateX(direction * overflow * 0.5);
        leftIcon.setTranslateX("left".equals(region) ? -overflow : 0);
        rightIcon.setTranslateX("right".equals(region) ? overflow : 0);
    }

    private void animateOverflowToRest() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(260),
                        new KeyValue(trackWrapper.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(trackWrapper.scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(trackWrapper.translateXProperty(), 0.0, Interpolator.EASE_OUT),
                        new KeyValue(leftIcon.translateXProperty(), 0.0, Interpolator.EASE_OUT),
                        new KeyValue(rightIcon.translateXProperty(), 0.0, Interpolator.EASE_OUT)
                )
        );
        timeline.setOnFinished(e -> {
            overflow = 0;
            region = "middle";
        });
        timeline.play();
    }

    private void pulseIcon(Label icon) {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(icon.scaleXProperty(), 1.0),
                        new KeyValue(icon.scaleYProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(100),
                        new KeyValue(icon.scaleXProperty(), 1.35, Interpolator.EASE_OUT),
                        new KeyValue(icon.scaleYProperty(), 1.35, Interpolator.EASE_OUT)
                ),
                new KeyFrame(Duration.millis(250),
                        new KeyValue(icon.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(icon.scaleYProperty(), 1.0, Interpolator.EASE_OUT)
                )
        );
        pulse.play();
    }

    private void playTimeline(KeyValue... values) {
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        currentAnimation = new Timeline(new KeyFrame(Duration.millis(180), values));
        currentAnimation.play();
    }

    private double decay(double rawValue, double max) {
        if (max == 0) {
            return 0;
        }
        double entry = rawValue / max;
        double sigmoid = 2 * (1 / (1 + Math.exp(-entry)) - 0.5);
        return sigmoid * max;
    }

    private double clamp(double rawValue, double min, double max) {
        return Math.min(Math.max(rawValue, min), max);
    }

    public double getStartingValue() {
        return startingValue.get();
    }

    public void setStartingValue(double value) {
        startingValue.set(value);
    }

    public DoubleProperty startingValueProperty() {
        return startingValue;
    }

    public double getMaxValue() {
        return maxValue.get();
    }

    public void setMaxValue(double value) {
        maxValue.set(value);
    }

    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public double getValue() {
        return value.get();
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public double getStepSize() {
        return stepSize.get();
    }

    public void setStepSize(double value) {
        stepSize.set(value);
    }

    public DoubleProperty stepSizeProperty() {
        return stepSize;
    }

    public boolean isStepped() {
        return stepped.get();
    }

    public void setStepped(boolean value) {
        stepped.set(value);
    }

    public BooleanProperty steppedProperty() {
        return stepped;
    }

    public String getLeftIconText() {
        return leftIcon.getText();
    }

    public void setLeftIconText(String value) {
        leftIcon.setText(value);
    }

    public String getRightIconText() {
        return rightIcon.getText();
    }

    public void setRightIconText(String value) {
        rightIcon.setText(value);
    }
}
