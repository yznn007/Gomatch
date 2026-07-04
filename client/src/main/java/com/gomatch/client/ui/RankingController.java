package com.gomatch.client.ui;

import com.gomatch.client.model.User;
import com.gomatch.client.network.NetworkListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.List;

public class RankingController implements NetworkListener {
    @FXML private TableView<User> rankingTable;
    @FXML private TableColumn<User, Integer> rankCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, Integer> scoreCol;
    @FXML private TableColumn<User, Integer> winCol;
    @FXML private TableColumn<User, Integer> loseCol;
    @FXML private TableColumn<User, String> winRateCol;

    private final ObservableList<User> rankingData = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    @FXML
    private void initialize() {
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        winCol.setCellValueFactory(new PropertyValueFactory<>("winCount"));
        loseCol.setCellValueFactory(new PropertyValueFactory<>("loseCount"));
        winRateCol.setCellValueFactory(new PropertyValueFactory<>("winRate"));

        rankingTable.setItems(rankingData);
        com.gomatch.client.ClientMain.getConnector().addListener(this);
        if (com.gomatch.client.ClientMain.getConnector().isConnected()) {
            com.gomatch.client.ClientMain.getConnector().send("RANKING");
        } else {
            String[][] fakeData = {
                {"1", "棋圣", "1500", "50", "10", "83.3%"},
                {"2", "高手如云", "1350", "40", "15", "72.7%"},
                {"3", "五子连珠", "1200", "35", "20", "63.6%"},
                {"4", "新手求带", "1050", "20", "25", "44.4%"},
                {"5", "游客", "1000", "0", "0", "0%"}
            };
            for (String[] row : fakeData) {
                User u = new User();
                u.setRank(Integer.parseInt(row[0]));
                u.setUsername(row[1]);
                u.setScore(Integer.parseInt(row[2]));
                u.setWinCount(Integer.parseInt(row[3]));
                u.setLoseCount(Integer.parseInt(row[4]));
                u.setWinRate(row[5]);
                rankingData.add(u);
            }
        }
    }

    @FXML
    private void handleBack() {
        ScreenManager.switchTo("/fxml/LobbyUI.fxml");
    }

    @Override
    public void onMessage(String command, String[] args) {
        if ("RANKING_LIST".equals(command)) {
            Type listType = new TypeToken<List<User>>() {}.getType();
            List<User> list = gson.fromJson(args[0], listType);
            DecimalFormat df = new DecimalFormat("0.0%");
            for (int i = 0; i < list.size(); i++) {
                User u = list.get(i);
                u.setRank(i + 1);
                int total = u.getWinCount() + u.getLoseCount();
                u.setWinRate(total > 0 ? df.format((double) u.getWinCount() / total) : "0%");
            }
            rankingData.setAll(list);
        }
    }
}
