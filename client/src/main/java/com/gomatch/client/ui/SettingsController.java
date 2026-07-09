package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.ClientMain;
import com.gomatch.client.UserSettings;
import com.gomatch.client.audio.BackgroundMusicManager;
import com.gomatch.client.model.User;
import com.gomatch.client.network.ClientConfig;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;

public class SettingsController {
    // 服务器配置
    @FXML private ComboBox<String> ipComboBox;
    @FXML private TextField portField;
    @FXML private Label statusLabel;

    // 基础设置
    @FXML private CheckBox autoStartCheckBox;
    @FXML private CheckBox rememberPasswordCheckBox;

    // 图像设置
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private CheckBox fullscreenCheckBox;
    @FXML private CheckBox vsyncCheckBox;
    @FXML private ComboBox<String> fpsComboBox;

    // 声音设置（远程新增的音频控件）
    @FXML private CheckBox globalMusicCheckBox;
    @FXML private ElasticVolumeSlider globalVolumeSlider;
    @FXML private CheckBox backgroundMusicCheckBox;
    @FXML private ElasticVolumeSlider backgroundVolumeSlider;
    @FXML private CheckBox soundCheckBox;
    @FXML private ElasticVolumeSlider soundVolumeSlider;
    @FXML private ComboBox<String> lobbySongComboBox;

    // 声音设置（本地扩展）
    @FXML private Slider volumeSlider;
    @FXML private CheckBox bgMusicCheckBox;
    @FXML private CheckBox soundEffectCheckBox;
    @FXML private CheckBox lobbyMusicCheckBox;
    @FXML private CheckBox muteOnBgCheckBox;

    // UI 风格
    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox dynamicBgCheckBox;
    @FXML private ComboBox<String> fontComboBox;

    // 语言
    @FXML private ComboBox<String> languageComboBox;

    // 其他
    @FXML private Button exitBtn;
    @FXML private Button logoutBtn;

    // 左侧导航
    @FXML private Button menuBaseBtn;
    @FXML private Button menuGraphicsBtn;
    @FXML private Button menuAudioBtn;
    @FXML private Button menuStyleBtn;
    @FXML private Button menuServerBtn;
    @FXML private Button menuLanguageBtn;
    @FXML private Button menuOtherBtn;

    // 面板
    @FXML private VBox basePane;
    @FXML private VBox graphicsPane;
    @FXML private VBox audioPane;
    @FXML private VBox stylePane;
    @FXML private VBox serverPane;
    @FXML private VBox languagePane;
    @FXML private VBox otherPane;

    private User currentUser;
    private boolean previewMode;

    /** 分辨率字符串 → 宽高映射 */
    private static final Map<String, int[]> RESOLUTION_MAP = Map.of(
            "900x640 (默认)", new int[]{900, 640},
            "1024x768", new int[]{1024, 768},
            "1280x720", new int[]{1280, 720}
    );

    private static String resolutionToDisplay(int w, int h) {
        for (var entry : RESOLUTION_MAP.entrySet()) {
            if (entry.getValue()[0] == w && entry.getValue()[1] == h) {
                return entry.getKey();
            }
        }
        return "900x640 (默认)";
    }

