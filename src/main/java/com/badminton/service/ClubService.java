package com.badminton.service;

import com.badminton.model.*;
import com.badminton.repository.ClubRepository;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * หัวใจของระบบ: รวมกติกาธุรกิจทั้งหมดตามโจทย์ (สมาชิก การจอง แพ็กเกจ เช่า/ขาย วันหยุด รายได้)
 * <p>
 * ไม่ขึ้นกับ JavaFX จึงทดสอบได้โดยไม่ต้องเปิดหน้าจอ และกติกายังถูกบังคับใช้แม้เรียกจากที่อื่นที่ไม่ใช่ UI
 * รับ {@link ClubRepository} และ {@link PricingPolicy} เป็น interface ผ่าน constructor
 * จึงเปลี่ยนวิธีเก็บข้อมูลหรือวิธีคิดราคาได้โดยไม่ต้องแก้คลาสนี้
 */
public final class ClubService {
    /** จำนวนสนามตามโจทย์ */
    public static final int COURT_COUNT = 12;
    private final ClubRepository repository;
    private final PricingPolicy pricing;
    // โฟลเดอร์ข้อมูล ใช้เก็บสำเนาสลิปใน receipts/
    private final Path directory;
    // นาฬิกาที่ส่งเข้ามา ทำให้เทสต์กำหนด "เวลาปัจจุบัน" เองได้
    private final Clock clock;
    // ข้อมูลชุดปัจจุบันที่บันทึกลงไฟล์แล้ว
    private ClubData data;
    // ผู้ที่เข้าสู่ระบบอยู่ (null = ยังไม่ได้เข้าสู่ระบบ)
    private StaffUser currentUser;

