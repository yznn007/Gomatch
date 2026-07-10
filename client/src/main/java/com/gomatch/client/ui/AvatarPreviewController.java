package com.gomatch.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * 头像大图预览弹窗。点击任意位置关闭。
 */
public class AvatarPreviewController {

    @FXML private StackPane avatarPane;
    @FXML private Circle avatarBg;
    @FXML private Label avatarLabel;
    @FXML private ImageView avatarImg;
    @FXML private Label usernameLabel;

    /**
     * 与 LobbyController.gradientFor / RankingController.gradientFor 完全一致的渐变调色板。
     * 必须复用同一份 hash→颜色映射，否则小头像和大头像颜色会不一致。
     */
    private static final String[] AVATAR_GRADIENTS = {
        "linear-gradient(from 0% 0% to 100% 100%, #60a5fa, #1e293b)",
        "linear-gradient(from 0% 0% to 100% 100%, #fbbf24, #b45309)",
        "linear-gradient(from 0% 0% to 100% 100%, #34d399, #065f46)",
        "linear-gradient(from 0% 0% to 100% 100%, #f472b6, #9d174d)",
        "linear-gradient(from 0% 0% to 100% 100%, #a78bfa, #5b21b6)",
        "linear-gradient(from 0% 0% to 100% 100%, #22d3ee, #155e75)",
        "linear-gradient(from 0% 0% to 100% 100%, #f87171, #991b1b)"
    };

    /**
     * 设置要预览的头像。
     *
     * @param image    头像图片（已加载）。传入 null 时强制使用首字母 fallback。
     * @param username 用户名（用于首字母回退和标题显示）
     */
    public void setAvatar(Image image, String username) {
        // 背景渐变：根据用户名 hash 选一个稳定渐变，与 LobbyController / RankingController 风格完全一致
        if (avatarPane != null) {
            avatarPane.setStyle("-fx-background-color: " + gradientFor(username) + ";"
                    + "-fx-background-radius: 150;");
        }
        // 兼容旧 FXML：保留 Circle 兜底（设为透明），不影响渲染
        if (avatarBg != null) {
            avatarBg.setFill(Color.TRANSPARENT);
        }

        // 首字母 fallback：默认先显示，等图片确认加载成功后再让位给图片
        final String initial = (username != null && !username.isEmpty())
                ? username.substring(0, 1).toUpperCase() : "U";
        avatarLabel.setText(initial);
        avatarLabel.setVisible(true);
        avatarLabel.setManaged(true);

        if (image == null) {
            // 没有头像 URL，直接用首字母
            avatarImg.setImage(null);
            avatarImg.setVisible(false);
            avatarImg.setManaged(false);
        } else {
            // 有头像：先让首字母占位（避免用户上传失败时一片白板），
            // 监听加载进度，**真正加载完成**后再切换到图片，失败则保留首字母。
            avatarImg.setImage(image);
            avatarImg.setVisible(false);
            avatarImg.setManaged(false);
            final Image imgRef = image;
            imgRef.progressProperty().addListener((obs, o, n) -> {
                if (n != null && n.doubleValue() >= 1.0 && !imgRef.isError()) {
                    javafx.application.Platform.runLater(() -> {
                        if (imgRef.isError()) return; // 二次检查：避免在错误回调后覆盖
                        avatarImg.setVisible(true);
                        avatarImg.setManaged(true);
                        avatarLabel.setVisible(false);
                        avatarLabel.setManaged(false);
                    });
                }
            });
            imgRef.errorProperty().addListener((obs, o, n) -> {
                if (n) {
                    // 图片加载失败 → 恢复首字母 fallback
                    javafx.application.Platform.runLater(() -> {
                        avatarImg.setVisible(false);
                        avatarImg.setManaged(false);
                        avatarLabel.setText(initial);
                        avatarLabel.setVisible(true);
                        avatarLabel.setManaged(true);
                    });
                }
            });
        }
        if (username != null) {
            usernameLabel.setText(username);
        }
    }

    private String gradientFor(String username) {
        if (username == null || username.isEmpty()) return AVATAR_GRADIENTS[0];
        return AVATAR_GRADIENTS[Math.abs(username.hashCode()) % AVATAR_GRADIENTS.length];
    }

    @FXML
    private void handleClose() {
        if (avatarPane == null || avatarPane.getScene() == null) return;
        Stage stage = (Stage) avatarPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