    @FXML
    private void initialize() {
        UserSettings s = UserSettings.get();

        // 服务器配置
        String currentHost = AppConfig.SERVER_HOST;
        if (currentHost == null || currentHost.trim().isEmpty()) {
            currentHost = "127.0.0.1";
        }
        ipComboBox.setValue(currentHost);
        portField.setText(String.valueOf(AppConfig.SERVER_PORT));

        if (ClientMain.getConnector().isConnected()) {
            statusLabel.setText("已连接");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("未连接 (网络离线)");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }

        // 绑定远程新增的音频控件
        bindAudioControl(globalMusicCheckBox, globalVolumeSlider);
        bindAudioControl(backgroundMusicCheckBox, backgroundVolumeSlider);
        bindAudioControl(soundCheckBox, soundVolumeSlider);
        bindBackgroundMusicSettings();

        // 基础设置
        autoStartCheckBox.setSelected(s.autoStart);
        rememberPasswordCheckBox.setSelected(s.rememberPassword);

        // 图像设置
        Stage stage = ScreenManager.getStage();
        if (stage != null) {
            resolutionComboBox.setValue(resolutionToDisplay((int) stage.getWidth(), (int) stage.getHeight()));
            fullscreenCheckBox.setSelected(stage.isFullScreen());
        } else {
            resolutionComboBox.setValue("900x640 (默认)");
            fullscreenCheckBox.setSelected(s.fullscreen);
        }
        vsyncCheckBox.setSelected(s.vsync);
        fpsComboBox.setValue(s.fpsLimit.equals("0") ? "无限制" : s.fpsLimit + " FPS");

        // 声音设置（本地扩展）
        volumeSlider.setValue(s.volume);
        bgMusicCheckBox.setSelected(s.bgMusic);
        soundEffectCheckBox.setSelected(s.soundEffect);
        lobbyMusicCheckBox.setSelected(s.lobbyMusic);
        muteOnBgCheckBox.setSelected(s.muteOnBackground);

        // UI 风格
        themeComboBox.setValue(s.theme);
        dynamicBgCheckBox.setSelected(s.dynamicBackground);
        fontComboBox.setValue(s.fontFamily);

        // 语言
        languageComboBox.setValue(s.language);

        // 初始激活基础设置标签页
        showBaseTab();
    }

    /** 绑定远程新增的 ElasticVolumeSlider 与 CheckBox 联动 */
    private void bindAudioControl(CheckBox toggle, ElasticVolumeSlider slider) {
        slider.disableProperty().bind(toggle.selectedProperty().not());
        slider.opacityProperty().bind(Bindings.when(toggle.selectedProperty()).then(1.0).otherwise(0.48));
    }