    public ClubService(ClubRepository repository, PricingPolicy pricing, Path directory, Clock clock) throws IOException {
        this.repository = repository; this.pricing = pricing;
        this.directory = directory.toAbsolutePath().normalize(); this.clock = clock;
        data = repository.load();
    }
    /** เข้าสู่ระบบ — ได้ Cashier หรือ Administrator ตามบัญชี */
    public void signIn(String username, String password) { currentUser = new AuthService().login(username, password); }
    public void signOut() { currentUser = null; }
    public StaffUser currentUser() { requireUser(); return currentUser; }
    /** วันนี้ตามเขตเวลาของนาฬิกา (Asia/Bangkok) */
    public LocalDate today() { return LocalDate.now(clock); }
    public Path directory() { return directory; }
    /** ทุกคำสั่งต้องเข้าสู่ระบบก่อน */
    private void requireUser() {
        if (currentUser == null) throw new IllegalStateException("กรุณาเข้าสู่ระบบก่อน");
    }
    /**
     * คำสั่งเฉพาะผู้ดูแลระบบ — ตรวจที่ service ด้วย ไม่ใช่แค่ซ่อนแท็บบนหน้าจอ
     * เรียก canViewReports() ผ่านชนิด StaffUser แล้วแต่ละคลาสลูกตอบไม่เหมือนกัน (Polymorphism)
     */
    private void requireAdmin() {
        requireUser();
        if (!currentUser.canViewReports()) throw new IllegalStateException("เฉพาะผู้ดูแลระบบเท่านั้น");
    }
    // เมธอดอ่านข้อมูลคืนสำเนาที่แก้ไม่ได้ (List.copyOf) หน้าจอจึงแก้ข้อมูลตรง ๆ ไม่ได้
    public List<Member> members() { requireUser(); return List.copyOf(data.members); }
    public List<Booking> bookings() { requireUser(); return List.copyOf(data.bookings); }
    public List<Product> products() { requireUser(); return List.copyOf(data.products); }
    public List<Rental> rentals() { requireUser(); return List.copyOf(data.rentals); }
    public Set<LocalDate> holidays() { requireUser(); return Collections.unmodifiableSet(new TreeSet<>(data.holidays)); }
    /** เวลาเปิด-ปิดของวันที่เลือก โดยคิดวันหยุดนักขัตฤกษ์ที่ตั้งไว้ด้วย */
    public OperatingHours hours(LocalDate date) {
        requireUser();
        if (date == null) throw new IllegalArgumentException("เลือกวันที่");
        return OperatingHours.forDate(date, data.holidays);
    }
    /** หาสมาชิกจากรหัส ถ้าไม่พบแปลว่ายังไม่ได้สมัคร จึงจองไม่ได้ */
    public Member member(String id) {
        requireUser();
        return data.members.stream().filter(m -> m.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("กรุณาเลือกสมาชิกที่สมัครแล้ว"));
    }
    /**
     * สมัครสมาชิก — ตามโจทย์ "ผู้ที่สามารถจองสนามได้ต้องสมัครเป็นสมาชิก"
     * ตรวจชื่อ 2-100 ตัวอักษร เบอร์โทร 9-10 หลักขึ้นต้นด้วย 0 และห้ามเบอร์ซ้ำ
     */
    public synchronized Member registerMember(String name, String phone) {
        requireUser();
        String cleanName = name == null ? "" : name.trim();
        // ตัดช่องว่างและขีดออก เช่น "081-234-5678" → "0812345678"
        String cleanPhone = phone == null ? "" : phone.replaceAll("[\\s-]", "");
        if (cleanName.length() < 2 || cleanName.length() > 100) throw new IllegalArgumentException("กรอกชื่อสมาชิก 2-100 ตัวอักษร");
        if (!cleanPhone.matches("0[0-9]{8,9}")) throw new IllegalArgumentException("กรอกเบอร์โทร 9-10 หลัก เริ่มด้วย 0");
        if (data.members.stream().anyMatch(m -> m.phone().equals(cleanPhone))) throw new IllegalArgumentException("เบอร์โทรนี้สมัครสมาชิกแล้ว");
        // รหัสเรียงตามลำดับ M0001, M0002, ... (ระบบไม่มีการลบสมาชิก รหัสจึงไม่ซ้ำ)
        Member member = new Member("M%04d".formatted(data.members.size() + 1), cleanName, cleanPhone, today(), 0, 0);
        commit(copy -> copy.members.add(member));
        return member;
    }
    /** คำนวณราคาล่วงหน้าเพื่อแสดงในแบบฟอร์ม (ยังไม่บันทึกอะไร) */
    public BookingQuote quote(String memberId, int duration, PaymentMode mode) {
        return pricing.quote(member(memberId), duration, mode);
    }
    /**
     * สนามว่างหรือไม่ — ต้องอยู่ในเวลาเปิด ยังไม่ผ่านไปแล้ว และไม่ชนกับการจองเดิมในสนามเดียวกัน
     * <p>
     * ช่วงเวลา [start, start + duration) ชนกับการจองเดิม [b.start, b.end) เมื่อ
     * start &lt; b.end และ start + duration &gt; b.start
     * จึงจองต่อกันพอดีได้ เช่น 13-15 กับ 15-16
     */
    public boolean isCourtAvailable(int court, LocalDate date, int start, int duration) {
        requireUser();
        if (date == null || court < 1 || court > COURT_COUNT || !hours(date).contains(start, duration)) return false;
        if (date.atTime(start, 0).isBefore(LocalDateTime.now(clock))) return false;
        return data.bookings.stream().noneMatch(b -> b.court() == court && b.date().equals(date)
                && start < b.endHour() && start + duration > b.startHour());
    }
    /**
     * บันทึกการจองสนาม — ตามโจทย์ แคชเชียร์กรอกชื่อสมาชิก หมายเลขสนาม วัน เวลา และแนบสลิป
     * <ol>
     *   <li>ตรวจสมาชิก สนาม 1-12 วันที่ เวลาเปิด และการจองซ้อน</li>
     *   <li>คำนวณราคาด้วย PricingPolicy</li>
     *   <li>ตรวจและคัดลอกสลิปเก็บไว้</li>
     *   <li>บันทึกการจอง เพิ่มชั่วโมงสะสม/หักแพ็กเกจ และบันทึกรายได้ในขั้นตอนเดียว</li>
     * </ol>
     * synchronized: ป้องกันการจองสองรายการพร้อมกันแล้วได้สนามเวลาเดียวกัน
     */
    public synchronized Booking book(String memberId, int court, LocalDate date, int start,
                                     int duration, PaymentMode mode, Path slip) {
        requireUser();
        Member member = member(memberId);
        if (court < 1 || court > COURT_COUNT) throw new IllegalArgumentException("เลือกสนาม 1-12");
        if (date == null || date.isBefore(today())) throw new IllegalArgumentException("ไม่สามารถจองวันที่ผ่านมาแล้ว");
        if (date.atTime(start, 0).isBefore(LocalDateTime.now(clock))) throw new IllegalArgumentException("เวลาเริ่มจองผ่านไปแล้ว");
        if (!hours(date).contains(start, duration)) throw new IllegalArgumentException("เวลาที่เลือกอยู่นอกเวลาเปิด " + hours(date));
        if (!isCourtAvailable(court, date, start, duration)) throw new IllegalArgumentException("สนามนี้มีการจองซ้อนในช่วงเวลาที่เลือก");
        BookingQuote quote = pricing.quote(member, duration, mode);
        // ต้องแนบหลักฐานทุกครั้ง แม้จะใช้แพ็กเกจจ่ายทั้งหมดก็ตาม
        return withSlip(slip, stored -> {
            Booking booking = new Booking("B%05d".formatted(data.bookings.size() + 1), member.id(), member.name(),
                    court, date, start, duration, quote.packageHoursUsed(), quote.discountedHours(), quote.paidBaht(),
                    stored, currentUser.getUsername(), LocalDateTime.now(clock));
            commit(copy -> {
                copy.bookings.add(booking);
                // ชั่วโมงสะสมเพิ่มตามจำนวนที่จอง (นับไปหาสิทธิ์ชั่วโมงที่ 20) และหักแพ็กเกจที่ใช้
                replaceMember(copy, member.withBooking(duration, quote.packageHoursUsed()));
                // บันทึกเป็นรายได้เฉพาะส่วนที่จ่ายเงินจริง (ส่วนที่ใช้แพ็กเกจนับรายได้ไปแล้วตอนซื้อแพ็กเกจ)
                if (quote.paidBaht() > 0) copy.payments.add(payment(member, RevenueType.COURT,
                        booking.id() + " สนาม " + court + " / " + date + " / " + booking.timeRange(), quote.paidBaht(), stored));
            });
            return booking;
        });
    }
    /** ซื้อแพ็กเกจเหมาจ่าย 15 ชั่วโมง ราคา 2,600 บาท (ต้องแนบสลิป) */
    public synchronized void buyPackage(String memberId, Path slip) {
        requireUser(); Member member = member(memberId);
        withSlip(slip, stored -> {
            commit(copy -> {
                replaceMember(copy, member.withPackage());
                copy.payments.add(payment(member, RevenueType.PACKAGE, "แพ็กเกจ 15 ชั่วโมง", 2600, stored));
            });
            return null;
        });
    }
    /** เช่ารองเท้าหรือไม้แบด คู่ละ 50 บาท/ครั้ง/วัน รับเงินทันทีจึงไม่ต้องแนบสลิป */
    public synchronized void rent(String memberId, RentalType type, int quantity, LocalDate date) {
        requireUser(); Member member = member(memberId);
        if (type == null) throw new IllegalArgumentException("เลือกอุปกรณ์ที่ต้องการเช่า");
        if (quantity < 1 || quantity > 50) throw new IllegalArgumentException("จำนวนเช่าต้องเป็น 1-50 คู่");
        if (date == null || date.isBefore(today())) throw new IllegalArgumentException("เลือกวันที่เช่าตั้งแต่วันนี้เป็นต้นไป");
        Rental rental = new Rental("R%05d".formatted(data.rentals.size() + 1), member.id(), member.name(),
                type, quantity, date, quantity * 50L, currentUser.getUsername());
        commit(copy -> {
            copy.rentals.add(rental);
            copy.payments.add(payment(member, RevenueType.RENTAL,
                    rental.id() + " " + type + " x " + quantity + " / " + date, rental.amountBaht(), ""));
        });
    }
    /** ขายสินค้า (ไม้แบด ลูกขนไก่ เทปพันด้าม) และตัดสต็อก ห้ามขายเกินจำนวนคงเหลือ */
    public synchronized void sell(String memberId, String productId, int quantity) {
        requireUser(); Member member = member(memberId);
        Product product = data.products.stream().filter(p -> p.id().equals(productId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("เลือกสินค้า"));
        if (quantity < 1 || quantity > product.stock()) throw new IllegalArgumentException("จำนวนสินค้าไม่ถูกต้องหรือสินค้าไม่เพียงพอ");
        commit(copy -> {
            // Product เป็น record จึงแทนที่ด้วยตัวใหม่ที่สต็อกลดลง
            copy.products.set(copy.products.indexOf(product), product.afterSale(quantity));
            copy.payments.add(payment(member, RevenueType.PRODUCT, product.name() + " x " + quantity,
                    product.priceBaht() * quantity, ""));
        });
    }
    /**
     * เพิ่ม/ลบวันหยุดนักขัตฤกษ์ (เฉพาะผู้ดูแลระบบ) วันหยุดจะเปิด 10:00 - 20:00
     * ไม่ยอมให้เปลี่ยนถ้าทำให้การจองที่มีอยู่แล้วตกไปอยู่นอกเวลาเปิดใหม่
     */
    public synchronized void setHoliday(LocalDate date, boolean enabled) {
        requireAdmin();
        if (date == null) throw new IllegalArgumentException("เลือกวันหยุด");
        // ลองคำนวณเวลาเปิดใหม่จากรายการวันหยุดที่เปลี่ยนแล้วก่อน
        Set<LocalDate> changed = new HashSet<>(data.holidays);
        if (enabled) changed.add(date); else changed.remove(date);
        OperatingHours newHours = OperatingHours.forDate(date, changed);
        if (data.bookings.stream().anyMatch(b -> b.date().equals(date) && !newHours.contains(b.startHour(), b.duration())))
            throw new IllegalArgumentException("เปลี่ยนวันหยุดไม่ได้ เพราะมีการจองนอกเวลาเปิดใหม่ " + newHours);
        commit(copy -> { if (enabled) copy.holidays.add(date); else copy.holidays.remove(date); });
    }
    /** ประวัติการจองสนามของสมาชิก เรียงตามวันและเวลา (เฉพาะผู้ดูแลระบบ) */
    public List<Booking> memberHistory(String memberId) {
        requireAdmin(); member(memberId);
        return data.bookings.stream().filter(b -> b.memberId().equals(memberId))
                .sorted(Comparator.comparing(Booking::date).thenComparingInt(Booking::startHour)).toList();
    }
    /**
     * รายการรับเงินในช่วงวันที่ from ถึง to (รวมทั้งสองวัน) เฉพาะผู้ดูแลระบบ
     * รายวันส่ง from = to ส่วนรายเดือนส่งวันที่ 1 ถึงวันสุดท้ายของเดือน
     */
    public List<Payment> revenue(LocalDate from, LocalDate to) {
        requireAdmin();
        if (from == null || to == null || to.isBefore(from)) throw new IllegalArgumentException("ช่วงวันที่ไม่ถูกต้อง");
        return data.payments.stream().filter(p -> !p.paidAt().toLocalDate().isBefore(from)
                && !p.paidAt().toLocalDate().isAfter(to)).sorted(Comparator.comparing(Payment::paidAt)).toList();
    }
    /** ยอดรายได้รวมในช่วงวันที่ */
    public long revenueTotal(LocalDate from, LocalDate to) { return revenue(from, to).stream().mapToLong(Payment::amountBaht).sum(); }
    /**
     * แปลงตำแหน่งสลิปที่เก็บไว้เป็น path เต็ม
     * ตรวจว่าอยู่ในโฟลเดอร์ receipts จริง กันข้อมูลที่ถูกแก้ให้ชี้ไปไฟล์อื่น เช่น "../../ไฟล์ระบบ"
     */
    public Path receiptPath(String stored) {
        requireUser();
        if (stored == null || stored.isBlank()) throw new IllegalArgumentException("ไม่พบสลิป");
        Path path = directory.resolve(stored).normalize();
        if (!path.startsWith(directory.resolve("receipts")))
            throw new IllegalArgumentException("ไม่พบสลิป");
        return path;
    }
    /** สร้างรายการรับเงิน ลงเวลาปัจจุบันและชื่อพนักงานที่เข้าสู่ระบบ */
    private Payment payment(Member member, RevenueType type, String description, long amount, String slip) {
        return new Payment("PAY-" + UUID.randomUUID().toString().substring(0, 8), member.id(), member.name(), type,
                description, amount, slip, currentUser.getUsername(), LocalDateTime.now(clock));
    }
    /** แทนที่ข้อมูลสมาชิกในสำเนาด้วยค่าใหม่ (เพราะ Member เป็น record แก้ค่าในตัวไม่ได้) */
    private void replaceMember(ClubData copy, Member member) {
        for (int i = 0; i < copy.members.size(); i++) {
            if (copy.members.get(i).id().equals(member.id())) { copy.members.set(i, member); return; }
        }
        throw new IllegalArgumentException("ไม่พบสมาชิก");
    }
    /**
     * บันทึกการเปลี่ยนแปลงแบบ "ทั้งหมดหรือไม่มีเลย"
     * แก้ไขบนสำเนา → บันทึกลงไฟล์ → ถ้าสำเร็จจึงใช้สำเนาเป็นข้อมูลปัจจุบัน
     * ถ้าบันทึกล้มเหลว ข้อมูลเดิมไม่เปลี่ยน จึงไม่มีสภาพครึ่ง ๆ เช่น มีการจองแต่ยอดสมาชิกไม่ขยับ
     */
    private void commit(Consumer<ClubData> change) {
        ClubData copy = new ClubData(data);
        change.accept(copy);
        try { repository.save(copy); data = copy; }
        catch (IOException ex) { throw new UncheckedIOException("บันทึกข้อมูลไม่สำเร็จ กรุณาตรวจสอบพื้นที่จัดเก็บ", ex); }
    }
    /**
     * ตรวจสลิป คัดลอกเข้าโฟลเดอร์ receipts แล้วเรียก operation พร้อมตำแหน่งไฟล์ที่เก็บ
     * <p>
     * ตรวจชนิดไฟล์จาก "ไบต์แรกของไฟล์" (file signature) ไม่ได้เชื่อนามสกุล
     * เช่น ไฟล์ข้อความที่เปลี่ยนชื่อเป็น .png จะถูกปฏิเสธ
     * ถ้า operation ล้มเหลว จะลบสำเนาสลิปทิ้งเพื่อไม่ให้มีไฟล์ค้างโดยไม่มีรายการอ้างอิง
     */
    private <T> T withSlip(Path source, Function<String, T> operation) {
        Path stored = null;
        try {
            if (source == null || !Files.isRegularFile(source)) throw new IllegalArgumentException("กรุณาแนบสลิปการชำระเงิน (PNG, JPG หรือ PDF)");
            if (Files.size(source) > 10 * 1024 * 1024) throw new IllegalArgumentException("สลิปต้องมีขนาดไม่เกิน 10 MB");
            byte[] header;
            try (InputStream input = Files.newInputStream(source)) { header = input.readNBytes(8); }
            String extension;
            // PNG ขึ้นต้นด้วย 89 50 4E 47 0D 0A 1A 0A / JPG ขึ้นต้นด้วย FF D8 FF / PDF ขึ้นต้นด้วยข้อความ "%PDF-"
            if (header.length >= 8 && Arrays.equals(header, new byte[]{(byte)137,80,78,71,13,10,26,10})) extension = ".png";
            else if (header.length >= 3 && header[0] == (byte)255 && header[1] == (byte)216 && header[2] == (byte)255) extension = ".jpg";
            else if (header.length >= 5 && new String(header, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) extension = ".pdf";
            else throw new IllegalArgumentException("ไฟล์สลิปต้องเป็นรูป PNG, JPG หรือ PDF จริง");
            Files.createDirectories(directory.resolve("receipts"));
            // ตั้งชื่อใหม่แบบสุ่ม (UUID) กันชื่อซ้ำ และต้นฉบับถูกย้าย/ลบภายหลังก็ไม่กระทบสำเนา
            stored = directory.resolve("receipts").resolve(UUID.randomUUID() + extension);
            Files.copy(source, stored);
            T result = operation.apply(directory.relativize(stored).toString());
            stored = null; // บันทึกสำเร็จแล้ว สำเนาสลิปเป็นของรายการนี้ ห้ามลบ
            return result;
        } catch (IOException ex) { throw new UncheckedIOException("คัดลอกสลิปไม่สำเร็จ", ex); }
        finally {
            if (stored != null) try { Files.deleteIfExists(stored); } catch (IOException ignored) { /* ยังไม่มีข้อมูลใดถูกบันทึก ลบไม่สำเร็จก็ไม่กระทบระบบ */ }
        }
    }
}
