package com.gomatch.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 头像裁剪控制器。
 * <p>
 * StackPane 固定 540×540 正方形，Clip 裁剪溢出。
 * ImageView 使用 translateX/Y 定位（不受 StackPane 布局影响），
 * 用户拖拽/滚轮缩放都操作 translate 值。
 * 遮罩 Group 固定在最上层（mouseTransparent），按钮区域与画布完全分离。
 */
public class CropAvatarController {

    private static final int CANVAS_SIZE = 540;
    private static final int OUTPUT_SIZE = 640;
    private static final double CROP_MARGIN = 40;
    private static final double MAX_DISPLAY_DIM = 4096;

    @FXML private StackPane canvasContainer;
    @FXML private Group maskGroup;
    @FXML private Rectangle maskTop, maskBottom, maskLeft, maskRight;
    @FXML private Rectangle cropFrame;
    @FXML private ImageView imageView;

    private Image sourceImage;
    private Consumer<byte[]> onCropComplete;
    private Runnable onLoadError;

    private double cropW, cropH;
    private double imgScale = 1.0;
    private double minImgScale = 1.0;
    private double sourceImgW, sourceImgH;

    private double dragStartTx, dragStartTy;
    private double pressSceneX, pressSceneY;

    private double imgTx() { return imageView.getTranslateX(); }
    private double imgTy() { return imageView.getTranslateY(); }
    private void setImgTx(double v) { imageView.setTranslateX(v); }
    private void setImgTy(double v) { imageView.setTranslateY(v); }

