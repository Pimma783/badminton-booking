package com.badminton.service;

import com.badminton.model.*;
import com.badminton.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ทดสอบกติกาธุรกิจใน ClubService ตามข้อกำหนดของโจทย์ โดยไม่ต้องเปิดหน้าจอ
 * ใช้โฟลเดอร์ชั่วคราว (@TempDir) จึงไม่กระทบข้อมูลจริงใน data/
 */
class ClubServiceTest {
    @TempDir Path directory;
    private ClubService service;
    private Member member;
    private Path slip;
    // วันจันทร์ที่ 12/10/2026 เป็นวันธรรมดา
    private final LocalDate monday = LocalDate.of(2026, 10, 12);
    // ตั้งนาฬิกาตายตัวเป็น 08:00 น. เวลาไทย (01:00 UTC) ของวันจันทร์ ผลทดสอบจึงไม่ขึ้นกับวันที่รันจริง
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T01:00:00Z"), ZoneId.of("Asia/Bangkok"));
    // ก่อนทุกเทสต์: เข้าสู่ระบบเป็น admin สมัครสมาชิก 1 คน และสร้างไฟล์สลิป PNG จริง
    @BeforeEach void setup() throws Exception {
        service = new ClubService(new FileClubRepository(directory), new StandardPricingPolicy(), directory, clock);
        service.signIn("admin", "admin123");
        member = service.registerMember("สมาชิก ทดสอบ", "0812345678");
        slip = directory.resolve("test-slip.png");
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", slip.toFile());
    }
    private Booking book(int court, LocalDate date, int hour, int duration, PaymentMode mode) {
        return service.book(member.id(), court, date, hour, duration, mode, slip);
    }
    // วันธรรมดา 13-21 / เสาร์ อาทิตย์ วันหยุด 10-20 และยกเลิกวันหยุดไม่ได้ถ้ามีการจอง 10:00 อยู่แล้ว
    @Test void weekdayWeekendAndHolidayHours() {
        assertEquals(new OperatingHours(13, 21), service.hours(monday));
        assertEquals(new OperatingHours(10, 20), service.hours(monday.plusDays(5)));
        assertEquals(new OperatingHours(10, 20), service.hours(monday.plusDays(6)));
        service.setHoliday(monday, true);
        assertEquals(new OperatingHours(10, 20), service.hours(monday));
        book(1, monday, 10, 1, PaymentMode.CASH);
        assertThrows(IllegalArgumentException.class, () -> service.setHoliday(monday, false));
    }
    // จอง 20:00-21:00 วันธรรมดาได้ แต่ก่อน 13:00 หรือเลยเวลาปิดไม่ได้
    @Test void closingTimeBoundaryAndOutsideHours() {
        book(1, monday, 20, 1, PaymentMode.CASH);
        assertThrows(IllegalArgumentException.class, () -> book(1, monday, 12, 1, PaymentMode.CASH));
        assertThrows(IllegalArgumentException.class, () -> book(2, monday, 20, 2, PaymentMode.CASH));
        assertThrows(IllegalArgumentException.class, () -> book(1, monday.plusDays(5), 20, 1, PaymentMode.CASH));
    }
    // จองย้อนหลัง หรือจองเวลาเริ่มที่ผ่านไปแล้วไม่ได้ (นาฬิกา 13:30 น. จอง 13:00 ไม่ได้)
    @Test void rejectsPastDatesAndPastStartTime() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> book(1, monday.minusDays(1), 13, 1, PaymentMode.CASH));
        Clock afternoon = Clock.fixed(Instant.parse("2026-10-12T06:30:00Z"), ZoneId.of("Asia/Bangkok"));
        ClubService later = new ClubService(new FileClubRepository(directory), new StandardPricingPolicy(), directory, afternoon);
        later.signIn("cashier", "cashier123");
        assertThrows(IllegalArgumentException.class, () -> later.book(member.id(), 1, monday, 13, 1, PaymentMode.CASH, slip));
    }
    // สนามเดียวกันเวลาซ้อนกันจองไม่ได้ แต่จองต่อกันพอดีหรือคนละสนามได้
    @Test void overlapRejectedButAdjacentBookingsAndOtherCourtsAllowed() {
        book(1, monday, 13, 2, PaymentMode.CASH);
        assertThrows(IllegalArgumentException.class, () -> book(1, monday, 14, 2, PaymentMode.CASH));
        book(1, monday, 15, 1, PaymentMode.CASH);
        book(2, monday, 14, 1, PaymentMode.CASH);
        assertEquals(3, service.bookings().size());
    }
    // มีสนาม 1-12 เท่านั้น
    @Test void exactlyTwelveCourts() {
        for (int court = 1; court <= 12; court++) book(court, monday, 13, 1, PaymentMode.CASH);
        assertThrows(IllegalArgumentException.class, () -> book(13, monday, 13, 1, PaymentMode.CASH));
        assertThrows(IllegalArgumentException.class, () -> book(0, monday, 13, 1, PaymentMode.CASH));
    }
    // ต้องเป็นสมาชิกและแนบสลิปทุกครั้ง แม้ใช้แพ็กเกจ
    @Test void requiresMemberAndSlipForEveryBooking() {
        assertThrows(IllegalArgumentException.class, () -> service.book("missing", 1, monday, 13, 1, PaymentMode.CASH, slip));
        assertThrows(IllegalArgumentException.class, () -> service.book(member.id(), 1, monday, 13, 1, PaymentMode.CASH, null));
        service.buyPackage(member.id(), slip);
        assertThrows(IllegalArgumentException.class, () -> service.book(member.id(), 1, monday, 13, 1, PaymentMode.PACKAGE, null));
        assertEquals(0, service.bookings().size());
    }
    // ไฟล์ปลอมที่ตั้งชื่อเป็น .png ต้องถูกปฏิเสธ และสลิปจริงถูกคัดลอกเก็บ (ลบต้นฉบับแล้วสำเนายังอยู่)
    @Test void rejectsFakeSlipAndCopiesEvidence() throws Exception {
        Path fake = directory.resolve("fake.png"); Files.writeString(fake, "this is not an image");
        assertThrows(IllegalArgumentException.class, () -> service.book(member.id(), 1, monday, 13, 1, PaymentMode.CASH, fake));
        Booking booking = book(1, monday, 13, 1, PaymentMode.CASH);
        Path copy = service.receiptPath(booking.slipPath());
        assertTrue(Files.isRegularFile(copy)); assertNotEquals(slip, copy);
        Files.delete(slip); assertTrue(Files.exists(copy));
    }
    // ซื้อแพ็กเกจ 15 ชม. ใช้ 2 ชม. เหลือ 13 รายได้ 2,600 บาท และเปิดโปรแกรมใหม่แล้วข้อมูลยังอยู่
    @Test void packageBalanceRevenueAndPersistence() throws Exception {
        service.buyPackage(member.id(), slip);
        book(1, monday.plusDays(1), 13, 2, PaymentMode.PACKAGE);
        assertEquals(13, service.member(member.id()).packageHours());
        assertEquals(2, service.member(member.id()).bookedHours());
        assertEquals(2600, service.revenueTotal(monday, monday));
        ClubService reopened = new ClubService(new FileClubRepository(directory), new StandardPricingPolicy(), directory, clock);
        reopened.signIn("admin", "admin123");
        assertEquals(13, reopened.member(member.id()).packageHours());
        assertEquals(1, reopened.bookings().size());
        assertEquals(2600, reopened.revenueTotal(monday, monday));
    }
    // ไม่มีแพ็กเกจแต่เลือกใช้แพ็กเกจ → จองไม่สำเร็จ และไม่มีข้อมูลใดเปลี่ยน
    @Test void insufficientPackageDoesNotMutateAnything() {
        assertThrows(IllegalArgumentException.class, () -> book(1, monday, 13, 1, PaymentMode.PACKAGE));
        assertEquals(0, service.member(member.id()).bookedHours());
        assertEquals(0, service.bookings().size()); assertEquals(0, service.revenueTotal(monday, monday));
    }
    // จองจริง 8 + 8 + 4 ชั่วโมง ชั่วโมงที่ 20 อยู่ในการจองครั้งสุดท้าย คิด 54 บาท
    @Test void discountedTwentiethHourThroughRealBookings() {
        book(1, monday, 13, 8, PaymentMode.CASH);
        book(1, monday.plusDays(1), 13, 8, PaymentMode.CASH);
        Booking last = book(1, monday.plusDays(2), 13, 4, PaymentMode.CASH);
        assertEquals(594, last.paidBaht()); // 3 * 180 + 54
        assertEquals(20, service.member(member.id()).bookedHours());
        assertEquals(19 * 180 + 54, service.revenueTotal(monday, monday));
    }
    // เบอร์ซ้ำ (แม้เขียนมีขีด) ชื่อว่าง และเบอร์ไม่ใช่ตัวเลข ต้องสมัครไม่ได้
    @Test void memberValidationAndDuplicatePhone() {
        assertThrows(IllegalArgumentException.class, () -> service.registerMember("อื่น", "081-234-5678"));
        assertThrows(IllegalArgumentException.class, () -> service.registerMember("", "0812345679"));
        assertThrows(IllegalArgumentException.class, () -> service.registerMember("สมาชิก", "hello"));
    }
    // แคชเชียร์จองได้ แต่ดูรายได้ ประวัติ และตั้งวันหยุดไม่ได้ / ออกจากระบบแล้วใช้งานไม่ได้
    @Test void cashierCanTransactButCannotReadReportsHistoryOrChangeHolidays() {
        service.signIn("cashier", "cashier123");
        book(1, monday, 13, 1, PaymentMode.CASH);
        assertEquals(1, service.bookings().size());
        assertThrows(IllegalStateException.class, () -> service.revenue(monday, monday));
        assertThrows(IllegalStateException.class, () -> service.memberHistory(member.id()));
        assertThrows(IllegalStateException.class, () -> service.setHoliday(monday, true));
        service.signOut(); assertThrows(IllegalStateException.class, () -> service.members());
    }
    // รายได้รายวัน/รายเดือนรวมครบ 4 ประเภท และนับตามวันที่รับเงิน ไม่ใช่วันที่ใช้สนาม
    @Test void dailyMonthlyRevenueIncludesAllCategoriesByPaymentDate() {
        service.buyPackage(member.id(), slip);
        book(1, monday.plusDays(2), 13, 2, PaymentMode.CASH);
        service.rent(member.id(), RentalType.SHOES, 2, monday.plusDays(1));
        service.rent(member.id(), RentalType.RACKET, 1, monday);
        service.sell(member.id(), "P003", 2);
        assertEquals(3240, service.revenueTotal(monday, monday)); // 2600+360+100+50+130
        assertEquals(3240, service.revenueTotal(monday.withDayOfMonth(1), monday.withDayOfMonth(31)));
        assertEquals(0, service.revenueTotal(monday.plusDays(2), monday.plusDays(2)));
        assertEquals(4, service.revenue(monday, monday).stream().map(Payment::type).distinct().count());
    }
    // ขายแล้วตัดสต็อก ขายเกินสต็อกหรือจำนวน 0 ไม่ได้ และเช่า 0 คู่ไม่ได้
    @Test void salesCannotExceedStockAndRentalsRequirePositiveQuantity() {
        service.sell(member.id(), "P001", 2);
        assertEquals(28, service.products().get(0).stock());
        assertThrows(IllegalArgumentException.class, () -> service.sell(member.id(), "P001", 29));
        assertThrows(IllegalArgumentException.class, () -> service.sell(member.id(), "P001", 0));
        assertThrows(IllegalArgumentException.class, () -> service.rent(member.id(), RentalType.SHOES, 0, monday));
    }
    // มีการจอง 20:00 ในวันธรรมดาแล้ว จะตั้งเป็นวันหยุด (ปิด 20:00) ไม่ได้
    @Test void changingHolidayCannotInvalidateExistingBooking() {
        book(1, monday, 20, 1, PaymentMode.CASH);
        assertThrows(IllegalArgumentException.class, () -> service.setHoliday(monday, true));
        assertFalse(service.holidays().contains(monday));
    }
    // จำลองดิสก์เต็ม: บันทึกไม่สำเร็จต้องไม่มีข้อมูลเปลี่ยน และไม่มีสลิปค้างในโฟลเดอร์
    @Test void saveFailureDoesNotPublishPartiallyChangedStateOrLeaveSlip() throws Exception {
        ClubRepository failing = new ClubRepository() {
            @Override public ClubData load() { ClubData d = ClubData.emptyClub(); d.members.add(member); return d; }
            @Override public void save(ClubData data) throws IOException { throw new IOException("disk full"); }
        };
        Path separate = directory.resolve("failed");
        ClubService broken = new ClubService(failing, new StandardPricingPolicy(), separate, clock);
        broken.signIn("cashier", "cashier123");
        assertThrows(UncheckedIOException.class, () -> broken.book(member.id(), 1, monday, 13, 1, PaymentMode.CASH, slip));
        assertEquals(0, broken.bookings().size()); assertEquals(0, broken.member(member.id()).bookedHours());
        try (var files = Files.list(separate.resolve("receipts"))) { assertEquals(0, files.count()); }
    }
    // รหัสผ่านผิดเข้าระบบไม่ได้ และแต่ละบัญชีได้บทบาทถูกต้อง
    @Test void invalidLoginRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AuthService().login("admin", "wrong"));
        assertEquals(Role.ADMIN, new AuthService().login("admin", "admin123").getRole());
        assertEquals(Role.CASHIER, new AuthService().login("cashier", "cashier123").getRole());
    }
}
