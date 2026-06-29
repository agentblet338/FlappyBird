module com.example.piano {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens com.flappy to javafx.fxml;
    exports com.flappy;
}