    /** 绑定远程新增的 BackgroundMusicManager 音频系统 */
    private void bindBackgroundMusicSettings() {
        globalMusicCheckBox.setSelected(BackgroundMusicManager.globalMusicEnabledProperty().get());
        backgroundMusicCheckBox.setSelected(BackgroundMusicManager.backgroundMusicEnabledProperty().get());
        globalVolumeSlider.setValue(BackgroundMusicManager.globalVolumeProperty().get());
        backgroundVolumeSlider.setValue(BackgroundMusicManager.backgroundVolumeProperty().get());

        globalMusicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.globalMusicEnabledProperty().set(newVal));
        backgroundMusicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.backgroundMusicEnabledProperty().set(newVal));
        globalVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.globalVolumeProperty().set(newVal.doubleValue()));
        backgroundVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.backgroundVolumeProperty().set(newVal.doubleValue()));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            statusLabel.setText("已连接 (浏览模式)");
            statusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    // ========== 标签页切换 ==========

    private void switchTab(VBox activePane, Button activeBtn) {
        basePane.setVisible(false);        basePane.setManaged(false);
        graphicsPane.setVisible(false);    graphicsPane.setManaged(false);
        audioPane.setVisible(false);       audioPane.setManaged(false);
        stylePane.setVisible(false);       stylePane.setManaged(false);
        serverPane.setVisible(false);      serverPane.setManaged(false);
        languagePane.setVisible(false);    languagePane.setManaged(false);
        otherPane.setVisible(false);       otherPane.setManaged(false);

        activePane.setVisible(true);
        activePane.setManaged(true);

        menuBaseBtn.getStyleClass().remove("active");
        menuGraphicsBtn.getStyleClass().remove("active");
        menuAudioBtn.getStyleClass().remove("active");
        menuStyleBtn.getStyleClass().remove("active");
        menuServerBtn.getStyleClass().remove("active");
        menuLanguageBtn.getStyleClass().remove("active");
        menuOtherBtn.getStyleClass().remove("active");

        activeBtn.getStyleClass().add("active");
    }

    @FXML private void showBaseTab()     { switchTab(basePane, menuBaseBtn); }
    @FXML private void showGraphicsTab() { switchTab(graphicsPane, menuGraphicsBtn); }
    @FXML private void showAudioTab()    { switchTab(audioPane, menuAudioBtn); }
    @FXML private void showStyleTab()    { switchTab(stylePane, menuStyleBtn); }
    @FXML private void showServerTab()   { switchTab(serverPane, menuServerBtn); }
    @FXML private void showLanguageTab() { switchTab(languagePane, menuLanguageBtn); }
    @FXML private void showOtherTab()    { switchTab(otherPane, menuOtherBtn); }

    // ========== 基础设置 ==========

    @FXML
    private void handleAutoStart() {
        boolean enabled = autoStartCheckBox.isSelected();
        UserSettings.get().autoStart = enabled;
        UserSettings.save();

        // Windows 开机自启：在 Startup 文件夹创建/删除 VBS 启动脚本
        String startupDir = System.getenv("APPDATA");
        if (startupDir == null) return;
        java.io.File startupFolder = new java.io.File(startupDir,
                "Microsoft\\Windows\\Start Menu\\Programs\\Startup");
        java.io.File scriptFile = new java.io.File(startupFolder, "Gomatch.vbs");

        try {
            if (enabled) {
                String scriptPath = new java.io.File(System.getProperty("user.dir"))
                        .getAbsolutePath() + "\\scripts\\hln-client.ps1";
                String content = "Set objShell = CreateObject(\"WScript.Shell\")\r\n"
                        + "objShell.Run \"powershell -ExecutionPolicy Bypass -File \"\""
                        + scriptPath.replace("\"", "\"\"\"")
                        + "\"\"\", 0, False\r\n";
                try (java.io.FileWriter fw = new java.io.FileWriter(scriptFile)) {
                    fw.write(content);
                }
            } else {
                if (scriptFile.exists()) {
                    scriptFile.delete();
                }
            }
        } catch (Exception e) {
            showInfoAlert("设置开机自启失败：" + e.getMessage());
            autoStartCheckBox.setSelected(!enabled);
        }
    }

    @FXML
    private void handleRememberPassword() {
        UserSettings.get().rememberPassword = rememberPasswordCheckBox.isSelected();
        UserSettings.save();
    }

    // ========== 图像设置 ==========

    @FXML
    private void handleResolution() {
        String val = resolutionComboBox.getValue();
        if (val == null) return;
        int[] size = RESOLUTION_MAP.get(val);
        if (size == null) return;

        Stage stage = ScreenManager.getStage();
        if (stage != null) {
            stage.setFullScreen(false);
            fullscreenCheckBox.setSelected(false);
            stage.setWidth(size[0]);
            stage.setHeight(size[1]);
            stage.centerOnScreen();
        }

        UserSettings.get().resolution = size[0] + "x" + size[1];
        UserSettings.save();
    }

    @FXML
    private void handleFullscreen() {
        boolean fullscreen = fullscreenCheckBox.isSelected();
        Stage stage = ScreenManager.getStage();
        if (stage != null) {
            stage.setFullScreen(fullscreen);
        }
        UserSettings.get().fullscreen = fullscreen;
        UserSettings.save();
    }

    @FXML
    private void handleVSync() {
        UserSettings.get().vsync = vsyncCheckBox.isSelected();
        UserSettings.save();
    }

    @FXML
    private void handleFpsLimit() {
        String val = fpsComboBox.getValue();
        if (val == null) return;
        if (val.equals("无限制")) {
            UserSettings.get().fpsLimit = "0";
        } else {
            UserSettings.get().fpsLimit = val.replace(" FPS", "");
        }
        UserSettings.save();
    }

    // ========== 声音设置 ==========

    @FXML
    private void handleVolume() {
        UserSettings.get().volume = volumeSlider.getValue();
        UserSettings.save();
    }

    @FXML
    private void handleBgMusic() {
        UserSettings.get().bgMusic = bgMusicCheckBox.isSelected();
        UserSettings.save();
    }

    @FXML
    private void handleSoundEffect() {
        UserSettings.get().soundEffect = soundEffectCheckBox.isSelected();
        UserSettings.save();
    }

    @FXML
    private void handleLobbyMusic() {
        UserSettings.get().lobbyMusic = lobbyMusicCheckBox.isSelected();
        UserSettings.save();
    }

    @FXML
    private void handleMuteOnBackground() {
        UserSettings.get().muteOnBackground = muteOnBgCheckBox.isSelected();
        UserSettings.save();
    }

    // ========== UI 风格 ==========

    @FXML
    private void handleTheme() {
        String val = themeComboBox.getValue();
        if (val == null) return;
        if (val.contains("开发中")) {
            showInfoAlert("暗夜黑磨砂主题正在开发中，敬请期待！");
            themeComboBox.setValue("清透白磨砂 (默认)");
            return;
        }
        UserSettings.get().theme = val;
        UserSettings.save();
    }

    @FXML
    private void handleDynamicBackground() {
        boolean enabled = dynamicBgCheckBox.isSelected();
        UserSettings.get().dynamicBackground = enabled;
        UserSettings.save();

        // 实时切换当前场景的动态背景
        ShapeGridBackground bg = ScreenManager.getCurrentBackground();
        if (bg != null) {
            bg.setEnabled(enabled);
        }
    }

    @FXML
    private void handleFontFamily() {
        String val = fontComboBox.getValue();
        if (val == null) return;
        UserSettings.get().fontFamily = val;
        UserSettings.save();

        // 实时切换全局字体
        Stage stage = ScreenManager.getStage();
        if (stage != null && stage.getScene() != null) {
            if (val.contains("系统默认")) {
                stage.getScene().getRoot().setStyle("-fx-font-family: \"Segoe UI\", \"Microsoft YaHei\", sans-serif;");
            } else {
                stage.getScene().getRoot().setStyle("-fx-font-family: \"Maple Mono NF CN\", \"Maple-Mono-NF-CN\", \"Maple Mono\", sans-serif;");
            }
        }
    }

    // ========== 语言 ==========

    @FXML
    private void handleLanguage() {
        String val = languageComboBox.getValue();
        if (val == null) return;
        if (val.contains("English")) {
            showInfoAlert("英文界面正在开发中，敬请期待！");
            languageComboBox.setValue("简体中文 (Chinese)");
            return;
        }
        UserSettings.get().language = val;
        UserSettings.save();
    }

    // ========== 服务器配置 ==========

    @FXML
    private void handleSaveConfig() {
        String host = ipComboBox.getValue();
        if (host == null || host.trim().isEmpty()) {
            statusLabel.setText("错误：IP 地址不能为空");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }
        host = host.trim();

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("错误：端口须为 1-65535 的整数");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        AppConfig.SERVER_HOST = host;
        AppConfig.SERVER_PORT = port;
        ClientConfig.saveConfig(host, port);

        statusLabel.setText("连接中...");
        statusLabel.setStyle("-fx-text-fill: #64748b;");

        final String finalHost = host;
        final int finalPort = port;
        Thread connectThread = new Thread(() -> {
            ClientMain.getConnector().disconnect();
            try {
                ClientMain.getConnector().connect(finalHost, finalPort);
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("✅ 已连接 (" + finalHost + ":" + finalPort + ")");
                    statusLabel.setStyle("-fx-text-fill: green;");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ 连接失败：" + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #dc2626;");
                });
            }
        });
        connectThread.setDaemon(true);
        connectThread.start();
    }

    // ========== 其他 ==========

    @FXML
    private void handleLogout() {
        ClientMain.getConnector().disconnect();
        AppConfig.setCurrentUser(null);
        ScreenManager.switchTo("/fxml/LoginUI.fxml");
    }

    @FXML
    private void handleExitGame() {
        ConfirmExitController ctrl = ScreenManager.showDialog("/fxml/ConfirmExitUI.fxml");
        if (ctrl != null && ctrl.isConfirmed()) {
            ClientMain.getConnector().disconnect();
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    @FXML
    private void handleBack() {
        LobbyController ctrl = ScreenManager.switchTo("/fxml/LobbyUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    private void showInfoAlert(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        ScreenManager.showStyledAlert(alert);
    }
}
