package com.badminton.controller;

import com.badminton.BadmintonApp;
import com.badminton.model.*;
import com.badminton.service.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import java.awt.Desktop;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * Controller ของหน้าหลัก (main.fxml) — รับเหตุการณ์จากหน้าจอแล้วส่งต่อให้ ClubService
 * <p>
 * คลาสนี้ไม่มีกติกาธุรกิจเอง ทำหน้าที่แค่อ่านค่าจากฟอร์ม เรียก service และแสดงผล (Separation of concerns)
 * ตัวแปร @FXML ทุกตัวต้องมีชื่อตรงกับ fx:id ในไฟล์ main.fxml
 */
public final class MainController {
    private ClubService service;
    // สลิปที่เลือกไว้แต่ยังไม่ได้บันทึก (ฟอร์มจอง และฟอร์มซื้อแพ็กเกจ)
    private Path bookingSlip, packageSlip;
    // รายการรับเงินในรายงานที่แสดงอยู่ ใช้ส่งออก CSV
    private List<Payment> reportRows = List.of();
    // true ระหว่างโหลดข้อมูลใหม่ทั้งหน้า เพื่อไม่ให้ listener ทำงานซ้ำทุกครั้งที่ ComboBox เปลี่ยนค่า
    private boolean refreshing;
    // แสดงวันที่แบบ ค.ศ. เช่น 14/10/2026
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // แถบหัว สถิติ แท็บ และแถบสถานะด้านล่าง
    @FXML private Label staffLabel, memberCountLabel, todayCountLabel, statusLabel;
    @FXML private TabPane tabs;
    @FXML private Tab historyTab, reportTab, holidaysTab;
    // แท็บจองสนาม
    @FXML private ComboBox<Member> bookingMember, packageMember, storeMember, historyMember;
    @FXML private ComboBox<Integer> courtCombo, startCombo, durationCombo;
    @FXML private ComboBox<PaymentMode> paymentCombo;
    @FXML private DatePicker bookingDate, searchDate;
    @FXML private Label hoursLabel, memberBalanceLabel, quoteLabel, bookingSlipLabel;
    @FXML private TilePane courtGrid;
    @FXML private TextField bookingSearch;
    @FXML private CheckBox allDates;
    @FXML private TableView<Booking> bookingTable, historyTable;
    // แท็บสมาชิก
    @FXML private TextField memberName, memberPhone, memberSearch;
    @FXML private TableView<Member> memberTable;
    // แท็บแพ็กเกจ
    @FXML private Label packageBalanceLabel, packageSlipLabel;
    // แท็บเช่าและขายอุปกรณ์
    @FXML private ComboBox<Product> productCombo;
    @FXML private ComboBox<RentalType> rentalCombo;
    @FXML private Spinner<Integer> productQuantity, rentalQuantity;
    @FXML private DatePicker rentalDate;
    @FXML private Label productTotalLabel, rentalTotalLabel;
    @FXML private TableView<Rental> rentalTable;
    // แท็บรายได้ (เฉพาะผู้ดูแลระบบ)
    @FXML private ComboBox<String> reportMode;
    @FXML private DatePicker reportDate;
    @FXML private Label reportRangeLabel, reportTotalLabel, courtRevenueLabel, packageRevenueLabel, rentalRevenueLabel, productRevenueLabel;
    @FXML private TableView<Payment> reportTable;
    // แท็บวันหยุด (เฉพาะผู้ดูแลระบบ)
    @FXML private DatePicker holidayDate;
    @FXML private ListView<LocalDate> holidayList;

