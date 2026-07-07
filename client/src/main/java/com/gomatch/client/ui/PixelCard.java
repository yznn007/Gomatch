package com.gomatch.client.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PixelCard extends StackPane {
    private final Canvas canvas = new Canvas();
    private final List<Pixel> pixels = new ArrayList<>();
    private final AnimationTimer timer;
    private boolean isAnimating = false;
    private String animationMode = "disappear";
    private long lastUpdate = 0;

    // 3D 旋转与缩放变换相关变量 (TiltedCard 特性)
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private double targetRotateX = 0;
    private double targetRotateY = 0;
    private double targetScale = 1.0;
    private double currentRotateX = 0;
    private double currentRotateY = 0;
    private double currentScale = 1.0;

    // 2D 物理单摆运动变量 (Lanyard 挂绳特性)
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);
    private double angle = 0;              // 单摆倾斜弧度 (Z轴)
    private double angularVelocity = 0;     // 角速度
    private double currentVelocity = 0;     // 绳子弯曲的平滑滑动角速度量
    private double lastMouseX = -1;
    private long lastTime = 0;
    private long lastExitTime = 0;

    // 吊绳相关常数
    private final double stringLength = 45.0; // 挂绳长度 (45px 预留区)
    private final Region cardBody = new Region(); // 隔离绘制的卡片本体容器

    // FXML 属性定义
    private final StringProperty variant = new SimpleStringProperty(this, "variant", "default");
    private final ObjectProperty<Integer> gap = new SimpleObjectProperty<>(this, "gap", null);
    private final ObjectProperty<Integer> speed = new SimpleObjectProperty<>(this, "speed", null);
    private final StringProperty colors = new SimpleStringProperty(this, "colors", null);
    private final ObjectProperty<Boolean> noFocus = new SimpleObjectProperty<>(this, "noFocus", null);
    private final BooleanProperty showParticles = new SimpleBooleanProperty(this, "showParticles", true);

    // 变体默认配置结构
    private static class VariantConfig {
        final Color activeColor;
        final int gap;
        final int speed;
        final String colors;
        final boolean noFocus;

        VariantConfig(Color activeColor, int gap, int speed, String colors, boolean noFocus) {
            this.activeColor = activeColor;
            this.gap = gap;
            this.speed = speed;
            this.colors = colors;
            this.noFocus = noFocus;
        }
    }

    private static final VariantConfig DEFAULT_CONFIG = new VariantConfig(null, 5, 35, "#f8fafc,#f1f5f9,#cbd5e1", false);
    private static final VariantConfig BLUE_CONFIG = new VariantConfig(Color.web("#e0f2fe"), 7, 28, "#e0f2fe,#7dd3fc,#0ea5e9,#ffffff,#ffffff,#e0f2fe,#7dd3fc,#e0f2fe,#7dd3fc,#818cf8,#a78bfa,#ffffff", false);
    private static final VariantConfig YELLOW_CONFIG = new VariantConfig(Color.web("#fef08a"), 8, 35, "#fef08a,#fde047,#eab308,#fef08a,#fde047,#fef08a,#fde047,#ffffff,#fed7aa,#fdba74,#ffedd5,#ffffff", false);
    private static final VariantConfig PINK_CONFIG = new VariantConfig(Color.web("#fecdd3"), 6, 80, "#fecdd3,#fda4af,#e11d48,#fecdd3,#fda4af,#fecdd3,#fda4af,#ffffff,#c084fc,#f472b6,#e9d5ff,#ffffff", true);

    private VariantConfig getVariantConfig(String v) {
        if ("blue".equalsIgnoreCase(v)) return BLUE_CONFIG;
        if ("yellow".equalsIgnoreCase(v)) return YELLOW_CONFIG;
        if ("pink".equalsIgnoreCase(v)) return PINK_CONFIG;
        return DEFAULT_CONFIG;
    }

    // JavaFX 属性的 Getters/Setters，支持 FXML 配置
    public String getVariant() { return variant.get(); }
    public void setVariant(String value) { variant.set(value); }
    public StringProperty variantProperty() { return variant; }

    public int getGap() { return gap.get() == null ? -1 : gap.get(); }
    public void setGap(int value) { gap.set(value); }
    public ObjectProperty<Integer> gapProperty() { return gap; }

    public int getSpeed() { return speed.get() == null ? -1 : speed.get(); }
    public void setSpeed(int value) { speed.set(value); }
    public ObjectProperty<Integer> speedProperty() { return speed; }

    public String getColors() { return colors.get(); }
    public void setColors(String value) { colors.set(value); }
    public StringProperty colorsProperty() { return colors; }

    public boolean isNoFocus() { return noFocus.get() == null ? false : noFocus.get(); }
    public void setNoFocus(boolean value) { noFocus.set(value); }
    public ObjectProperty<Boolean> noFocusProperty() { return noFocus; }

    public boolean isShowParticles() { return showParticles.get(); }
    public void setShowParticles(boolean value) { showParticles.set(value); }
    public BooleanProperty showParticlesProperty() { return showParticles; }

    // 获取融合了变体与单独属性配置后的最终生效值
    private int getFinalGap() {
        VariantConfig cfg = getVariantConfig(getVariant());
        return gap.get() != null ? gap.get() : cfg.gap;
    }

    private int getFinalSpeed() {
        VariantConfig cfg = getVariantConfig(getVariant());
        return speed.get() != null ? speed.get() : cfg.speed;
    }

    private String getFinalColors() {
        VariantConfig cfg = getVariantConfig(getVariant());
        return colors.get() != null ? colors.get() : cfg.colors;
    }

    private boolean getFinalNoFocus() {
        VariantConfig cfg = getVariantConfig(getVariant());
        return noFocus.get() != null ? noFocus.get() : cfg.noFocus;
    }

    public PixelCard() {
        // 将画布放置为第一个子元素（作为背景层）
        canvas.setManaged(false);
        this.getChildren().add(0, canvas);

        // 隔离绘制的卡片本体背景 Region，单独赋予样式，使其不覆盖挂绳区
        cardBody.getStyleClass().add("pixel-card-body");
        cardBody.setManaged(false);
        this.getChildren().add(1, cardBody);

        // 添加样式类
        this.getStyleClass().add("pixel-card");

        // 注册 2D 悬挂摆动 (Z轴) 和 3D 偏转 (X/Y轴) 变换组
        // 先应用 Z 轴摆动，再在其局部坐标系下应用 X 和 Y 轴扭转
        this.getTransforms().addAll(rotateZ, rotateX, rotateY);

        // 注册悬停与鼠标移动事件 (TiltedCard + Lanyard 特性)
        this.setOnMouseEntered(e -> {
            // 防抖冷却时间限制：当卡片刚刚滑出 300ms 内，如果由于吊绳惯性回摆重新触及鼠标，直接拦截不予进入 hover 状态
            if (System.currentTimeMillis() - lastExitTime < 300) {
                return;
            }

            this.setViewOrder(-1.0); // 悬停时层级置顶，防止边缘被相邻卡片遮挡
            targetScale = 1.05;      // 悬停轻微放大 1.05 倍
            
            // 鼠标滑入时施加一个初速度的微弱摆动力学推力 (由 1.5 削减为 0.25)
            double push = (new Random().nextDouble() - 0.5) * 0.25;
            angularVelocity += push;
            
            lastMouseX = e.getScreenX();
            lastTime = System.nanoTime();
            
            handleAnimation("appear");
        });

        this.setOnMouseMoved(e -> {
            // 在处于滑出冷却期时，鼠标移动也不触发偏转计算
            if (System.currentTimeMillis() - lastExitTime < 300) {
                return;
            }
            handleMouseMoved(e.getScreenX(), e.getScreenY());
            applyMouseForce(e.getScreenX());
        });

        this.setOnMouseDragged(e -> {
            if (System.currentTimeMillis() - lastExitTime < 300) {
                return;
            }
            handleMouseMoved(e.getScreenX(), e.getScreenY());
            applyMouseForce(e.getScreenX());
        });

        this.setOnMouseExited(e -> {
            // 核心安全检测：利用 Parent 的投影计算，获取不包含 3D 旋转和 Scale 的原始静态包围盒。
            // 当 3D 倾斜偏转使卡片边缘移开鼠标时，若鼠标物理坐标依旧在静态的 2D 矩形框内，忽略此 Exit 事件，根除抖动！
            if (this.getParent() != null) {
                javafx.geometry.Bounds staticBounds = new javafx.geometry.BoundingBox(
                    this.getLayoutX(), this.getLayoutY(), this.getWidth(), this.getHeight()
                );
                javafx.geometry.Bounds screenBounds = this.getParent().localToScreen(staticBounds);
                if (screenBounds != null && screenBounds.contains(e.getScreenX(), e.getScreenY())) {
                    return; 
                }
            }

            this.setViewOrder(0.0);  // 恢复渲染层级
            targetScale = 1.0;
            targetRotateX = 0;
            targetRotateY = 0;
            
            lastMouseX = -1;
            lastTime = 0;
            lastExitTime = System.currentTimeMillis(); // 记录本次真正滑出的物理时间戳
            
            handleAnimation("disappear");
        });

        // 注册焦点事件（支持键盘聚焦触发动画，除非 noFocus 开启）
        this.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!getFinalNoFocus()) {
                if (newVal) {
                    targetScale = 1.05;
                    handleAnimation("appear");
                } else {
                    targetScale = 1.0;
                    targetRotateX = 0;
                    targetRotateY = 0;
                    handleAnimation("disappear");
                }
            }
        });

        // 绑定属性改变时重新初始化像素集
        variant.addListener((obs, oldVal, newVal) -> initPixels());
        gap.addListener((obs, oldVal, newVal) -> initPixels());
        speed.addListener((obs, oldVal, newVal) -> initPixels());
        colors.addListener((obs, oldVal, newVal) -> initPixels());
        noFocus.addListener((obs, oldVal, newVal) -> {
            this.setFocusTraversable(newVal == null || !newVal);
        });

        this.setFocusTraversable(!getFinalNoFocus());

        // 初始化动画定时器
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 限频 60 FPS (约 16.67 毫秒每帧)
                if (lastUpdate > 0) {
                    long elapsed = now - lastUpdate;
                    if (elapsed < 16_666_666) {
                        return;
                    }
                }
                lastUpdate = now;

                // 1. 物理单摆动力学计算 (Z 轴摆动)
                double dt = 0.016; 
                double gravityVal = 6.5;   // 重力恢复系数（调高使摆角回正力更强）
                double dampingVal = 3.5;   // 摆动阻尼（调高使摆动能快速静止）
                double angularAcceleration = -gravityVal * Math.sin(angle) - dampingVal * angularVelocity;
                angularVelocity += angularAcceleration * dt;
                angle += angularVelocity * dt;
                
                // 将弧度转换为角度赋给 Z 轴旋转器
                rotateZ.setAngle(Math.toDegrees(angle));
                
                // 平滑过渡角速度值，用于绳子弯曲曲线计算
                currentVelocity += (angularVelocity - currentVelocity) * 0.15;

                // 2. 3D 旋转和缩放比例过渡 (指数衰减模拟 Spring 阻尼弹簧效果)
                currentRotateX += (targetRotateX - currentRotateX) * 0.12;
                currentRotateY += (targetRotateY - currentRotateY) * 0.12;
                currentScale += (targetScale - currentScale) * 0.12;

                rotateX.setAngle(currentRotateX);
                rotateY.setAngle(currentRotateY);
                setScaleX(currentScale);
                setScaleY(currentScale);

                drawFrame();
            }
        };
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = getWidth();
        double h = getHeight();

        // 1. 动态设定 3D 偏转中心（Pivot），设为卡片身体（除去顶部挂绳预留区）的正中心
        double cx = w / 2.0;
        double cy = stringLength + (h - stringLength) / 2.0;
        rotateX.setPivotX(cx);
        rotateX.setPivotY(cy);
        rotateY.setPivotX(cx);
        rotateY.setPivotY(cy);

        // 2. 设定 2D 物理单摆中心（Z 轴旋转中心），设为卡片顶部的悬挂锚点
        rotateZ.setPivotX(cx);
        rotateZ.setPivotY(0);

        // 3. 当尺寸变化时自动缩放 Canvas 并重新初始化粒子分布
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            initPixels();
        }
        canvas.relocate(0, 0);

        // 4. 定位卡片背景区域 (cardBody) 仅覆盖底部卡片物理身体区域
        if (cardBody != null) {
            layoutInArea(cardBody, 0, stringLength, w, h - stringLength, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);
        }

        // 5. 定位所有 FXML 子内容节点，同样限定并平移在卡片身体区域内
        for (Node child : getChildren()) {
            if (child != canvas && child != cardBody) {
                layoutInArea(child, 0, stringLength, w, h - stringLength, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);
            }
        }
    }

    private double getEffectiveSpeed(int speedVal) {
        double min = 0;
        double max = 100;
        double throttle = 0.002;
        if (speedVal <= min) {
            return min;
        } else if (speedVal >= max) {
            return max * throttle;
        } else {
            return speedVal * throttle;
        }
    }

    private List<Color> parseColors(String colorsStr) {
        List<Color> list = new ArrayList<>();
        if (colorsStr == null || colorsStr.isEmpty()) {
            return list;
        }
        String[] parts = colorsStr.split(",");
        for (String part : parts) {
            try {
                list.add(Color.web(part.trim()));
            } catch (IllegalArgumentException e) {
                // 忽略无效颜色格式
            }
        }
        return list;
    }

    private void initPixels() {
        if (!isShowParticles()) {
            pixels.clear();
            return;
        }
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            pixels.clear();
            return;
        }

        int finalGapVal = getFinalGap();
        int finalSpeedVal = getFinalSpeed();
        String finalColorsVal = getFinalColors();

        List<Color> colorsList = parseColors(finalColorsVal);
        if (colorsList.isEmpty()) {
            colorsList.add(Color.LIGHTGRAY);
        }

        pixels.clear();
        double effSpeed = getEffectiveSpeed(finalSpeedVal);

        // 像素粒子的中心位置设在卡片身体正中央
        double centerX = width / 2.0;
        double centerY = stringLength + (height - stringLength) / 2.0;
        double maxDistance = Math.sqrt(centerX * centerX + (centerY - stringLength) * (centerY - stringLength));

        // 按照卡片宽度相对于 300px 基准的比例来计算粒子的等比缩放系数
        double scaleFactor = width / 300.0;

        Random random = new Random();
        for (double x = 0; x < width; x += finalGapVal) {
            // 粒子仅仅在挂绳区之下的卡片物理身体内部产生
            for (double y = stringLength; y < height; y += finalGapVal) {
                Color color = colorsList.get(random.nextInt(colorsList.size()));

                double dx = x - centerX;
                double dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                // 1. 粒子扩散速度调优：使用 0.65 作为延迟因子，使其扩散时有明显且舒适的涟漪波动感
                double delay = distance * 0.65;

                pixels.add(new Pixel(x, y, color, effSpeed, delay, width + height, distance, maxDistance, scaleFactor));
            }
        }
    }

    private void handleAnimation(String mode) {
        this.animationMode = mode;
        if (!isAnimating) {
            isAnimating = true;
            lastUpdate = 0;
            timer.start();
        }
    }

    private void drawFrame() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        // 1. 绘制挂绳 (Lanyard Band) 与挂钩夹子 (Clip/Clamp)
        VariantConfig cfg = getVariantConfig(getVariant());
        Color activeColor = cfg.activeColor != null ? cfg.activeColor : Color.web("#cbd5e1");
        
        // 挂绳颜色：将卡片变体主色调制为 60% 透明度，使得吊绳显得细腻温和
        gc.setStroke(Color.color(activeColor.getRed(), activeColor.getGreen(), activeColor.getBlue(), 0.60));
        gc.setLineWidth(2.8);
        gc.setLineCap(StrokeLineCap.ROUND);

        double centerX = width / 2.0;
        double ctrlY = stringLength * 0.48;
        // 绳线在晃动时根据速度产生反向的弯曲弯折，体现布料软体质感
        double ctrlX1 = centerX - 11.0 - currentVelocity * 30.0;
        double ctrlX2 = centerX + 11.0 - currentVelocity * 30.0;

        // 绘制吊带环圈的左半部分
        gc.beginPath();
        gc.moveTo(centerX, 0);
        gc.quadraticCurveTo(ctrlX1, ctrlY, centerX, stringLength);
        gc.stroke();

        // 绘制吊带环圈的右半部分
        gc.beginPath();
        gc.moveTo(centerX, 0);
        gc.quadraticCurveTo(ctrlX2, ctrlY, centerX, stringLength);
        gc.stroke();

        // 绘制卡片身体与挂钩的挂孔搭扣
        gc.setFill(Color.web("#94a3b8")); // 银灰色夹子扣
        gc.fillOval(centerX - 4.5, stringLength - 4.5, 9, 9);
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.0);
        gc.strokeOval(centerX - 4.5, stringLength - 4.5, 9, 9);

        // 2. 绘制像素微尘粒子
        boolean allIdle = true;
        if (isShowParticles()) {
            for (Pixel pixel : pixels) {
                if ("appear".equals(animationMode)) {
                    pixel.appear(gc);
                } else {
                    pixel.disappear(gc);
                }
                if (!pixel.isIdle()) {
                    allIdle = false;
                }
            }
        }

        // 3. 确保粒子、3D 侧倾、缩放、以及 2D 单摆悬挂晃动全部静止收敛后，再停止动画以节约 CPU 资源
        double rotateThreshold = 0.05;
        double scaleThreshold = 0.005;
        boolean rotatedSettled = Math.abs(currentRotateX - targetRotateX) < rotateThreshold
                && Math.abs(currentRotateY - targetRotateY) < rotateThreshold;
        boolean scaleSettled = Math.abs(currentScale - targetScale) < scaleThreshold;
        boolean swingSettled = Math.abs(angle) < 0.005 && Math.abs(angularVelocity) < 0.01;

        if (allIdle && rotatedSettled && scaleSettled && swingSettled) {
            angle = 0;
            angularVelocity = 0;
            currentVelocity = 0;
            rotateZ.setAngle(0);

            currentRotateX = targetRotateX;
            currentRotateY = targetRotateY;
            currentScale = targetScale;
            rotateX.setAngle(currentRotateX);
            rotateY.setAngle(currentRotateY);
            setScaleX(currentScale);
            setScaleY(currentScale);

            timer.stop();
            isAnimating = false;
            if ("disappear".equals(animationMode)) {
                gc.clearRect(0, 0, width, height);
            }
        }
    }

    private void handleMouseMoved(double screenX, double screenY) {
        Bounds bounds = this.localToScreen(this.getBoundsInLocal());
        if (bounds == null) return;

        double rectLeft = bounds.getMinX();
        double rectTop = bounds.getMinY();
        double rectWidth = bounds.getWidth();
        double rectHeight = bounds.getHeight();

        if (rectWidth <= 0 || rectHeight <= 0) return;

        // 使用除去吊绳区之后的卡片身体中央计算 3D 侧倾
        double centerYScreen = rectTop + stringLength + (rectHeight - stringLength) / 2.0;
        double centerXScreen = rectLeft + rectWidth / 2.0;

        double offsetX = screenX - centerXScreen;
        double offsetY = screenY - centerYScreen;

        // 旋转振幅提升至 18 度，使 3D 偏转侧倾倾斜效果更加夸张和明显
        double amplitude = 18.0;
        double rawRotateX = (offsetY / ((rectHeight - stringLength) / 2.0)) * -amplitude;
        double rawRotateY = (offsetX / (rectWidth / 2.0)) * amplitude;

        // 边缘阻尼渐变：当鼠标接近卡片边缘时，平滑降低偏转角度，边缘处降为 0，防止物理抖动
        double localX = screenX - rectLeft;
        double localY = screenY - rectTop;
        double localYInBody = localY - stringLength;

        double distL = Math.max(0, localX);
        double distR = Math.max(0, rectWidth - localX);
        double distT = Math.max(0, localYInBody);
        double distB = Math.max(0, rectHeight - localY);

        double minDist = Math.min(Math.min(distL, distR), Math.min(distT, distB));
        
        // 渐变过渡区设为 25px
        double fadeZone = 25.0;
        double dampingFactor = minDist < fadeZone ? minDist / fadeZone : 1.0;

        targetRotateX = rawRotateX * dampingFactor;
        targetRotateY = rawRotateY * dampingFactor;

        if (!isAnimating) {
            isAnimating = true;
            lastUpdate = 0;
            timer.start();
        }
    }

    private void applyMouseForce(double screenX) {
        long now = System.nanoTime();
        if (lastMouseX >= 0 && lastTime > 0) {
            double dx = screenX - lastMouseX;
            double dt = (now - lastTime) / 1_000_000_000.0;
            if (dt > 0) {
                double mouseVx = dx / dt;
                // 力学推力系数：根据鼠标横向速度，产生 Z 轴角度角加速度 (由 0.00008 调小为 0.000025，使晃动幅度变得轻柔优雅)
                double force = mouseVx * 0.000025;
                // 限制最大瞬时力，防止甩动过度 (限制从 0.6 缩减为 0.15)
                force = Math.max(-0.15, Math.min(0.15, force));
                angularVelocity += force;
            }
        }
        lastMouseX = screenX;
        lastTime = now;

        if (!isAnimating) {
            isAnimating = true;
            lastUpdate = 0;
            timer.start();
        }
    }

    // 粒子内部嵌套类，描述每个像素点的状态、运动和大小
    private static class Pixel {
        private final double x;
        private final double y;
        private final Color color;
        private final double speed;
        private final double maxSize;
        private final double delay;
        private final double counterStep;

        private double size = 0;
        private final double sizeStep;
        // 2. 微调粒子大小，保持细腻微小的极简像素星尘感，避免粒子过大显得粗糙
        private final double scaleFactor;
        private final double minSizeScaled;
        private final double maxSizeIntegerScaled;

        private double counter = 0;
        private boolean isIdle = false;
        private boolean isReverse = false;
        private boolean isShimmer = false;

        private static final Random random = new Random();

        public Pixel(double x, double y, Color color, double speed, double delay, double widthPlusHeight, double distance, double maxDistance, double scaleFactor) {
            this.x = x;
            this.y = y;
            this.scaleFactor = scaleFactor;
            // 粒子缩放时其呼吸闪烁位移速度等比缩放
            this.speed = (0.15 + random.nextDouble() * 0.7) * speed * scaleFactor;
            this.sizeStep = random.nextDouble() * 0.45 * scaleFactor;
            this.delay = delay;
            this.counterStep = random.nextDouble() * 4.0 + widthPlusHeight * 0.01;

            // 3. 中间淡四周浓：离中心越近，粒子越小且越透明（淡化）；越往四周粒子越大且越清晰
            double ratio = 0.35 + 0.65 * (distance / maxDistance);

            double baseMin = 1.6 * scaleFactor;
            double baseMax = 4.8 * scaleFactor;
            this.maxSizeIntegerScaled = baseMax;

            this.minSizeScaled = baseMin * ratio;
            this.maxSize = (baseMin + random.nextDouble() * (baseMax - baseMin)) * ratio;
            
            // 用 ratio 动态缩放颜色的不透明度（Opacity），达到“中间淡四周浓”的精细渐变感
            this.color = Color.color(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity() * ratio);
        }

        public boolean isIdle() {
            return isIdle;
        }

        public void draw(GraphicsContext gc) {
            // 保持颗粒绘制居中对齐
            double centerOffset = maxSizeIntegerScaled * 0.5 - size * 0.5;
            gc.setFill(color);
            gc.fillRect(x + centerOffset, y + centerOffset, size, size);
        }

        public void appear(GraphicsContext gc) {
            isIdle = false;
            if (counter <= delay) {
                counter += counterStep;
                return;
            }
            if (size >= maxSize) {
                isShimmer = true;
            }
            if (isShimmer) {
                shimmer();
            } else {
                size += sizeStep;
            }
            draw(gc);
        }

        public void disappear(GraphicsContext gc) {
            isShimmer = false;
            counter = 0;
            if (size <= 0) {
                isIdle = true;
                return;
            } else {
                // 等比缩减消失收缩速度，保证在大卡片和小卡片上粒子完全收缩回去的耗时完全一致
                size -= 0.1 * scaleFactor;
            }
            draw(gc);
        }

        private void shimmer() {
            if (size >= maxSize) {
                isReverse = true;
            } else if (size <= minSizeScaled) {
                isReverse = false;
            }
            if (isReverse) {
                size -= speed;
            } else {
                size += speed;
            }
        }
    }
}
