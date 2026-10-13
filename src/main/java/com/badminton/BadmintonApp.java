package com.badminton;

import com.badminton.repository.FileClubRepository;
import com.badminton.service.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;

/**
 * แอปพลิเคชัน JavaFX: เตรียมโฟลเดอร์ข้อมูล สร้าง ClubService และสลับหน้าจอเข้าสู่ระบบ/หน้าหลัก
 */
public final class BadmintonApp extends Application {
    // เก็บเป็น static เพื่อให้ Controller ที่ FXMLLoader สร้างขึ้นเองเรียกใช้ service และหน้าต่างเดียวกันได้
    private static ClubService service;
    private static Stage window;
    // ล็อกไฟล์ข้อมูล กันเปิดโปรแกรมหลายหน้าต่างแล้วบันทึกทับกัน
    private FileChannel lockChannel;
    private FileLock lock;
    public static ClubService service() { return service; }
    public static Stage window() { return window; }
    @Override public void start(Stage stage) {
        window = stage;
        try {
            // โฟลเดอร์ข้อมูลเริ่มต้นคือ ./data เปลี่ยนได้ด้วย -Dbadminton.data.dir=...
            Path directory = Path.of(System.getProperty("badminton.data.dir", "data")).toAbsolutePath();
            Files.createDirectories(directory);
            lockChannel = FileChannel.open(directory.resolve(".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            // tryLock คืน null ถ้ามีโปรแกรมอื่นถือล็อกอยู่แล้ว
            lock = lockChannel.tryLock();
            if (lock == null) throw new IOException("มีโปรแกรมเปิดใช้งานข้อมูลชุดนี้อยู่แล้ว กรุณาปิดหน้าต่างเดิมก่อน");
            // ประกอบ service จากส่วนประกอบจริง: เก็บไฟล์, ราคามาตรฐาน, นาฬิกาเวลาประเทศไทย
            service = new ClubService(new FileClubRepository(directory), new StandardPricingPolicy(), directory, Clock.system(ZoneId.of("Asia/Bangkok")));
            stage.setTitle("Badminton Club • ระบบจองสนามแบดมินตัน");
            showLogin();
            stage.show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "เปิดโปรแกรมไม่สำเร็จ\n" + ex.getMessage());
            alert.setHeaderText("กรุณาตรวจสอบแฟ้มข้อมูล"); alert.showAndWait();
            javafx.application.Platform.exit();
        }
    }
    /** แสดงหน้าเข้าสู่ระบบ */
    public static void showLogin() throws IOException { showView("login.fxml", 1040, 710); }
    /** แสดงหน้าหลัก (เรียกหลังเข้าสู่ระบบสำเร็จ) */
    public static void showMain() throws IOException { showView("main.fxml", 1360, 900); }
    /** โหลดไฟล์ FXML พร้อม theme.css แล้วกำหนดขนาดขั้นต่ำของหน้าต่าง */
    private static void showView(String name, int width, int height) throws IOException {
        Scene scene = new Scene(FXMLLoader.load(BadmintonApp.class.getResource("/com/badminton/view/" + name)), width, height);
        scene.getStylesheets().add(BadmintonApp.class.getResource("/com/badminton/view/theme.css").toExternalForm());
        window.setScene(scene); window.setMinWidth(name.equals("main.fxml") ? 1120 : 860);
        window.setMinHeight(name.equals("main.fxml") ? 760 : 650);
        window.centerOnScreen();
    }
    /** ปล่อยล็อกไฟล์ข้อมูลเมื่อปิดโปรแกรม */
    @Override public void stop() throws IOException {
        if (lock != null) lock.release();
        if (lockChannel != null) lockChannel.close();
    }
    public static void main(String[] args) { launch(args); }
}
