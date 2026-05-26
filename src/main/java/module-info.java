module com.example.piano {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens com.example.piano to javafx.fxml;
    exports com.example.piano;
}