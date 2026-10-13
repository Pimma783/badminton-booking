package com.badminton.controller;

import com.badminton.BadmintonApp;
import com.badminton.model.*;
import com.badminton.repository.FileClubRepository;
import com.badminton.service.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ทดสอบหน้าจอจริง: โหลดไฟล์ FXML ทดลองกดปุ่ม ตรวจสิทธิ์ตามบทบาท และบันทึกภาพหน้าจอทุกแท็บ
 * ไว้ที่ target/ui-preview/ (ต้องรันบนเครื่องที่เปิดหน้าจอกราฟิกได้)
 */
class FxmlViewTest {
    @TempDir Path directory;
    // เริ่มระบบ JavaFX ครั้งเดียวก่อนทุกเทสต์
    @BeforeAll static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(20, TimeUnit.SECONDS));
    }
    @AfterAll static void stopToolkit() { Platform.exit(); }
    @Test void viewsLoadActionsWorkAndScreenshotsRender() throws Exception {
        ClubService service = new ClubService(new FileClubRepository(directory), new StandardPricingPolicy(), directory,
                Clock.fixed(Instant.parse("2026-10-12T01:00:00Z"), ZoneId.of("Asia/Bangkok")));
        // ใส่ service ทดสอบเข้าไปแทน service จริง (ฟิลด์เป็น private static จึงต้องใช้ reflection)
        var serviceField = BadmintonApp.class.getDeclaredField("service"); serviceField.setAccessible(true); serviceField.set(null, service);
        Path slip = directory.resolve("receipt.png");
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", slip.toFile());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        // งานที่แตะหน้าจอต้องรันบน JavaFX Application Thread
        Platform.runLater(() -> {
            Stage stage = new Stage();
            try {
                // 1) หน้าเข้าสู่ระบบโหลดได้
                Parent login = FXMLLoader.load(BadmintonApp.class.getResource("/com/badminton/view/login.fxml"));
                show(stage, login, 1040, 710); snapshot(login, "login");
                // 2) admin เห็นครบ 7 แท็บ
                service.signIn("admin", "admin123");
                FXMLLoader loader = new FXMLLoader(BadmintonApp.class.getResource("/com/badminton/view/main.fxml"));
                Parent root = loader.load(); show(stage, root, 1360, 900);
                TabPane tabs = (TabPane) root.lookup("#tabs"); assertEquals(7, tabs.getTabs().size());
                tabs.getSelectionModel().select(1); root.applyCss(); root.layout();
                // 3) กรอกฟอร์มและกดปุ่มสมัครสมาชิกจริง
                ((TextField) root.lookup("#memberName")).setText("ณัฐวุฒิ ใจดี");
                ((TextField) root.lookup("#memberPhone")).setText("0812345678");
                Button register = root.lookupAll(".button").stream().filter(n -> n instanceof Button b && b.getText().contains("สมัครสมาชิก")).map(n -> (Button)n).findFirst().orElseThrow();
                register.fire(); assertEquals(1, service.members().size());
                String memberId = service.members().get(0).id();
                // 4) สร้างข้อมูลตัวอย่างทุกประเภท แล้วสั่งรีเฟรชหน้าจอ
                service.buyPackage(memberId, slip);
                service.book(memberId, 2, service.today().plusDays(1), 14, 2, PaymentMode.CASH, slip);
                service.rent(memberId, RentalType.SHOES, 1, service.today());
                service.sell(memberId, "P003", 2);
                Object controller = loader.getController();
                var refresh = MainController.class.getDeclaredMethod("refreshAll"); refresh.setAccessible(true); refresh.invoke(controller);
                // 5) บันทึกการจองผ่านปุ่มในหน้าจอ (เรียกเมธอด private ด้วย reflection)
                tabs.getSelectionModel().selectFirst(); root.applyCss(); root.layout();
                ((DatePicker)root.lookup("#bookingDate")).setValue(service.today().plusDays(1));
                var path = MainController.class.getDeclaredField("bookingSlip"); path.setAccessible(true); path.set(controller, slip);
                var save = MainController.class.getDeclaredMethod("saveBooking"); save.setAccessible(true); save.invoke(controller);
                assertEquals(2, service.bookings().size()); assertEquals(3, service.member(memberId).bookedHours());
                // 6) บันทึกภาพหน้าจอทุกแท็บ
                String[] names = {"booking", "members", "packages", "shop", "history", "revenue", "holidays"};
                for (int i = 0; i < names.length; i++) {
                    tabs.getSelectionModel().select(i); root.applyCss(); root.layout(); snapshot(root, names[i]);
                }
                // 7) แคชเชียร์ต้องเห็นแค่ 4 แท็บ (ไม่มีประวัติ รายได้ วันหยุด)
                service.signIn("cashier", "cashier123");
                Parent cashier = FXMLLoader.load(BadmintonApp.class.getResource("/com/badminton/view/main.fxml"));
                show(stage, cashier, 1360, 900);
                assertEquals(4, ((TabPane)cashier.lookup("#tabs")).getTabs().size());
            } catch (Throwable ex) { failure.set(ex); }
            finally { stage.close(); done.countDown(); }
        });
        assertTrue(done.await(60, TimeUnit.SECONDS), "UI test timed out");
        if (failure.get() != null) throw new AssertionError("FXML UI verification failed", failure.get());
    }
    /** แสดง root บนหน้าต่างทดสอบแล้วจัด layout ให้เสร็จ */
    private static void show(Stage stage, Parent root, int width, int height) {
        Scene scene = new Scene(root, width, height);
        stage.setScene(scene); stage.show(); root.applyCss(); root.layout();
    }
    /** ถ่ายภาพหน้าจอแล้วบันทึกเป็น PNG ที่ target/ui-preview/ */
    private static void snapshot(Parent root, String name) throws Exception {
        WritableImage image = root.snapshot(null, null);
        BufferedImage png = new BufferedImage((int)image.getWidth(), (int)image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var reader = image.getPixelReader();
        for (int y = 0; y < png.getHeight(); y++) for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, reader.getArgb(x, y));
        Path output = Path.of("target", "ui-preview", name + ".png"); Files.createDirectories(output.getParent());
        ImageIO.write(png, "png", output.toFile());
    }
}