    /** FXMLLoader เรียกเมธอดนี้อัตโนมัติหลังผูกตัวแปร @FXML ครบแล้ว ใช้ตั้งค่าเริ่มต้นของหน้าจอ */
    @FXML public void initialize() {
        service = BadmintonApp.service();
        staffLabel.setText(service.currentUser().getDisplayName() + "  /  " + service.currentUser().getRole());
        // แคชเชียร์ไม่เห็นแท็บประวัติ รายได้ และวันหยุด (service ก็ตรวจสิทธิ์ซ้ำอีกชั้น)
        if (!service.currentUser().canViewReports()) tabs.getTabs().removeAll(historyTab, reportTab, holidaysTab);
        // ตัวเลือกสนาม 1-12 และจำนวนชั่วโมง 1-10
        courtCombo.setItems(FXCollections.observableArrayList(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList()));
        durationCombo.setItems(FXCollections.observableArrayList(java.util.stream.IntStream.rangeClosed(1, 10).boxed().toList()));
        paymentCombo.setItems(FXCollections.observableArrayList(PaymentMode.values()));
        courtCombo.setValue(1); durationCombo.setValue(1); paymentCombo.setValue(PaymentMode.CASH);
        bookingDate.setValue(service.today()); searchDate.setValue(service.today());
        rentalDate.setValue(service.today()); reportDate.setValue(service.today()); holidayDate.setValue(service.today());
        // ให้ DatePicker ทุกตัวแสดงและรับวันที่รูปแบบ dd/MM/yyyy
        for (DatePicker picker : List.of(bookingDate, searchDate, rentalDate, reportDate, holidayDate)) {
            picker.setConverter(new StringConverter<>() {
                @Override public String toString(LocalDate date) { return date == null ? "" : dateFormat.format(date); }
                @Override public LocalDate fromString(String text) { return LocalDate.parse(text, dateFormat); }
            });
        }
        reportMode.setItems(FXCollections.observableArrayList("รายวัน", "รายเดือน")); reportMode.setValue("รายวัน");
        rentalCombo.setItems(FXCollections.observableArrayList(RentalType.values())); rentalCombo.getSelectionModel().selectFirst();
        productQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        rentalQuantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        setupTables();
        // listener: เมื่อผู้ใช้เปลี่ยนค่าในฟอร์ม ให้คำนวณราคา สนามว่าง หรือผลค้นหาใหม่ทันที
        bookingMember.valueProperty().addListener((o, a, b) -> updateQuote());
        packageMember.valueProperty().addListener((o, a, b) -> updatePackageBalance());
        bookingDate.valueProperty().addListener((o, a, b) -> updateHours());
        startCombo.valueProperty().addListener((o, a, b) -> updateAvailability());
        durationCombo.valueProperty().addListener((o, a, b) -> { updateAvailability(); updateQuote(); });
        paymentCombo.valueProperty().addListener((o, a, b) -> updateQuote());
        bookingSearch.textProperty().addListener((o, a, b) -> filterBookings());
        searchDate.valueProperty().addListener((o, a, b) -> filterBookings());
        allDates.selectedProperty().addListener((o, a, b) -> { searchDate.setDisable(b); filterBookings(); });
        memberSearch.textProperty().addListener((o, a, b) -> filterMembers());
        historyMember.valueProperty().addListener((o, a, b) -> loadHistory());
        productCombo.valueProperty().addListener((o, a, b) -> updateStoreTotals());
        productQuantity.valueProperty().addListener((o, a, b) -> updateStoreTotals());
        rentalQuantity.valueProperty().addListener((o, a, b) -> updateStoreTotals());
        reportMode.valueProperty().addListener((o, a, b) -> loadReport());
        reportDate.valueProperty().addListener((o, a, b) -> loadReport());
        updateHours(); refreshAll();
    }
    /** เพิ่มคอลัมน์ให้ตาราง โดยส่งฟังก์ชันที่ดึงค่าจากแต่ละแถว เช่น Booking::id */
    private <T> void column(TableView<T> table, String title, double width, Function<T, ?> value) {
        TableColumn<T, Object> column = new TableColumn<>(title);
        column.setPrefWidth(width); column.setMinWidth(60);
        column.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(value.apply(row.getValue())));
        table.getColumns().add(column);
    }
    /** สร้างคอลัมน์ของทุกตาราง (สร้างในโค้ดแทน FXML เพื่อกำหนดค่าที่แสดงได้ง่าย) */
    private void setupTables() {
        // ตารางค้นหาการจอง และตารางประวัติสมาชิก ใช้คอลัมน์ชุดเดียวกัน
        for (TableView<Booking> table : List.of(bookingTable, historyTable)) {
            column(table, "เลขที่", 85, Booking::id);
            column(table, "สมาชิก", 145, Booking::memberName);
            column(table, "สนาม", 65, Booking::court);
            column(table, "วันที่", 100, b -> dateFormat.format(b.date()));
            column(table, "เวลา", 115, Booking::timeRange);
            column(table, "บาท", 65, Booking::paidBaht);
            column(table, "แพ็กเกจ (ชม.)", 105, Booking::packageHoursUsed);
            column(table, "ผู้บันทึก", 85, Booking::staffUsername);
        }
        column(memberTable, "รหัสสมาชิก", 110, Member::id);
        column(memberTable, "ชื่อ-นามสกุล", 240, Member::name);
        column(memberTable, "เบอร์โทรศัพท์", 170, Member::phone);
        column(memberTable, "วันที่สมัคร", 150, m -> dateFormat.format(m.joinedOn()));
        column(memberTable, "จองสะสม (ชม.)", 160, Member::bookedHours);
        column(memberTable, "แพ็กเกจคงเหลือ (ชม.)", 180, Member::packageHours);
        column(rentalTable, "เลขที่", 100, Rental::id);
        column(rentalTable, "สมาชิก", 200, Rental::memberName);
        column(rentalTable, "อุปกรณ์", 230, r -> r.type().toString());
        column(rentalTable, "จำนวนคู่", 100, Rental::quantity);
        column(rentalTable, "วันที่เช่า", 150, r -> dateFormat.format(r.date()));
        column(rentalTable, "บาท", 100, Rental::amountBaht);
        column(reportTable, "วันที่รับเงิน", 155, p -> p.paidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        column(reportTable, "สมาชิก", 160, Payment::memberName);
        column(reportTable, "ประเภท", 135, p -> p.type().toString());
        column(reportTable, "รายละเอียด", 440, Payment::description);
        column(reportTable, "บาท", 90, Payment::amountBaht);
        column(reportTable, "ผู้บันทึก", 100, Payment::staffUsername);
        for (TableView<?> table : List.of(bookingTable, historyTable, memberTable, rentalTable, reportTable)) {
            table.setPlaceholder(new Label("ยังไม่มีรายการ"));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        // รายการวันหยุดแสดงวันที่พร้อมเวลาเปิดของวันหยุด
        holidayList.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty); setText(empty || date == null ? null : dateFormat.format(date) + "   •   เปิด 10:00 - 20:00");
            }
        });
    }
    /**
     * โหลดข้อมูลล่าสุดจาก service มาแสดงทุกแท็บ เรียกหลังทำรายการสำเร็จทุกครั้ง หรือกดปุ่มรีเฟรช
     * พยายามคงสมาชิก/สินค้าที่เลือกไว้เดิมใน ComboBox
     */
    @FXML private void refreshAll() {
        refreshing = true;
        try {
            List<Member> members = service.members();
            for (ComboBox<Member> combo : List.of(bookingMember, packageMember, storeMember, historyMember)) {
                // จำรหัสที่เลือกไว้ แล้วเลือกคนเดิมจากข้อมูลใหม่ (ยอดชั่วโมงจะเป็นค่าล่าสุด)
                String old = combo.getValue() == null ? "" : combo.getValue().id();
                combo.setItems(FXCollections.observableArrayList(members));
                combo.setValue(members.stream().filter(m -> m.id().equals(old)).findFirst().orElse(members.isEmpty() ? null : members.get(0)));
            }
            String productId = productCombo.getValue() == null ? "" : productCombo.getValue().id();
            productCombo.setItems(FXCollections.observableArrayList(service.products()));
            productCombo.setValue(service.products().stream().filter(p -> p.id().equals(productId)).findFirst().orElse(service.products().get(0)));
            memberCountLabel.setText(String.valueOf(members.size()));
            todayCountLabel.setText(String.valueOf(service.bookings().stream().filter(b -> b.date().equals(service.today())).count()));
            rentalTable.setItems(FXCollections.observableArrayList(service.rentals().stream().sorted(Comparator.comparing(Rental::date).reversed()).toList()));
            holidayList.setItems(FXCollections.observableArrayList(service.holidays()));
        } finally { refreshing = false; }
        // อัปเดตส่วนที่ขึ้นกับค่าในฟอร์มครั้งเดียวหลังโหลดเสร็จ
        filterMembers(); filterBookings(); updateAvailability(); updateQuote(); updatePackageBalance(); updateStoreTotals();
        loadHistory(); loadReport();
    }
    /** เมื่อเปลี่ยนวันที่จอง: แสดงเวลาเปิดของวันนั้น และสร้างตัวเลือกเวลาเริ่มใหม่ */
    private void updateHours() {
        if (bookingDate.getValue() == null) return;
        OperatingHours hours = service.hours(bookingDate.getValue());
        hoursLabel.setText("เวลาเปิด " + hours + " น.");
        Integer old = startCombo.getValue();
        // เวลาเริ่มได้ตั้งแต่ชั่วโมงเปิด ถึงก่อนปิด 1 ชั่วโมง
        startCombo.setItems(FXCollections.observableArrayList(java.util.stream.IntStream.range(hours.open(), hours.close()).boxed().toList()));
        startCombo.setValue(old != null && old >= hours.open() && old < hours.close() ? old : hours.open());
        updateAvailability();
    }
    /** วาดปุ่มสนาม 12 สนาม สีเขียว = ว่าง / สีแดง = ไม่ว่าง ตามวัน เวลาเริ่ม และจำนวนชั่วโมงที่เลือก */
    private void updateAvailability() {
        if (refreshing || bookingDate.getValue() == null || startCombo.getValue() == null || durationCombo.getValue() == null) return;
        courtGrid.getChildren().clear();
        for (int court = 1; court <= 12; court++) {
            int selected = court;
            boolean available = service.isCourtAvailable(court, bookingDate.getValue(), startCombo.getValue(), durationCombo.getValue());
            Button tile = new Button("%02d  •  %s".formatted(court, available ? "ว่าง" : "ไม่ว่าง"));
            tile.getStyleClass().add(available ? "court-free" : "court-busy");
            tile.setPrefSize(118, 43); tile.setDisable(!available);
            // คลิกสนามที่ว่างเพื่อเลือกสนามนั้นในฟอร์ม
            tile.setOnAction(event -> courtCombo.setValue(selected));
            courtGrid.getChildren().add(tile);
        }
    }
    /** แสดงยอดสะสม/แพ็กเกจของสมาชิก และราคาที่ต้องจ่ายตามตัวเลือกปัจจุบัน */
    private void updateQuote() {
        if (refreshing) return;
        Member member = bookingMember.getValue();
        memberBalanceLabel.setText(member == null ? "เลือกสมาชิกเพื่อดูสิทธิ์" : "จองสะสม " + member.bookedHours()
                + " ชม. / แพ็กเกจเหลือ " + member.packageHours() + " ชม.");
        if (member == null || durationCombo.getValue() == null) { quoteLabel.setText("กรุณาเลือกสมาชิก"); return; }
        // ถ้าคำนวณไม่ได้ เช่น แพ็กเกจไม่พอ ให้แสดงเหตุผลแทนราคา
        try { quoteLabel.setText(service.quote(member.id(), durationCombo.getValue(), paymentCombo.getValue()).description()); }
        catch (RuntimeException ex) { quoteLabel.setText(ex.getMessage()); }
    }
    private void updatePackageBalance() {
        Member member = packageMember.getValue();
        packageBalanceLabel.setText(member == null ? "เลือกสมาชิก" : "ชั่วโมงคงเหลือ  " + member.packageHours() + "  ชั่วโมง");
    }
    /** แสดงยอดรวมของการขายสินค้าและการเช่า (เช่าคู่ละ 50 บาท) */
    private void updateStoreTotals() {
        if (productQuantity.getValue() == null || rentalQuantity.getValue() == null) return;
        Product product = productCombo.getValue();
        productTotalLabel.setText(product == null ? "เลือกสินค้า" : "รวม " + money(product.priceBaht() * productQuantity.getValue()));
        rentalTotalLabel.setText("รวม " + money(50L * rentalQuantity.getValue()));
    }
    /**
     * ค้นหาการจอง — ตามโจทย์ "พนักงานแคชเชียร์สามารถค้นหาข้อมูลการจองได้"
     * พิมพ์คำค้นได้ทั้งเลขที่ รหัสสมาชิก ชื่อ หมายเลขสนาม หรือเบอร์โทร และกรองตามวันที่ (หรือติ๊ก "ทุกวัน")
     */
    private void filterBookings() {
        String query = bookingSearch.getText().trim().toLowerCase(Locale.ROOT);
        bookingTable.setItems(FXCollections.observableArrayList(service.bookings().stream()
                .filter(b -> allDates.isSelected() || Objects.equals(b.date(), searchDate.getValue()))
                // รวมข้อมูลที่ค้นได้เป็นข้อความเดียว แล้วตรวจว่ามีคำค้นอยู่หรือไม่
                .filter(b -> (b.id() + " " + b.memberId() + " " + b.memberName() + " " + b.court() + " " + service.member(b.memberId()).phone())
                        .toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Booking::date).thenComparingInt(Booking::startHour).thenComparingInt(Booking::court)).toList()));
    }
    /** ค้นหาสมาชิกจากรหัส ชื่อ หรือเบอร์โทร (ข้อความจาก Member.toString) */
    private void filterMembers() {
        String query = memberSearch.getText().trim().toLowerCase(Locale.ROOT);
        memberTable.setItems(FXCollections.observableArrayList(service.members().stream()
                .filter(m -> m.toString().toLowerCase(Locale.ROOT).contains(query)).toList()));
    }
    /** ปุ่ม "สมัครสมาชิก" — สมัครแล้วเลือกสมาชิกใหม่ในฟอร์มอื่นให้ทันที */
    @FXML private void registerMember() {
        execute(() -> {
            Member member = service.registerMember(memberName.getText(), memberPhone.getText());
            memberName.clear(); memberPhone.clear(); refreshAll();
            bookingMember.setValue(member); packageMember.setValue(member); storeMember.setValue(member);
            success("สมัครสมาชิกสำเร็จ • " + member.id() + " " + member.name());
        });
    }
    // ปุ่มแนบสลิป: จำไฟล์ที่เลือกไว้ และแสดงชื่อไฟล์ใต้ปุ่ม
    @FXML private void chooseBookingSlip() {
        Path file = chooseSlip(); if (file != null) { bookingSlip = file; bookingSlipLabel.setText(file.getFileName().toString()); }
    }
    @FXML private void choosePackageSlip() {
        Path file = chooseSlip(); if (file != null) { packageSlip = file; packageSlipLabel.setText(file.getFileName().toString()); }
    }
    /** เปิดหน้าต่างเลือกไฟล์สลิป คืน null ถ้าผู้ใช้กดยกเลิก */
    private Path chooseSlip() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("แนบสลิปการชำระเงิน");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("สลิป (PNG, JPG, PDF)", "*.png", "*.jpg", "*.jpeg", "*.pdf"));
        java.io.File chosen = chooser.showOpenDialog(BadmintonApp.window());
        return chosen == null ? null : chosen.toPath();
    }
    /** รหัสสมาชิกที่เลือกใน ComboBox ถ้ายังไม่เลือกให้แจ้งเตือน */
    private String selectedMember(ComboBox<Member> combo) {
        if (combo.getValue() == null) throw new IllegalArgumentException("กรุณาสมัครและเลือกสมาชิกก่อน");
        return combo.getValue().id();
    }
    /** ปุ่ม "บันทึกการจอง" — ส่งข้อมูลในฟอร์มให้ service ตรวจและบันทึก */
    @FXML private void saveBooking() {
        execute(() -> {
            if (courtCombo.getValue() == null || startCombo.getValue() == null) throw new IllegalArgumentException("เลือกสนามและเวลาเริ่ม");
            Booking booking = service.book(selectedMember(bookingMember), courtCombo.getValue(), bookingDate.getValue(),
                    startCombo.getValue(), durationCombo.getValue(), paymentCombo.getValue(), bookingSlip);
            // ล้างสลิปที่แนบไว้ เพื่อไม่ให้ใช้ซ้ำกับการจองถัดไปโดยไม่ตั้งใจ
            bookingSlip = null; bookingSlipLabel.setText("ยังไม่ได้เลือกสลิป");
            // เปลี่ยนวันที่ค้นหาเป็นวันที่จอง เพื่อให้เห็นรายการที่เพิ่งบันทึกในตาราง
            searchDate.setValue(booking.date()); refreshAll();
            success("บันทึก " + booking.id() + " สำเร็จ • สนาม " + booking.court() + " • รับเงิน " + money(booking.paidBaht()));
        });
    }
    /** ปุ่มซื้อแพ็กเกจ 15 ชั่วโมง 2,600 บาท */
    @FXML private void buyPackage() {
        execute(() -> {
            service.buyPackage(selectedMember(packageMember), packageSlip);
            packageSlip = null; packageSlipLabel.setText("ยังไม่ได้เลือกสลิป"); refreshAll();
            success("ซื้อแพ็กเกจสำเร็จ • เพิ่ม 15 ชั่วโมง • รับเงิน 2,600 บาท");
        });
    }
    /** ปุ่มขายสินค้า */
    @FXML private void sellProduct() {
        execute(() -> {
            if (productCombo.getValue() == null) throw new IllegalArgumentException("เลือกสินค้า");
            service.sell(selectedMember(storeMember), productCombo.getValue().id(), productQuantity.getValue());
            refreshAll(); success("บันทึกการขายและตัดสต็อกสำเร็จ");
        });
    }
    /** ปุ่มบันทึกการเช่ารองเท้า/ไม้แบด */
    @FXML private void saveRental() {
        execute(() -> {
            service.rent(selectedMember(storeMember), rentalCombo.getValue(), rentalQuantity.getValue(), rentalDate.getValue());
            refreshAll(); success("บันทึกการเช่าสำเร็จ • คิดราคา 50 บาทต่อคู่ต่อครั้งต่อวัน");
        });
    }
    // ปุ่มเปิดสลิปของแถวที่เลือก (แท็บจองสนาม และแท็บประวัติสมาชิก)
    @FXML private void openBookingSlip() { openSelectedSlip(bookingTable); }
    @FXML private void openHistorySlip() { openSelectedSlip(historyTable); }
    /** เปิดสำเนาสลิปของการจองที่เลือกด้วยโปรแกรมเริ่มต้นของเครื่อง (โปรแกรมดูรูป/PDF) */
    private void openSelectedSlip(TableView<Booking> table) {
        execute(() -> {
            Booking booking = table.getSelectionModel().getSelectedItem();
            if (booking == null) throw new IllegalArgumentException("เลือกแถวการจองก่อนเปิดสลิป");
            Path file = service.receiptPath(booking.slipPath());
            if (!Files.isRegularFile(file)) throw new IllegalArgumentException("ไม่พบไฟล์สลิปในแฟ้มข้อมูล");
            Desktop.getDesktop().open(file.toFile());
        });
    }
    /** แสดงประวัติการจองของสมาชิกที่เลือก — ตามโจทย์ "ผู้ดูแลระบบดูประวัติการจองสนามของสมาชิกได้" */
    @FXML private void loadHistory() {
        if (refreshing || !service.currentUser().canViewReports()) return;
        Member member = historyMember.getValue();
        historyTable.setItems(FXCollections.observableArrayList(member == null ? List.of() : service.memberHistory(member.id())));
    }
    /**
     * โหลดรายงานรายได้ — ตามโจทย์ "ผู้ดูแลระบบดูสรุปรายได้ในแต่ละวัน แต่ละเดือนได้"
     * รายวันใช้วันที่เลือก ส่วนรายเดือนใช้วันที่ 1 ถึงวันสุดท้ายของเดือนนั้น แล้วแยกยอดตาม 4 ประเภท
     */
    @FXML private void loadReport() {
        if (refreshing || !service.currentUser().canViewReports() || reportDate.getValue() == null) return;
        LocalDate date = reportDate.getValue();
        boolean monthly = "รายเดือน".equals(reportMode.getValue());
        LocalDate from = monthly ? date.withDayOfMonth(1) : date;
        // lengthOfMonth() คืนจำนวนวันของเดือนนั้น (28-31) จึงได้วันสุดท้ายของเดือนถูกต้อง
        LocalDate to = monthly ? date.withDayOfMonth(date.lengthOfMonth()) : date;
        reportRows = service.revenue(from, to);
        reportTable.setItems(FXCollections.observableArrayList(reportRows));
        reportRangeLabel.setText("วันที่รับเงิน " + dateFormat.format(from) + " - " + dateFormat.format(to));
        reportTotalLabel.setText(money(reportRows.stream().mapToLong(Payment::amountBaht).sum()));
        courtRevenueLabel.setText(revenueOf(RevenueType.COURT)); packageRevenueLabel.setText(revenueOf(RevenueType.PACKAGE));
        rentalRevenueLabel.setText(revenueOf(RevenueType.RENTAL)); productRevenueLabel.setText(revenueOf(RevenueType.PRODUCT));
    }
    /** ยอดรวมของรายได้ประเภทเดียวในรายงานที่แสดงอยู่ */
    private String revenueOf(RevenueType type) { return money(reportRows.stream().filter(p -> p.type() == type).mapToLong(Payment::amountBaht).sum()); }
    /** ส่งออกรายงานที่แสดงอยู่เป็นไฟล์ CSV */
    @FXML private void exportReport() {
        execute(() -> {
            loadReport();
            FileChooser chooser = new FileChooser(); chooser.setTitle("ส่งออกรายได้ CSV");
            chooser.setInitialFileName("revenue-" + reportDate.getValue() + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (เปิดใน Excel ได้)", "*.csv"));
            java.io.File file = chooser.showSaveDialog(BadmintonApp.window());
            if (file != null) { CsvReport.export(file.toPath(), reportRows); success("ส่งออกรายงานสำเร็จ • " + file.getName()); }
        });
    }
    /** เพิ่มวันหยุดนักขัตฤกษ์ แล้วอัปเดตเวลาเปิดในฟอร์มจองทันที */
    @FXML private void addHoliday() {
        execute(() -> { service.setHoliday(holidayDate.getValue(), true); refreshAll(); updateHours(); success("เพิ่มวันหยุดเรียบร้อย"); });
    }
    /** ลบวันหยุดที่เลือกในรายการ */
    @FXML private void removeHoliday() {
        execute(() -> {
            LocalDate date = holidayList.getSelectionModel().getSelectedItem();
            if (date == null) throw new IllegalArgumentException("เลือกวันหยุดในรายการก่อนลบ");
            service.setHoliday(date, false); refreshAll(); updateHours(); success("ลบวันหยุดเรียบร้อย");
        });
    }
    /** ออกจากระบบแล้วกลับไปหน้าเข้าสู่ระบบ */
    @FXML private void logout() {
        execute(() -> { service.signOut(); BadmintonApp.showLogin(); });
    }
    /** จัดรูปแบบเงิน เช่น 2600 → "2,600 บาท" */
    private String money(long amount) { return String.format(Locale.US, "%,d บาท", amount); }
    /** แสดงข้อความสำเร็จสีเขียวที่แถบสถานะด้านล่าง */
    private void success(String text) { statusLabel.setText("✓  " + text); statusLabel.setStyle("-fx-text-fill: #12654a;"); }
    /** งานที่โยน exception ได้ ใช้ส่งเข้า execute() */
    @FunctionalInterface private interface Action { void run() throws Exception; }
    /**
     * ทำงานของปุ่มแบบปลอดภัย: ถ้าเกิดข้อผิดพลาด (เช่น สนามไม่ว่าง ไม่แนบสลิป)
     * จะแสดงข้อความสีแดงที่แถบสถานะและหน้าต่างแจ้งเตือน แทนที่โปรแกรมจะค้าง
     */
    private void execute(Action action) {
        try { action.run(); }
        catch (Exception ex) {
            statusLabel.setText(ex.getMessage()); statusLabel.setStyle("-fx-text-fill: #a23434;");
            Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            alert.initOwner(BadmintonApp.window()); alert.setHeaderText("ทำรายการไม่สำเร็จ"); alert.showAndWait();
        }
    }
}
