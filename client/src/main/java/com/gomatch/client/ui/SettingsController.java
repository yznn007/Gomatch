package com.gomatch.client.ui;

import com.gomatch.client.AppConfig;
import com.gomatch.client.audio.BackgroundMusicManager;
import com.gomatch.client.audio.PlayOrder;
import com.gomatch.client.ClientMain;
import com.gomatch.client.I18N;
import com.gomatch.client.UserSettings;
import com.gomatch.client.model.User;
import com.gomatch.client.network.ClientConfig;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
    @FXML private CheckBox fullscreenCheckBox;
    @FXML private CheckBox vsyncCheckBox;
    @FXML private ComboBox<String> fpsComboBox;

    // 声音设置（远程新增的 ElasticVolumeSlider 控件）
    @FXML private CheckBox globalMusicCheckBox;
    @FXML private ElasticVolumeSlider globalVolumeSlider;
    @FXML private CheckBox backgroundMusicCheckBox;
    @FXML private ElasticVolumeSlider backgroundVolumeSlider;
    @FXML private CheckBox soundCheckBox;
    @FXML private ElasticVolumeSlider soundVolumeSlider;
    @FXML private ComboBox<String> lobbySongComboBox;
    @FXML private Button playOrderBtn;

    // 后台静音
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
    private boolean syncingSongComboBox;
    private boolean initializing;
    private boolean audioConfigSyncSuppressed;

    /** 填充所有 ComboBox 的可选项（i18n） */
    private void populateComboBoxItems() {
        // 帧率
        fpsComboBox.getItems().setAll(
                I18N.get("settings.fps.60"),
                I18N.get("settings.fps.120"),
                I18N.get("settings.fps.unlimited"));

        // 主题
        themeComboBox.getItems().setAll(I18N.get("settings.theme.default"), I18N.get("settings.theme.dark"));

        // 字体
        fontComboBox.getItems().setAll(
                I18N.get("settings.font.maple"),
                I18N.get("settings.font.system"));

        // 大厅歌曲由 populateSongComboBox() 动态填充，此处跳过

        // 语言
        languageComboBox.getItems().setAll(
                I18N.get("settings.language.zh"),
                I18N.get("settings.language.en"));

        // IP
        ipComboBox.getItems().setAll("127.0.0.1");
    }

    @FXML
    private void initialize() {
        initializing = true;
        UserSettings s = UserSettings.get();

        // 先填充所有 ComboBox 可选项（i18n）
        populateComboBoxItems();

        // 服务器配置
        String currentHost = AppConfig.SERVER_HOST;
        if (ClientConfig.getDefaultHost().equals(currentHost)) {
            ipComboBox.getEditor().clear();
        } else {
            ipComboBox.setValue(currentHost);
        }
        portField.setText(String.valueOf(AppConfig.SERVER_PORT));

        if (ClientMain.getConnector().isConnected()) {
            statusLabel.setText(I18N.get("settings.connected"));
            statusLabel.setStyle("-fx-text-fill: #86efac;");
        } else {
            statusLabel.setText(I18N.get("settings.disconnected"));
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }

        // 绑定远程新增的音频控件到 BackgroundMusicManager
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
            fullscreenCheckBox.setSelected(stage.isFullScreen());
        } else {
            fullscreenCheckBox.setSelected(s.fullscreen);
        }
        vsyncCheckBox.setSelected(s.vsync);
        fpsComboBox.setValue(s.fpsLimit.equals("0") ? I18N.get("settings.fps.unlimited") : s.fpsLimit + " FPS");

        // 后台静音
        muteOnBgCheckBox.setSelected(s.muteOnBackground);

        // UI 风格
        // 根据保存的 fontFamily key 设置当前显示值
        if (s.fontFamily != null && s.fontFamily.contains("系统")) {
            fontComboBox.setValue(I18N.get("settings.font.system"));
        } else {
            fontComboBox.setValue(I18N.get("settings.font.maple"));
        }
        // 根据保存的 theme key 设置当前显示值
        themeComboBox.setValue(ScreenManager.isDarkTheme() ? I18N.get("settings.theme.dark") : I18N.get("settings.theme.default"));
        dynamicBgCheckBox.setSelected(s.dynamicBackground);

        // 语言
        if (s.language != null && s.language.contains("English")) {
            languageComboBox.setValue(I18N.get("settings.language.en"));
        } else {
            languageComboBox.setValue(I18N.get("settings.language.zh"));
        }

        // 大厅歌曲选中项由 populateSongComboBox() 设置

        // 初始激活基础设置标签页
        showBaseTab();
        initializing = false;
    }

    /** 绑定 ElasticVolumeSlider 与 CheckBox 联动（禁用/半透明） */
    private void bindAudioControl(CheckBox toggle, ElasticVolumeSlider slider) {
        slider.disableProperty().bind(toggle.selectedProperty().not());
        slider.opacityProperty().bind(Bindings.when(toggle.selectedProperty()).then(1.0).otherwise(0.48));
    }

    /** 绑定 BackgroundMusicManager 音频系统，实时控制音乐播放 */
    private void bindBackgroundMusicSettings() {
        globalMusicCheckBox.setSelected(BackgroundMusicManager.globalMusicEnabledProperty().get());
        backgroundMusicCheckBox.setSelected(BackgroundMusicManager.backgroundMusicEnabledProperty().get());
        globalVolumeSlider.setValue(BackgroundMusicManager.globalVolumeProperty().get());
        backgroundVolumeSlider.setValue(BackgroundMusicManager.backgroundVolumeProperty().get());

        // 双向绑定：UI ↔ BackgroundMusicManager
        globalMusicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.globalMusicEnabledProperty().set(newVal));
        backgroundMusicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.backgroundMusicEnabledProperty().set(newVal));
        globalMusicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!initializing && !audioConfigSyncSuppressed) {
                UserSettings.get().lobbyMusic = newVal;
                UserSettings.save();
            }
        });
        backgroundMusicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!initializing && !audioConfigSyncSuppressed) {
                UserSettings.get().bgMusic = newVal;
                UserSettings.save();
            }
        });
        soundCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!initializing && !audioConfigSyncSuppressed) {
                UserSettings.get().soundEffect = newVal;
                UserSettings.save();
            }
        });
        globalVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.globalVolumeProperty().set(newVal.doubleValue()));
        backgroundVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                BackgroundMusicManager.backgroundVolumeProperty().set(newVal.doubleValue()));

        populateSongComboBox();
        setupPlayOrderButton();
    }

    /** 滑块拖动期间只更新本地音频；松开鼠标时再落盘并发送一次 Socket 请求。 */
    @FXML
    private void handleAudioVolumeReleased() {
        if (initializing) return;
        UserSettings settings = UserSettings.get();
        settings.volume = globalVolumeSlider.getValue();
        settings.bgMusic = backgroundMusicCheckBox.isSelected();
        settings.soundEffect = soundCheckBox.isSelected();
        UserSettings.save();
    }

    private void populateSongComboBox() {
        Map<String, String> songs = BackgroundMusicManager.getAvailableSongs();
        ObservableList<String> items = FXCollections.observableArrayList(songs.values());
        lobbySongComboBox.setItems(items);

        String currentName = BackgroundMusicManager.getCurrentSongName();
        if (currentName != null && !currentName.isEmpty()) {
            lobbySongComboBox.setValue(currentName);
        } else if (!items.isEmpty()) {
            lobbySongComboBox.setValue(items.get(0));
        }

        lobbySongComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.equals(oldVal) || syncingSongComboBox) {
                return;
            }
            String path = songs.entrySet().stream()
                    .filter(e -> e.getValue().equals(newVal))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (path != null) {
                BackgroundMusicManager.selectAndPlaySong(path);
            }
        });

        BackgroundMusicManager.currentSongNameProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.equals(lobbySongComboBox.getValue())) {
                return;
            }
            syncingSongComboBox = true;
            lobbySongComboBox.setValue(newVal);
            syncingSongComboBox = false;
        });
    }

    private void setupPlayOrderButton() {
        updatePlayOrderButtonText();
        BackgroundMusicManager.currentPlayOrderProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updatePlayOrderButtonText();
            }
        });
    }

    private void updatePlayOrderButtonText() {
        PlayOrder current = BackgroundMusicManager.getCurrentPlayOrder();
        playOrderBtn.setText(current.getDisplayName());
    }

    @FXML
    private void handlePlayOrderCycle() {
        PlayOrder current = BackgroundMusicManager.getCurrentPlayOrder();
        PlayOrder[] orders = PlayOrder.values();
        PlayOrder next = orders[(current.ordinal() + 1) % orders.length];
        BackgroundMusicManager.setPlayOrder(next);
        updatePlayOrderButtonText();
    }


    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            statusLabel.setText(I18N.get("settings.connected") + " (" + I18N.get("settings.previewMode") + ")");
            statusLabel.setStyle("-fx-text-fill: #86efac;");
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
            showInfoAlert(I18N.get("settings.autoStartFail", e.getMessage()));
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
        String unlimited = I18N.get("settings.fps.unlimited");
        if (val.equals(unlimited)) {
            UserSettings.get().fpsLimit = "0";
        } else {
            UserSettings.get().fpsLimit = val.replace(" FPS", "");
        }
        UserSettings.save();
    }

    // ========== 声音设置 ==========

    @FXML
    private void handleMuteOnBackground() {
        boolean enabled = muteOnBgCheckBox.isSelected();
        UserSettings.get().muteOnBackground = enabled;
        UserSettings.save();

        if (!enabled) {
            BackgroundMusicManager.setMutedByBackground(false);
        } else {
            Stage stage = ScreenManager.getStage();
            if (stage != null && !stage.isFocused()) {
                BackgroundMusicManager.setMutedByBackground(true);
            }
        }
    }

    // ========== UI 风格 ==========

    @FXML
    private void handleTheme() {
        if (initializing) return;
        String val = themeComboBox.getValue();
        if (val == null) return;
        UserSettings.get().theme = val;
        UserSettings.save();

        Stage stage = ScreenManager.getStage();
        if (stage != null && stage.getScene() != null) {
            ScreenManager.applyUserTheme(stage.getScene());
        }
    }

    @FXML
    private void handleDynamicBackground() {
        boolean enabled = dynamicBgCheckBox.isSelected();
        UserSettings.get().dynamicBackground = enabled;
        UserSettings.save();

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

        Stage stage = ScreenManager.getStage();
        if (stage != null && stage.getScene() != null) {
            ScreenManager.applyUserFont(stage.getScene());
        }
    }

    // ========== 语言 ==========

    @FXML
    private void handleLanguage() {
        String val = languageComboBox.getValue();
        if (val == null) return;
        UserSettings.get().language = val;
        UserSettings.save();
        // 切换语言并重载当前场景
        I18N.init(val);
        SettingsController ctrl = ScreenManager.switchTo("/fxml/SettingsUI.fxml");
        ctrl.setCurrentUser(currentUser);
        ctrl.setPreviewMode(previewMode);
    }

    // ========== 服务器配置 ==========

    @FXML
    private void handleSaveConfig() {
        String enteredHost = ipComboBox.getEditor().getText();
        String host = enteredHost == null || enteredHost.trim().isEmpty()
                ? ClientConfig.getDefaultHost()
                : enteredHost.trim();

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            statusLabel.setText(I18N.get("settings.portInvalid"));
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        AppConfig.SERVER_HOST = host;
        AppConfig.SERVER_PORT = port;
        ClientConfig.saveConfig(enteredHost, port);

        statusLabel.setText(I18N.get("settings.connecting"));
        statusLabel.setStyle("-fx-text-fill: " + ScreenManager.themeColor("#64748b", "#a7b0c2") + ";");

        final String finalHost = host;
        final int finalPort = port;
        Thread connectThread = new Thread(() -> {
            ClientMain.getConnector().disconnect();
            try {
                ClientMain.getConnector().connect(finalHost, finalPort);
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(I18N.get("settings.connectedWith", finalHost, finalPort));
                    statusLabel.setStyle("-fx-text-fill: #86efac;");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(I18N.get("settings.connectFailed", e.getMessage()));
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
