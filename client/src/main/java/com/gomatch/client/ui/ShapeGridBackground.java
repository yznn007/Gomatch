package com.gomatch.client.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * 仿 React Bits - Shape Grid 效果的动态交互背景组件。
 * 在浅色渐变背景上，渲染出一层由微弱加号、圆点、正方形组成的几何点阵。
 * 支持：
 * 1. 背景向着左上角平滑且无缝地滚动移动。
 * 2. 鼠标移动时的局部放大与高光呼吸灯悬停效果（Hover Trail）。
 * 3. 增强点阵的明度和线条宽度，使其更加显化可见。
 */
public class ShapeGridBackground extends Pane {
    private final Canvas canvas;
    private double mouseX = -1000;
    private double mouseY = -1000;
    private double targetMouseX = -1000;
    private double targetMouseY = -1000;

    private static final int CELL_SIZE = 65; // 点阵间距稍微拉开一点
    private static final double MAX_REACTION_DIST = 180.0; // 鼠标影响半径
    
    // 向左上角滚动的速度（每秒像素数，代替原先的每帧固定像素，确保流速恒定）
    private static final double SPEED_X_PER_SEC = 17.0; 
    private static final double SPEED_Y_PER_SEC = 17.0;

    // 扁平点阵数据与无缝包裹尺寸
    private GridPoint[] points;
    private double totalWidth = 0;
    private double totalHeight = 0;
    private long lastTime = 0;

    public ShapeGridBackground() {
        canvas = new Canvas(1, 1);
        getChildren().add(canvas);

        // 避免背景吞噬鼠标点击事件，只用于渲染，不拦截事件
        setMouseTransparent(true);

        // 监听尺寸变化，动态重算点阵大小
        widthProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
        heightProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());

        // 启动高性能动画渲染渲染器
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (points == null) {
                    lastTime = now;
                    return;
                }
                if (lastTime == 0) {
                    lastTime = now;
                }
                double dt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                // 限制最大帧时间，避免挂起后产生突跃
                if (dt > 0.1) {
                    dt = 0.1;
                }

                // 缓动鼠标位置，让轨迹跟随更平滑
                mouseX += (targetMouseX - mouseX) * 0.15;
                mouseY += (targetMouseY - mouseY) * 0.15;

                updateAndRender(dt);
            }
        };
        timer.start();
    }

    /**
     * 由外部（如父级 StackPane）统一分发鼠标移动事件，规避层级遮挡导致无法触发监听的问题
     */
    public void setMousePosition(double x, double y) {
        this.targetMouseX = x;
        this.targetMouseY = y;
    }

    private void resizeCanvas() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (Math.abs(canvas.getWidth() - w) < 1 && Math.abs(canvas.getHeight() - h) < 1) return;

        canvas.setWidth(w);
        canvas.setHeight(h);

        // 初始化点阵数据（为了实现无缝包裹，cols/rows 增加 2 块空白冗余）
        int cols = (int) (w / CELL_SIZE) + 3;
        int rows = (int) (h / CELL_SIZE) + 3;
        
        totalWidth = cols * CELL_SIZE;
        totalHeight = rows * CELL_SIZE;
        
        points = new GridPoint[cols * rows];

        int idx = 0;
        Random rand = new Random(42); // 固定种子
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double x = c * CELL_SIZE;
                double y = r * CELL_SIZE;
                int shapeType = rand.nextInt(3); // 0=加号, 1=小圆圈, 2=微型正方形
                points[idx++] = new GridPoint(x, y, shapeType);
            }
        }
    }

    private void updateAndRender(double dt) {
        double cw = canvas.getWidth();
        double ch = canvas.getHeight();
        if (cw <= 0 || ch <= 0) return;
        if (points == null) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cw, ch);

        double moveX = SPEED_X_PER_SEC * dt;
        double moveY = SPEED_Y_PER_SEC * dt;

        for (GridPoint p : points) {
            if (p == null) continue;

            // 1. 无缝向左上角移动 (基于时间步长)
            p.x -= moveX;
            p.y -= moveY;

            // 越界向右下角包裹
            if (p.x < -CELL_SIZE) {
                p.x += totalWidth;
            }
            if (p.y < -CELL_SIZE) {
                p.y += totalHeight;
            }

            // 2. 计算到鼠标的距离
            double dx = p.x - mouseX;
            double dy = p.y - mouseY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            // 3. 鼠标交互缩放和渐变增益
            double activeFactor = 0.0;
            if (dist < MAX_REACTION_DIST) {
                activeFactor = 1.0 - (dist / MAX_REACTION_DIST); // 0.0 到 1.0
            }

            // 目标缩放与淡入值
            p.scale += (p.baseScale + activeFactor * 1.1 - p.scale) * 0.12;
            p.alpha += (p.baseAlpha + activeFactor * 0.45 - p.alpha) * 0.12;

            drawShape(gc, p);
        }
    }

    private void drawShape(GraphicsContext gc, GridPoint p) {
        gc.save();
        
        // 显化背景：调深颜色灰度（使点阵更明显），线宽从 1.2 提升到 1.6
        gc.setStroke(Color.color(115 / 255.0, 130 / 255.0, 150 / 255.0, p.alpha));
        gc.setFill(Color.color(115 / 255.0, 130 / 255.0, 150 / 255.0, p.alpha));
        gc.setLineWidth(1.6 * p.scale);

        double size = p.size * p.scale;

        switch (p.shapeType) {
            case 0 -> { // 绘制“+”加号
                gc.strokeLine(p.x - size, p.y, p.x + size, p.y);
                gc.strokeLine(p.x, p.y - size, p.x, p.y + size);
            }
            case 1 -> { // 绘制微型空心圆
                gc.strokeOval(p.x - size, p.y - size, size * 2, size * 2);
            }
            case 2 -> { // 绘制微型正方形
                gc.strokeRect(p.x - size, p.y - size, size * 2, size * 2);
            }
        }

        gc.restore();
    }

    private static class GridPoint {
        double x;
        double y;
        final int shapeType;
        final double size = 5.5; // 几何形状本身稍微变大一点
        
        double scale = 1.0;
        double alpha = 0.28;

        final double baseScale = 0.90;
        final double baseAlpha = 0.28; // 显化背景：基础可见度从 0.16 提高到 0.28

        GridPoint(double x, double y, int shapeType) {
            this.x = x;
            this.y = y;
            this.shapeType = shapeType;
            this.scale = baseScale;
            this.alpha = baseAlpha;
        }
    }
}