    @FXML
    private void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasContainer.widthProperty());
        clip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(clip);

        canvasContainer.widthProperty().addListener((o, a, b) -> onCanvasResized());
        canvasContainer.heightProperty().addListener((o, a, b) -> onCanvasResized());

        canvasContainer.setOnMousePressed(e -> {
            if (sourceImage == null) return;
            pressSceneX = e.getSceneX();
            pressSceneY = e.getSceneY();
            dragStartTx = imgTx();
            dragStartTy = imgTy();
        });
        canvasContainer.setOnMouseDragged(e -> {
            if (sourceImage == null) return;
            double dx = e.getSceneX() - pressSceneX;
            double dy = e.getSceneY() - pressSceneY;
            setImgTx(dragStartTx + dx);
            setImgTy(dragStartTy + dy);
            clampImagePosition();
            redrawMask();
        });

        Platform.runLater(() -> {
            if (canvasContainer.getWidth() > 0 && canvasContainer.getHeight() > 0) {
                initCropFrame();
                redrawMask();
            }
        });
    }

    private void onCanvasResized() {
        if (canvasContainer.getWidth() <= 0 || canvasContainer.getHeight() <= 0) return;
        initCropFrame();
        if (sourceImage != null) {
            clampImagePosition();
            redrawMask();
        }
    }

    public void setSourceFile(File file) {
        try {
            sourceImage = new Image(file.toURI().toString(), true);
        } catch (Exception e) {
            System.err.println("[CropAvatar] 图片打开失败: " + e.getMessage());
            if (onLoadError != null) Platform.runLater(onLoadError);
            Platform.runLater(this::closeDialog);
            return;
        }
        sourceImage.progressProperty().addListener((obs, old, val) -> {
            if (val.doubleValue() >= 1.0) {
                if (sourceImage.isError()) {
                    System.err.println("[CropAvatar] 图片加载失败: " + sourceImage.getException());
                    if (onLoadError != null) Platform.runLater(onLoadError);
                    Platform.runLater(this::closeDialog);
                    return;
                }
                Platform.runLater(this::initImage);
            }
        });
        if (sourceImage.getProgress() >= 1.0 && !sourceImage.isError()) {
            initImage();
        }
    }

    private void initImage() {
        if (sourceImage == null) return;

        sourceImgW = sourceImage.getWidth();
        sourceImgH = sourceImage.getHeight();
        initCropFrame();

        minImgScale = Math.max(cropW / sourceImgW, cropH / sourceImgH);
        double fillScale = Math.max(CANVAS_SIZE / sourceImgW, CANVAS_SIZE / sourceImgH);
        imgScale = Math.max(fillScale, minImgScale);

        // 防止超大尺寸图片超出 GPU 纹理上限导致黑屏
        double maxSafeScale = Math.min(MAX_DISPLAY_DIM / sourceImgW, MAX_DISPLAY_DIM / sourceImgH);
        if (imgScale > maxSafeScale) {
            imgScale = Math.max(maxSafeScale, minImgScale);
        }

        applyScaleToImage();
        centerImage();
        clampImagePosition();
        redrawMask();

        imageView.setCursor(Cursor.MOVE);

        canvasContainer.setOnScroll(e -> {
            double oldScale = imgScale;
            double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
            double newScale = oldScale * delta;
            double maxScale = Math.max(minImgScale, 3.0);
            double safeLimit = Math.min(MAX_DISPLAY_DIM / sourceImgW, MAX_DISPLAY_DIM / sourceImgH);
            newScale = Math.max(minImgScale, Math.min(Math.min(maxScale, safeLimit), newScale));
            if (Math.abs(newScale - oldScale) < 0.001) return;

            double anchorX = cropFrame.getX() + cropW / 2;
            double anchorY = cropFrame.getY() + cropH / 2;
            double imgX = imgTx();
            double imgY = imgTy();

            double anchorImgX = (anchorX - imgX) / oldScale;
            double anchorImgY = (anchorY - imgY) / oldScale;

            imgScale = newScale;
            applyScaleToImage();

            setImgTx(anchorX - anchorImgX * newScale);
            setImgTy(anchorY - anchorImgY * newScale);
            clampImagePosition();
            redrawMask();
        });
    }

    private void applyScaleToImage() {
        imageView.setImage(sourceImage);
        imageView.setFitWidth(sourceImgW * imgScale);
        imageView.setFitHeight(sourceImgH * imgScale);
        imageView.setPreserveRatio(false);
    }

    private void initCropFrame() {
        double side = CANVAS_SIZE - CROP_MARGIN * 2;
        cropW = side;
        cropH = side;
        if (cropFrame != null) {
            cropFrame.setWidth(cropW);
            cropFrame.setHeight(cropH);
            cropFrame.setX(CROP_MARGIN);
            cropFrame.setY(CROP_MARGIN);
        }
    }

    private void centerImage() {
        double imgW = sourceImgW * imgScale;
        double imgH = sourceImgH * imgScale;
        setImgTx((CANVAS_SIZE - imgW) / 2);
        setImgTy((CANVAS_SIZE - imgH) / 2);
    }

    private void clampImagePosition() {
        double imgW = sourceImgW * imgScale;
        double imgH = sourceImgH * imgScale;
        double fx = cropFrame.getX();
        double fy = cropFrame.getY();
        double fw = cropFrame.getWidth();
        double fh = cropFrame.getHeight();

        double minX = fx + fw - imgW;
        double maxX = fx;
        double minY = fy + fh - imgH;
        double maxY = fy;

        double x = Math.max(minX, Math.min(maxX, imgTx()));
        double y = Math.max(minY, Math.min(maxY, imgTy()));
        setImgTx(x);
        setImgTy(y);
    }

    private void redrawMask() {
        if (cropFrame == null || maskTop == null) return;

        double fx = cropFrame.getX();
        double fy = cropFrame.getY();
        double fw = cropFrame.getWidth();
        double fh = cropFrame.getHeight();
        double sz = CANVAS_SIZE;

        setRect(maskTop, 0, 0, sz, fy);
        setRect(maskBottom, 0, fy + fh, sz, Math.max(0, sz - fy - fh));
        setRect(maskLeft, 0, fy, Math.max(0, fx), fh);
        setRect(maskRight, fx + fw, fy, Math.max(0, sz - fx - fw), fh);
    }

    private void setRect(Rectangle r, double x, double y, double w, double h) {
        if (r == null) return;
        r.setX(x);
        r.setY(y);
        r.setWidth(w);
        r.setHeight(h);
    }

    @FXML
    private void handleConfirm() {
        if (sourceImage == null || cropFrame == null) {
            closeDialog();
            return;
        }

        double fx = cropFrame.getX();
        double fy = cropFrame.getY();
        double fw = cropFrame.getWidth();
        double fh = cropFrame.getHeight();

        double srcX = (fx - imgTx()) / imgScale;
        double srcY = (fy - imgTy()) / imgScale;
        double srcW = fw / imgScale;
        double srcH = fh / imgScale;

        Canvas canvas = new Canvas(OUTPUT_SIZE, OUTPUT_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.drawImage(sourceImage, srcX, srcY, srcW, srcH, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE);

        Group tempRoot = new Group(canvas);
        new Scene(tempRoot);

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage wi = new WritableImage(OUTPUT_SIZE, OUTPUT_SIZE);
        canvas.snapshot(sp, wi);

        byte[] bytes = encodeToPng(wi);

        if (bytes != null && onCropComplete != null) {
            onCropComplete.accept(bytes);
        }
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    public void setOnCropComplete(Consumer<byte[]> callback) {
        this.onCropComplete = callback;
    }

    public void setOnLoadError(Runnable callback) {
        this.onLoadError = callback;
    }

    private byte[] encodeToPng(WritableImage wi) {
        int w = (int) wi.getWidth();
        int h = (int) wi.getHeight();
        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = wi.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bImg.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bImg, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("[CropAvatar] PNG 编码失败: " + e.getMessage());
            return null;
        }
    }

    private void closeDialog() {
        if (canvasContainer.getScene() != null && canvasContainer.getScene().getWindow() != null) {
            ((Stage) canvasContainer.getScene().getWindow()).close();
        }
    }
}
