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
 * 图片固定铺满画布，用户拖动白色裁剪框选择 640×640 区域。
 * 4 个 Rectangle 围成暗色遮罩，跟随裁剪框位置。
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

    private double dragStartFrameX, dragStartFrameY;
    private double pressSceneX, pressSceneY;

    @FXML
    private void initialize() {
        // 把 maskGroup 顶到左上角，让 Rectangle 坐标相对 StackPane
        if (maskGroup != null) {
            StackPane.setAlignment(maskGroup, Pos.TOP_LEFT);
        }
        canvasContainer.widthProperty().addListener((o, a, b) -> onCanvasResized());
        canvasContainer.heightProperty().addListener((o, a, b) -> onCanvasResized());
        // 首次布局后强制画一次
        Platform.runLater(() -> {
            cw = canvasContainer.getWidth();
            ch = canvasContainer.getHeight();
            if (cw > 0 && ch > 0) {
                if (sourceImage != null) {
                    initCropFrameSize();
                    cropFrame.setX((cw - cropW) / 2);
                    cropFrame.setY((ch - cropH) / 2);
                }
                redrawMask();
            }
        });
    }

    private void onCanvasResized() {
        cw = canvasContainer.getWidth();
        ch = canvasContainer.getHeight();
        if (cw <= 0 || ch <= 0) return;
        initCropFrameSize();
        if (sourceImage != null) {
            clampFramePosition();
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

        // 图片铺满画布
        double imgW = sourceImage.getWidth();
        double imgH = sourceImage.getHeight();
        double sx = cw / imgW;
        double sy = ch / imgH;
        double scale = Math.max(sx, sy);
        imageView.setImage(sourceImage);
        imageView.setFitWidth(imgW * scale);
        imageView.setFitHeight(imgH * scale);
        imageView.setPreserveRatio(false);
        imageView.setLayoutX((cw - imgW * scale) / 2);
        imageView.setLayoutY((ch - imgH * scale) / 2);

        // 初始化裁剪框
        initCropFrameSize();
        cropFrame.setX((cw - cropW) / 2);
        cropFrame.setY((ch - cropH) / 2);

        // 裁剪框可拖动
        cropFrame.setCursor(Cursor.MOVE);
        cropFrame.setOnMousePressed(e -> {
            pressSceneX = e.getSceneX();
            pressSceneY = e.getSceneY();
            dragStartFrameX = cropFrame.getX();
            dragStartFrameY = cropFrame.getY();
        });
        cropFrame.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - pressSceneX;
            double dy = e.getSceneY() - pressSceneY;
            double newX = dragStartFrameX + dx;
            double newY = dragStartFrameY + dy;
            newX = Math.max(0, Math.min(cw - cropW, newX));
            newY = Math.max(0, Math.min(ch - cropH, newY));
            cropFrame.setX(newX);
            cropFrame.setY(newY);
            redrawMask();
        });

        redrawMask();
    }

    private void initCropFrameSize() {
        if (cw <= 0 || ch <= 0) return;
        double side = Math.min(cw, ch) - 40;
        cropW = side;
        cropH = side;
        if (cropFrame != null) {
            cropFrame.setWidth(cropW);
            cropFrame.setHeight(cropH);
        }
    }

    private void clampFramePosition() {
        if (cropFrame == null) return;
        double x = Math.max(0, Math.min(cw - cropW, cropFrame.getX()));
        double y = Math.max(0, Math.min(ch - cropH, cropFrame.getY()));
        cropFrame.setX(x);
        cropFrame.setY(y);
    }

    /** 调整 4 个遮罩 Rectangle 的大小和位置（用 Shape.setX/setY/setWidth/setHeight） */
    private void redrawMask() {
        if (cropFrame == null) return;
        if (cw <= 0 || ch <= 0) return;
        if (maskTop == null) return;

        double fx = cropFrame.getX();
        double fy = cropFrame.getY();
        double fw = cropFrame.getWidth();
        double fh = cropFrame.getHeight();

        // 上
        setRect(maskTop, 0, 0, cw, fy);
        // 底
        setRect(maskBottom, 0, fy + fh, cw, Math.max(0, ch - fy - fh));
        // 左
        setRect(maskLeft, 0, fy, Math.max(0, fx), fh);
        // 右
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

        double imgW = sourceImage.getWidth();
        double imgH = sourceImage.getHeight();
        double sx = cw / imgW;
        double sy = ch / imgH;
        double scale = Math.max(sx, sy);
        double dispW = imgW * scale;
        double dispH = imgH * scale;
        double imgOffX = (cw - dispW) / 2;
        double imgOffY = (ch - dispH) / 2;

        double srcX = (fx - imgOffX) / scale;
        double srcY = (fy - imgOffY) / scale;
        double srcW = fw / scale;
        double srcH = fh / scale;

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
