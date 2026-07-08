package com.gomatch.client.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AsciiTextCanvas extends Canvas {
    private String text = "Gomatch";
    private int asciiFontSize = 5; 
    private boolean enableWaves = true;

    // 字符集
    private static final String CHARSET = " .'`^\",:;Il!i~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$";

    private AnimationTimer timer;
    private WritableImage textImage;
    private PixelReader textureReader;
    private int textWidth;
    private int textHeight;

    private double mouseX = 180;
    private double mouseY = 160;
    private double targetMouseX = 180;
    private double targetMouseY = 160;

    // 3D 旋转角度物理角
    private double meshRotX = 0;
    private double meshRotY = 0;

    public AsciiTextCanvas() {
        // 初始化非零尺寸，防止 GPU RTTexture 分配失败
        super(1, 1);
        widthProperty().addListener(evt -> {
            resetMouseCenter();
            drawFrame(System.nanoTime() * 1e-9);
        });
        heightProperty().addListener(evt -> {
            resetMouseCenter();
            drawFrame(System.nanoTime() * 1e-9);
        });

        setOnMouseMoved(e -> {
            targetMouseX = e.getX();
            targetMouseY = e.getY();
        });

        setOnMouseExited(e -> {
            resetMouseCenter();
        });

        initTextImage();
        startAnimation();
    }

    private void resetMouseCenter() {
        double w = getWidth();
        double h = getHeight();
        if (w > 0 && h > 0) {
            targetMouseX = w / 2.0;
            targetMouseY = h / 2.0;
        }
    }

    private void initTextImage() {
        // 180px 巨大高清粗体字作为贴图源
        Font font = Font.font("System", FontWeight.EXTRA_BOLD, 180);
        
        javafx.scene.text.Text textNode = new javafx.scene.text.Text(text);
        textNode.setFont(font);
        double layoutW = textNode.getLayoutBounds().getWidth();
        double layoutH = textNode.getLayoutBounds().getHeight();
        
        if (layoutW <= 0 || layoutH <= 0) {
            layoutW = 400;  // fallback
            layoutH = 180;
        }
        
        int rawW = (int) Math.ceil(layoutW) + 40;
        int rawH = (int) Math.ceil(layoutH) + 40;
        if (rawW <= 0) rawW = 100;
        if (rawH <= 0) rawH = 100;

        Canvas tempCanvas = new Canvas(rawW, rawH);
        GraphicsContext gc = tempCanvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, rawW, rawH);

        gc.setFill(Color.WHITE);
        gc.setFont(font);
        
        double textY = rawH / 2.0 + layoutH / 4.0;
        gc.fillText(text, 20, textY);

        try {
            textImage = tempCanvas.snapshot(null, null);
        } catch (Exception e) {
            System.err.println("[AsciiTextCanvas] snapshot failed: " + e.getMessage());
            return;
        }

        if (textImage == null) return;

        textWidth = (int) textImage.getWidth();
        textHeight = (int) textImage.getHeight();
        if (textWidth <= 0 || textHeight <= 0) return;

        textureReader = textImage.getPixelReader();
    }

    private void startAnimation() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double time = now * 1e-9;
                
                mouseX += (targetMouseX - mouseX) * 0.08;
                mouseY += (targetMouseY - mouseY) * 0.08;

                drawFrame(time);
            }
        };
        timer.start();
    }

    private double sampleTexture(double u, double v, int channel) {
        if (textureReader == null) return 0.0;
        if (u < 0.0 || u > 1.0 || v < 0.0 || v > 1.0) {
            return 0.0;
        }
        int tx = (int) (u * (textWidth - 1));
        int ty = (int) (v * (textHeight - 1));
        if (tx >= 0 && tx < textWidth && ty >= 0 && ty < textHeight) {
            Color c = textureReader.getColor(tx, ty);
            switch (channel) {
                case 0: return c.getRed();
                case 1: return c.getGreen();
                case 2: return c.getBlue();
                default: return c.getOpacity();
            }
        }
        return 0.0;
    }

    private void drawFrame(double time) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double charW = asciiFontSize * 0.55;
        double charH = asciiFontSize;

        int cols = (int) Math.floor(w / charW);
        int rows = (int) Math.floor(h / charH);
        if (cols <= 0 || rows <= 0) return;

        // 3D 旋转角度插值计算 (对应 React 的 x -> mouse.y, y -> mouse.x)
        double targetRotX = ((mouseY / h) - 0.5) * -0.65; 
        double targetRotY = ((mouseX / w) - 0.5) * 0.65;  
        meshRotX += (targetRotX - meshRotX) * 0.08;
        meshRotY += (targetRotY - meshRotY) * 0.08;

        double cosX = Math.cos(meshRotX);
        double sinX = Math.sin(meshRotX);
        double cosY = Math.cos(meshRotY);
        double sinY = Math.sin(meshRotY);

        // 临时网格用于融合采样数据
        double[][] gridR = new double[cols][rows];
        double[][] gridG = new double[cols][rows];
        double[][] gridB = new double[cols][rows];
        double[][] gridA = new double[cols][rows];

        double waveFactor = enableWaves ? 1.0 : 0.0;

        double textAspect = (double) textWidth / textHeight;

        // 3D 视锥透视自适应
        double cameraZ = 30.0; 
        double fovRad = Math.toRadians(45.0);
        double fovFactor = Math.tan(fovRad / 2.0);
        double aspect = w / h;

        double viewH = 2.0 * cameraZ * fovFactor; 
        double viewW = viewH * aspect;

        double planeW = viewW * 0.92;
        double planeH = planeW / textAspect;

        if (planeH > viewH * 0.92) {
            planeH = viewH * 0.92;
            planeW = planeH * textAspect;
        }

        // 采样网格点
        int numU = 480;
        int numV = 120;

        // 第一步：遍历并计算 3D 透视坐标，绘制深色粒子阴影字底，并收集 ASCII 各通道强度
        for (int iv = 0; iv < numV; iv++) {
            double v = (double) iv / (numV - 1);
            for (int iu = 0; iu < numU; iu++) {
                double u = (double) iu / (numU - 1);

                double x_local = (u - 0.5) * planeW;
                double y_local = (0.5 - v) * planeH;

                // Vertex Shader 几何顶点变形 (大幅减小系数以减少扭动晃动，让标志显得厚重坚实)
                double timeSpeed = time * 2.2; // 降慢扭动角速度
                double tx = x_local + Math.sin(timeSpeed + y_local) * 0.15 * waveFactor;
                double ty = y_local + Math.cos(timeSpeed) * 0.04 * waveFactor;
                double tz = Math.sin(timeSpeed + x_local) * 0.25 * waveFactor;

                // 3D Rotation
                double x1 = tx * cosY - tz * sinY;
                double z1 = tx * sinY + tz * cosY;
                double y1 = ty;

                double x2 = x1;
                double y2 = y1 * cosX - z1 * sinX;
                double z2 = y1 * sinX + z1 * cosX;

                // Perspective Projection
                double z_cam = cameraZ - z2;
                double ndcX = x2 / (z_cam * fovFactor);
                double ndcY = y2 / (z_cam * fovFactor);

                double screenX = (ndcX / aspect + 1.0) * (w / 2.0);
                double screenY = (-ndcY + 1.0) * (h / 2.0);

                int col = (int) Math.floor(screenX / charW);
                int row = (int) Math.floor(screenY / charH);

                if (col >= 0 && col < cols && row >= 0 && row < rows) {
                    // Fragment Shader: Chromatic Aberration 模拟（将偏移收缩为 0.012 以形成极度精致纤细的边缘偏色带）
                    double u_r = u + Math.cos(time * 2.5 + u) * 0.012;
                    double v_r = v + Math.cos(time * 2.5 + v) * 0.012;

                    double u_g = u + Math.sin(time * 1.5 + u) * 0.012;
                    double v_g = v + Math.sin(time * 1.5 + v) * 0.012;

                    double u_b = u - Math.cos(time * 2.5 + v) * 0.012;
                    double v_b = v - Math.cos(time * 2.5 + u) * 0.012;

                    double r = sampleTexture(u_r, v_r, 0);
                    double g = sampleTexture(u_g, v_g, 1);
                    double b = sampleTexture(u_b, v_b, 2);
                    double a = sampleTexture(u, v, 3);

                    if (a > 0.05) {
                        if (a > gridA[col][row]) {
                            gridR[col][row] = r;
                            gridG[col][row] = g;
                            gridB[col][row] = b;
                            gridA[col][row] = a;
                        }
                    }
                }
            }
        }

        // 第二步：绘制上层高饱和度偏色色差 ASCII 字符
        gc.setFont(Font.font("System", FontWeight.BLACK, asciiFontSize));

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double a = gridA[col][row];
                if (a > 0.05) {
                    double r = gridR[col][row];
                    double g = gridG[col][row];
                    double b = gridB[col][row];

                    // 计算亮度并映射字符
                    double gray = 0.3 * r + 0.59 * g + 0.11 * b;
                    int charIdx = (int) (gray * (CHARSET.length() - 1));
                    if (charIdx < 0) charIdx = 0;
                    if (charIdx >= CHARSET.length()) charIdx = CHARSET.length() - 1;

                    char c = CHARSET.charAt(charIdx);

                    int rClr = (int) (r * 255);
                    int gClr = (int) (g * 255);
                    int bClr = (int) (b * 255);

                    // 文字主色调判定：门槛下放至 45（极暗临界），彻底拉满纯黑字身的所占比例
                    if (rClr > 45 && gClr > 45 && bClr > 45) {
                        // 文字核心：极简深黑色主色调
                        gc.setFill(Color.web("#09090b"));
                    } else {
                        // 边缘偏色：取消高亮拉满，对红色和蓝色色散通道实施大幅衰减，融入黑色色系
                        // 这会使左侧红色边缘转为深红黑，右侧蓝色边缘转为深蓝黑，高冷内敛
                        int darkR = (int) (rClr * 0.22);
                        int darkG = (int) (gClr * 0.35); // 绿色/黄色稍微保留一点微弱的灰调
                        int darkB = (int) (bClr * 0.20);
                        gc.setFill(Color.rgb(darkR, darkG, darkB, a));
                    }

                    double dx = col * charW;
                    double dy = row * charH + charH * 0.85;

                    gc.fillText(String.valueOf(c), dx, dy);
                }
            }
        }
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
