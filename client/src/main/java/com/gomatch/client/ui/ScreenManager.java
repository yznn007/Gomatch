package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.UserSettings;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class ScreenManager {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final String STYLE_PATH = "/css/style.css";
    private static final String THEME_LIGHT = "theme-light";
    private static final String THEME_DARK = "theme-dark";
    private static Stage stage;
    private static boolean firstShow = true;

    // 窗口移动与拉伸临时变量
    private static double xOffset = 0;
    private static double yOffset = 0;
    private static double dragStartX = 0;
    private static double dragStartY = 0;
    private static double stageStartX = 0;
    private static double stageStartY = 0;
    private static double stageStartWidth = 0;
    private static double stageStartHeight = 0;
    private static ResizeMode resizeMode = ResizeMode.NONE;

    // 解决无边框窗口最大化遮挡 Windows 任务栏的自定义最大化状态
    private static boolean isCustomMaximized = false;
    private static double prevX = 0;
    private static double prevY = 0;
    private static double prevWidth = WIDTH;
    private static double prevHeight = HEIGHT;

    // 当前场景的动态背景引用，供设置页实时切换
    private static ShapeGridBackground currentBackground;

    private enum ResizeMode {
        NONE, N, S, W, E, NW, NE, SW, SE
    }

    public static void init(Stage s) {
        stage = s;
        stage.setTitle("Gomatch");
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        stage.setResizable(true);

        // 设置为无系统装饰边框窗口，实现自定义沉浸式标题栏
        stage.initStyle(StageStyle.UNDECORATED);

        // 核心修复：监听最小化还原事件，确保在 Windows 任务栏点击图标时能正确唤醒和置顶无边框 stage
        stage.iconifiedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                stage.show();
                stage.toFront();
            }
        });
    }

    public static <T> T switchTo(String fxmlPath) {
        try {
            // 切换前清除所有旧监听，防止内存泄漏和消息乱投
            ClientMain.getConnector().clearListeners();

            FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
            loader.setResources(I18N.getBundle());
            Parent root = loader.load();
            T controller = loader.getController();

            // 1. 创建自定义沉浸式标题栏
            HBox customTitleBar = createCustomTitleBar();

            // 2. 将标题栏与业务 UI 垂直布局，VBox 自适应填满窗口
            VBox mainLayout = new VBox();
            mainLayout.setStyle("-fx-background-color: transparent;");
            VBox.setVgrow(root, Priority.ALWAYS);
            mainLayout.getChildren().addAll(customTitleBar, root);

            // 3. 包装一层 StackPane，将 ShapeGridBackground 动态粒子点阵放在最底层，主 UI 在上层
            StackPane wrapper = new StackPane();
            ShapeGridBackground bg = new ShapeGridBackground();
            bg.setEnabled(UserSettings.get().dynamicBackground);
            currentBackground = bg;
            wrapper.getChildren().addAll(bg, mainLayout);

            // 主布局占满外层容器并从左上角布局，保证标题栏右侧按钮始终贴合窗口右边缘。
            StackPane.setAlignment(mainLayout, javafx.geometry.Pos.TOP_LEFT);
            mainLayout.maxWidthProperty().bind(wrapper.widthProperty());
            mainLayout.maxHeightProperty().bind(wrapper.heightProperty());

            // 4. 监听鼠标移动，将其坐标转发给底层的 ShapeGridBackground
            wrapper.setOnMouseMoved(e -> {
                bg.setMousePosition(e.getX(), e.getY());
                updateCursor(e.getX(), e.getY());
            });
            wrapper.setOnMouseExited(e -> bg.setMousePosition(-1000, -1000));

            // 5. 使用事件过滤器（Event Filter）在事件到达子节点（卡片或按钮）前拦截并消费边框拉伸事件
            wrapper.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                ResizeMode mode = getResizeMode(e.getX(), e.getY());
                if (mode != ResizeMode.NONE) {
                    dragStartX = e.getScreenX();
                    dragStartY = e.getScreenY();
                    stageStartX = stage.getX();
                    stageStartY = stage.getY();
                    stageStartWidth = stage.getWidth();
                    stageStartHeight = stage.getHeight();
                    resizeMode = mode;
                    e.consume(); // 拦截并消费事件，避免传递给子节点（卡片或按钮）
                }
            });

            wrapper.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
                if (resizeMode != ResizeMode.NONE) {
                    handleResize(e.getScreenX(), e.getScreenY());
                    e.consume(); // 拦截并消费事件，避免子卡片物理晃动产生异常
                }
            });

            wrapper.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
                if (resizeMode != ResizeMode.NONE) {
                    resizeMode = ResizeMode.NONE;
                    e.consume(); // 拦截并消费事件
                }
            });

            Scene oldScene = stage.getScene();
            double w = oldScene == null ? WIDTH : oldScene.getWidth();
            double h = oldScene == null ? HEIGHT : oldScene.getHeight();
            Scene scene = new Scene(wrapper, w, h);
            scene.setCamera(new javafx.scene.PerspectiveCamera(false));
            
            // 核心绑定：将标题栏宽度绑定至 mainLayout 宽度，保证在缩放时仍与主界面容器完美贴合
            customTitleBar.prefWidthProperty().bind(mainLayout.widthProperty());
            
            applyStylesheet(scene);
            applyUserTheme(scene);
            applyUserFont(scene);
            stage.setScene(scene);

            if (firstShow) {
                stage.centerOnScreen();
                firstShow = false;
            }
            stage.show();
            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public static <T> T showDialog(String fxmlPath) {
        return showDialog(fxmlPath, controller -> {});
    }

    public static <T> T showDialog(String fxmlPath, java.util.function.Consumer<T> setupCallback) {
        try {
            FXMLLoader loader = new FXMLLoader(ScreenManager.class.getResource(fxmlPath));
            loader.setResources(I18N.getBundle());
            Parent root = loader.load();
            T controller = loader.getController();
            if (setupCallback != null) {
                setupCallback.accept(controller);
            }
            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.initModality(Modality.WINDOW_MODAL);
            
            // 弹窗使用透明窗口，使圆角内容边缘不受原生背景干扰
            dialog.initStyle(StageStyle.TRANSPARENT);

            // 给弹窗也包上动态点阵背景和自定义小标题栏
            HBox dialogTitleBar = createDialogTitleBar(dialog);

            VBox dialogLayout = new VBox();
            dialogLayout.setStyle("-fx-background-color: transparent;");
            VBox.setVgrow(root, Priority.ALWAYS);
            dialogLayout.getChildren().addAll(dialogTitleBar, root);

            StackPane dialogWrapper = new StackPane();
            ShapeGridBackground bg = new ShapeGridBackground();
            dialogWrapper.getChildren().addAll(bg, dialogLayout);

            dialogWrapper.setOnMouseMoved(e -> bg.setMousePosition(e.getX(), e.getY()));
            dialogWrapper.setOnMouseExited(e -> bg.setMousePosition(-1000, -1000));

            Scene scene = new Scene(dialogWrapper);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.setCamera(new javafx.scene.PerspectiveCamera(false));
            
            // 绑定弹窗标题栏宽度
            dialogTitleBar.prefWidthProperty().bind(scene.widthProperty());
            
            applyStylesheet(scene);
            applyDialogTheme(scene);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();
            return controller;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dialog FXML: " + fxmlPath, e);
        }
    }

    private static HBox createCustomTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("custom-title-bar");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 最小化按钮
        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("title-bar-btn");
        minBtn.setOnAction(e -> stage.setIconified(true));

        // 最大化按钮
        Button maxBtn = new Button("⛶");
        maxBtn.getStyleClass().addAll("title-bar-btn", "title-bar-max");
        maxBtn.setOnAction(e -> toggleMaximize());

        // 关闭按钮
        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().addAll("title-bar-btn", "title-bar-close");
        closeBtn.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        titleBar.getChildren().addAll(spacer, minBtn, maxBtn, closeBtn);

        // 双击标题栏最大化 / 还原
        titleBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleMaximize();
            }
        });

        // 鼠标按下：记录拖拽起点
        titleBar.setOnMousePressed(event -> {
            if (getResizeMode(event.getX(), event.getY()) == ResizeMode.NONE) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });

        // 鼠标拖动：支持窗口位移与【最大化状态下拖拽自动还原且居中鼠标】的高级特性
        titleBar.setOnMouseDragged(event -> {
            if (getResizeMode(event.getX(), event.getY()) == ResizeMode.NONE) {
                if (isCustomMaximized) {
                    double ratio = event.getX() / stage.getWidth();
                    toggleMaximize(); // 自动还原窗口大小
                    
                    // 精准重置偏移量，使还原后的窗口在鼠标正下方
                    xOffset = stage.getWidth() * ratio;
                    yOffset = event.getY();
                }
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // 鼠标释放：支持 Windows Aero Snap 贴边分屏模拟
        titleBar.setOnMouseReleased(event -> {
            double screenX = event.getScreenX();
            double screenY = event.getScreenY();
            
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();

            // 1. 拖到屏幕最顶端 -> 最大化 (使用自定义最大化，不挡任务栏)
            if (screenY < visualBounds.getMinY() + 6) {
                if (!isCustomMaximized) {
                    toggleMaximize();
                }
            } 
            // 2. 拖到屏幕最左侧 -> 左半屏分屏
            else if (screenX < visualBounds.getMinX() + 6) {
                if (isCustomMaximized) {
                    isCustomMaximized = false;
                }
                stage.setX(visualBounds.getMinX());
                stage.setY(visualBounds.getMinY());
                stage.setWidth(visualBounds.getWidth() / 2.0);
                stage.setHeight(visualBounds.getHeight());
            } 
            // 3. 拖到屏幕最右侧 -> 右半屏分屏
            else if (screenX > visualBounds.getMaxX() - 6) {
                if (isCustomMaximized) {
                    isCustomMaximized = false;
                }
                stage.setX(visualBounds.getMinX() + visualBounds.getWidth() / 2.0);
                stage.setY(visualBounds.getMinY());
                stage.setWidth(visualBounds.getWidth() / 2.0);
                stage.setHeight(visualBounds.getHeight());
            }
        });

        return titleBar;
    }

    /**
     * 自定义最大化实现，规避 StageStyle.UNDECORATED 在全屏最大化时遮挡 Windows 系统任务栏的 Bug。
     * 最大化时窗口会贴合当前屏幕的可用工作区（排除任务栏）。
     */
    private static void toggleMaximize() {
        if (isCustomMaximized) {
            // 还原到先前的大小和位置
            stage.setX(prevX);
            stage.setY(prevY);
            stage.setWidth(prevWidth);
            stage.setHeight(prevHeight);
            isCustomMaximized = false;
        } else {
            // 保存当前位置大小
            prevX = stage.getX();
            prevY = stage.getY();
            prevWidth = stage.getWidth();
            prevHeight = stage.getHeight();
            
            // 获取当前屏幕可用视觉边界（即扣除了系统底栏任务栏后的工作区）
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
            isCustomMaximized = true;
        }
    }

    private static HBox createDialogTitleBar(Stage dialog) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("custom-title-bar");
        titleBar.setStyle("-fx-pref-height: 32; -fx-min-height: 32; -fx-max-height: 32;");

        Label titleLabel = new Label(I18N.get("message.title"));
        titleLabel.getStyleClass().add("muted");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().addAll("title-bar-btn", "title-bar-close");
        closeBtn.setStyle("-fx-pref-height: 32; -fx-pref-width: 36;");
        closeBtn.setOnAction(e -> dialog.close());

        titleBar.getChildren().addAll(titleLabel, spacer, closeBtn);

        // 拖拽弹窗
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            dialog.setX(event.getScreenX() - xOffset);
            dialog.setY(event.getScreenY() - yOffset);
        });

        return titleBar;
    }

    private static void applyStylesheet(Scene scene) {
        String css = ScreenManager.class.getResource(STYLE_PATH).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }

    /** 供外部类（如 BoardController）动态创建弹窗时注入全局 CSS，效果等同于内部 applyStylesheet。 */
    public static void applyStylesheetPublic(Scene scene) {
        applyStylesheet(scene);
    }


    /** 当前是否使用暗夜黑磨砂主题。 */
    public static boolean isDarkTheme() {
        String theme = UserSettings.get().theme;
        return theme != null && (theme.contains("暗夜") || theme.toLowerCase().contains("dark") || theme.contains("Frosted Dark"));
    }

    /** 给当前 Scene 根节点挂载主题类，浅色原色与暗色覆盖层互斥。 */
    public static void applyUserTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        boolean dark = isDarkTheme();
        scene.getRoot().getStyleClass().removeAll(THEME_LIGHT, THEME_DARK);
        scene.getRoot().getStyleClass().add(dark ? THEME_DARK : THEME_LIGHT);

        String background = dark
                ? "linear-gradient(to bottom right, #010208, #040713 46%, #080c16)"
                : "transparent";
        scene.getRoot().setStyle(appendBackgroundColor(scene.getRoot().getStyle(), background));
    }

    /** 给透明弹窗挂载主题类，避免写入主窗口的深色渐变背景。 */
    public static void applyDialogTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        scene.getRoot().getStyleClass().removeAll(THEME_LIGHT, THEME_DARK);
        scene.getRoot().getStyleClass().add(isDarkTheme() ? THEME_DARK : THEME_LIGHT);
        if (scene.getRoot() instanceof javafx.scene.control.DialogPane) {
            scene.getRoot().setStyle(removeBackgroundColor(scene.getRoot().getStyle()));
        } else {
            scene.getRoot().setStyle(appendBackgroundColor(scene.getRoot().getStyle(), "transparent"));
        }
        applyUserFont(scene);
    }

    /** 动态生成控件使用的主题色。 */
    public static String themeColor(String lightColor, String darkColor) {
        return isDarkTheme() ? darkColor : lightColor;
    }

    /** 根据用户偏好设置全局字体（不覆盖其他内联样式） */
    public static void applyUserFont(Scene scene) {
        if (scene.getRoot() == null) return;
        String fontFamily = UserSettings.get().fontFamily;
        if (fontFamily != null) {
            if (fontFamily.contains("系统默认") || fontFamily.contains("System")) {
                scene.getRoot().setStyle(appendFontFamily(scene.getRoot().getStyle(), "\"Segoe UI\", \"Microsoft YaHei\", sans-serif"));
            } else {
                scene.getRoot().setStyle(appendFontFamily(scene.getRoot().getStyle(), "\"Maple Mono NF CN\", \"Maple-Mono-NF-CN\", \"Maple Mono\", sans-serif"));
            }
        }
    }


    /** 在已有 style 字符串中追加或替换 -fx-background-color，保留字体等其他属性 */
    private static String appendBackgroundColor(String existingStyle, String backgroundValue) {
        String newProp = "-fx-background-color: " + backgroundValue + ";";
        if (existingStyle == null || existingStyle.isBlank()) return newProp;
        if (existingStyle.contains("-fx-background-color:")) {
            return existingStyle.replaceAll("-fx-background-color:\\s*[^;]+;", newProp);
        }
        return existingStyle + " " + newProp;
    }
    /** 移除内联背景色，让 DialogPane 的主题 CSS 能绘制圆角表面。 */
    private static String removeBackgroundColor(String existingStyle) {
        if (existingStyle == null || existingStyle.isBlank()) return "";
        return existingStyle.replaceAll("-fx-background-color:\\s*[^;]+;", "").trim();
    }
    /** 在已有 style 字符串中追加或替换 -fx-font-family，保留其他属性 */
    private static String appendFontFamily(String existingStyle, String fontFamilyValue) {
        String newProp = "-fx-font-family: " + fontFamilyValue + ";";
        if (existingStyle == null || existingStyle.isBlank()) return newProp;
        if (existingStyle.contains("-fx-font-family:")) {
            return existingStyle.replaceAll("-fx-font-family:\\s*[^;]+;", newProp);
        }
        return existingStyle + " " + newProp;
    }

    /** 获取当前场景的动态背景实例，供设置页实时切换 */
    public static ShapeGridBackground getCurrentBackground() {
        return currentBackground;
    }

    // ---------- 无边框窗口拉伸算法 ----------
    private static final int BORDER_WIDTH = 6;

    private static ResizeMode getResizeMode(double x, double y) {
        if (isCustomMaximized) return ResizeMode.NONE;
        
        double w = stage.getWidth();
        double h = stage.getHeight();

        // 屏蔽右上角按钮区的边缘拉伸响应，防止误触导致无法点击关闭/最小化
        if (y < 38 && x > w - 150) {
            return ResizeMode.NONE;
        }

        boolean north = y < BORDER_WIDTH;
        boolean south = y > h - BORDER_WIDTH;
        boolean west = x < BORDER_WIDTH;
        boolean east = x > w - BORDER_WIDTH;

        if (north && west) return ResizeMode.NW;
        if (north && east) return ResizeMode.NE;
        if (south && west) return ResizeMode.SW;
        if (south && east) return ResizeMode.SE;
        if (north) return ResizeMode.N;
        if (south) return ResizeMode.S;
        if (west) return ResizeMode.W;
        if (east) return ResizeMode.E;

        return ResizeMode.NONE;
    }

    private static void updateCursor(double x, double y) {
        if (isCustomMaximized) {
            stage.getScene().setCursor(Cursor.DEFAULT);
            return;
        }

        ResizeMode mode = getResizeMode(x, y);
        Cursor cursor = switch (mode) {
            case NW -> Cursor.NW_RESIZE;
            case NE -> Cursor.NE_RESIZE;
            case SW -> Cursor.SW_RESIZE;
            case SE -> Cursor.SE_RESIZE;
            case N -> Cursor.N_RESIZE;
            case S -> Cursor.S_RESIZE;
            case W -> Cursor.W_RESIZE;
            case E -> Cursor.E_RESIZE;
            default -> Cursor.DEFAULT;
        };
        stage.getScene().setCursor(cursor);
    }

    private static void handleResize(double screenX, double screenY) {
        double dx = screenX - dragStartX;
        double dy = screenY - dragStartY;

        double newWidth = stageStartWidth;
        double newHeight = stageStartHeight;
        double newX = stageStartX;
        double newY = stageStartY;

        // 水平调整
        if (resizeMode == ResizeMode.W || resizeMode == ResizeMode.NW || resizeMode == ResizeMode.SW) {
            newWidth = stageStartWidth - dx;
            if (newWidth >= stage.getMinWidth()) {
                newX = stageStartX + dx;
            } else {
                newWidth = stage.getMinWidth();
                newX = stageStartX + (stageStartWidth - stage.getMinWidth());
            }
        } else if (resizeMode == ResizeMode.E || resizeMode == ResizeMode.NE || resizeMode == ResizeMode.SE) {
            newWidth = stageStartWidth + dx;
            newWidth = Math.max(newWidth, stage.getMinWidth());
        }

        // 垂直调整
        if (resizeMode == ResizeMode.N || resizeMode == ResizeMode.NW || resizeMode == ResizeMode.NE) {
            newHeight = stageStartHeight - dy;
            if (newHeight >= stage.getMinHeight()) {
                newY = stageStartY + dy;
            } else {
                newHeight = stage.getMinHeight();
                newY = stageStartY + (stageStartHeight - stage.getMinHeight());
            }
        } else if (resizeMode == ResizeMode.S || resizeMode == ResizeMode.SW || resizeMode == ResizeMode.SE) {
            newHeight = stageStartHeight + dy;
            newHeight = Math.max(newHeight, stage.getMinHeight());
        }

        stage.setX(newX);
        stage.setY(newY);
        stage.setWidth(newWidth);
        stage.setHeight(newHeight);
    }

    public static Stage getStage() {
        return stage;
    }

    public static <R> java.util.Optional<R> showStyledDialog(javafx.scene.control.Dialog<R> dialog) {
        if (dialog != null) {
            try {
                dialog.initStyle(StageStyle.TRANSPARENT);
            } catch (IllegalStateException e) {
                // Ignore if dialog is already showing or initialized
            }
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            if (dialogPane != null) {
                String css = ScreenManager.class.getResource(STYLE_PATH).toExternalForm();
                if (!dialogPane.getStylesheets().contains(css)) {
                    dialogPane.getStylesheets().add(css);
                }

                // Enable dragging for undecorated dialogs
                final double[] xOffset = new double[1];
                final double[] yOffset = new double[1];
                dialogPane.setOnMousePressed(event -> {
                    xOffset[0] = event.getSceneX();
                    yOffset[0] = event.getSceneY();
                });
                dialogPane.setOnMouseDragged(event -> {
                    javafx.stage.Window window = dialogPane.getScene().getWindow();
                    if (window != null) {
                        window.setX(event.getScreenX() - xOffset[0]);
                        window.setY(event.getScreenY() - yOffset[0]);
                    }
                });

                // Clear native dialog window background to display beautiful rounded corners properly
                dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        newScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                        applyDialogTheme(newScene);
                        newScene.fillProperty().addListener((o, oldFill, newFill) -> {
                            if (newFill != javafx.scene.paint.Color.TRANSPARENT) {
                                newScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                            }
                        });
                    }
                });
                Scene scene = dialogPane.getScene();
                if (scene != null) {
                    scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                    applyDialogTheme(scene);
                    scene.fillProperty().addListener((o, oldFill, newFill) -> {
                        if (newFill != javafx.scene.paint.Color.TRANSPARENT) {
                            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                        }
                    });
                }
            }
            if (stage != null) {
                dialog.initOwner(stage);
            }
            return dialog.showAndWait();
        }
        return java.util.Optional.empty();
    }

    public static java.util.Optional<javafx.scene.control.ButtonType> showStyledAlert(javafx.scene.control.Alert alert) {
        return showStyledDialog(alert);
    }
}
