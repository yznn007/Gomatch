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
     * @param image    头像图片（已加载）
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

        if (image != null && !image.isError()) {
            avatarImg.setImage(image);
            avatarImg.setVisible(true);
            avatarImg.setManaged(true);
            avatarLabel.setVisible(false);
            avatarLabel.setManaged(false);
        } else {
            // 无头像时显示首字母
            avatarImg.setVisible(false);
            avatarImg.setManaged(false);
            avatarLabel.setVisible(true);
            avatarLabel.setManaged(true);
            String initial = (username != null && !username.isEmpty())
                    ? username.substring(0, 1).toUpperCase() : "U";
            avatarLabel.setText(initial);
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
