package com.gomatch.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
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
 * 裁剪框固定居中，用户拖动图片来调整裁剪区域。
 * 4 个 Rectangle 围成暗色遮罩，固定在裁剪框外侧。
 */
public class CropAvatarController {

    private static final int OUTPUT_SIZE = 640;

    @FXML private StackPane canvasContainer;
    @FXML private Group maskGroup;
    @FXML private Rectangle maskTop, maskBottom, maskLeft, maskRight;
    @FXML private Rectangle cropFrame;
    @FXML private ImageView imageView;

    private Image sourceImage;
    private Consumer<byte[]> onCropComplete;

    private double cropW, cropH;
    private double cw, ch;
    private double imgScale = 1.0;
    private double minImgScale = 1.0;
    private double sourceImgW, sourceImgH;

    private double dragStartImgX, dragStartImgY;
    private double pressSceneX, pressSceneY;

    @FXML
    private void initialize() {
        if (maskGroup != null) {
            StackPane.setAlignment(maskGroup, Pos.TOP_LEFT);
        }
        // 裁剪画布溢出，防止图片遮挡底部按钮
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasContainer.widthProperty());
        clip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(clip);

        canvasContainer.widthProperty().addListener((o, a, b) -> onCanvasResized());
        canvasContainer.heightProperty().addListener((o, a, b) -> onCanvasResized());
        Platform.runLater(() -> {
            cw = canvasContainer.getWidth();
            ch = canvasContainer.getHeight();
            if (cw > 0 && ch > 0) {
                if (sourceImage != null) {
                    initCropFrame();
                }
                redrawMask();
            }
        });
    }

    private void onCanvasResized() {
        cw = canvasContainer.getWidth();
        ch = canvasContainer.getHeight();
        if (cw <= 0 || ch <= 0) return;
        initCropFrame();
        if (sourceImage != null) {
            clampImagePosition();
            redrawMask();
        }
    }

    public void setSourceFile(File file) {
        sourceImage = new Image(file.toURI().toString(), true);
        sourceImage.progressProperty().addListener((obs, old, val) -> {
            if (val.doubleValue() >= 1.0) {
                if (sourceImage.isError()) {
                    System.err.println("[CropAvatar] 图片加载失败: " + sourceImage.getException());
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
        cw = canvasContainer.getWidth() > 0 ? canvasContainer.getWidth() : 540;
        ch = canvasContainer.getHeight() > 0 ? canvasContainer.getHeight() : 540;

        sourceImgW = sourceImage.getWidth();
        sourceImgH = sourceImage.getHeight();

        initCropFrame();
        // 最小缩放：保证裁剪框区域完全被图片覆盖
        minImgScale = Math.max(cropW / sourceImgW, cropH / sourceImgH);
        double fillScale = Math.max(cw / sourceImgW, ch / sourceImgH);
        imgScale = Math.max(fillScale, minImgScale);

        applyScaleToImage();
        centerImage();
        clampImagePosition();
        redrawMask();

        imageView.setCursor(Cursor.MOVE);
        imageView.setOnMousePressed(e -> {
            pressSceneX = e.getSceneX();
            pressSceneY = e.getSceneY();
            dragStartImgX = imageView.getLayoutX();
            dragStartImgY = imageView.getLayoutY();
        });
        imageView.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - pressSceneX;
            double dy = e.getSceneY() - pressSceneY;
            imageView.setLayoutX(dragStartImgX + dx);
            imageView.setLayoutY(dragStartImgY + dy);
            clampImagePosition();
            redrawMask();
        });

        // 滚轮缩放
        canvasContainer.setOnScroll(e -> {
            double oldScale = imgScale;
            double delta = e.getDeltaY() > 0 ? 1.1 : 0.9;
            double newScale = oldScale * delta;
            double maxScale = Math.max(minImgScale, 3.0);
            newScale = Math.max(minImgScale, Math.min(maxScale, newScale));
            if (Math.abs(newScale - oldScale) < 0.001) return;

            // 以裁剪框中心为锚点缩放
            double anchorX = cropFrame.getX() + cropW / 2;
            double anchorY = cropFrame.getY() + cropH / 2;
            double imgX = imageView.getLayoutX();
            double imgY = imageView.getLayoutY();

            double anchorImgX = (anchorX - imgX) / oldScale;
            double anchorImgY = (anchorY - imgY) / oldScale;

            imgScale = newScale;
            applyScaleToImage();

            imageView.setLayoutX(anchorX - anchorImgX * newScale);
            imageView.setLayoutY(anchorY - anchorImgY * newScale);
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
        if (cw <= 0 || ch <= 0) return;
        double side = Math.min(cw, ch) - 40;
        cropW = side;
        cropH = side;
        if (cropFrame != null) {
            cropFrame.setWidth(cropW);
            cropFrame.setHeight(cropH);
            cropFrame.setX((cw - cropW) / 2);
            cropFrame.setY((ch - cropH) / 2);
        }
    }

    private void centerImage() {
        double imgW = sourceImgW * imgScale;
        double imgH = sourceImgH * imgScale;
        imageView.setLayoutX((cw - imgW) / 2);
        imageView.setLayoutY((ch - imgH) / 2);
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

        double x = Math.max(minX, Math.min(maxX, imageView.getLayoutX()));
        double y = Math.max(minY, Math.min(maxY, imageView.getLayoutY()));
        imageView.setLayoutX(x);
        imageView.setLayoutY(y);
    }

    private void redrawMask() {
        if (cropFrame == null) return;
        if (cw <= 0 || ch <= 0) return;
        if (maskTop == null) return;

        double fx = cropFrame.getX();
        double fy = cropFrame.getY();
        double fw = cropFrame.getWidth();
        double fh = cropFrame.getHeight();

        setRect(maskTop, 0, 0, cw, fy);
        setRect(maskBottom, 0, fy + fh, cw, Math.max(0, ch - fy - fh));
        setRect(maskLeft, 0, fy, Math.max(0, fx), fh);
        setRect(maskRight, fx + fw, fy, Math.max(0, cw - fx - fw), fh);
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

        double srcX = (fx - imageView.getLayoutX()) / imgScale;
        double srcY = (fy - imageView.getLayoutY()) / imgScale;
        double srcW = fw / imgScale;
        double srcH = fh / imgScale;

        Canvas canvas = new Canvas(OUTPUT_SIZE, OUTPUT_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.drawImage(sourceImage,
                srcX, srcY, srcW, srcH,
                0, 0, OUTPUT_SIZE, OUTPUT_SIZE);

        WritableImage wi = canvas.snapshot(null, null);
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
