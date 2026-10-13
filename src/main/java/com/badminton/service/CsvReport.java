package com.badminton.service;

import com.badminton.model.Payment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** ส่งออกรายงานรายได้เป็นไฟล์ CSV เพื่อเปิดต่อใน Excel */
public final class CsvReport {
    // คลาสนี้มีแต่ static method จึงห้ามสร้างอ็อบเจกต์
    private CsvReport() {}
    public static void export(Path file, List<Payment> payments) throws IOException {
        // อักขระ BOM ที่ต้นไฟล์ทำให้ Excel รู้ว่าเป็น UTF-8 และแสดงภาษาไทยได้ถูกต้อง
        // ใช้ \r\n ขึ้นบรรทัดใหม่ตามมาตรฐาน CSV
        StringBuilder csv = new StringBuilder("\uFEFFวันที่รับเงิน,เลขที่,สมาชิก,ประเภท,รายละเอียด,จำนวนเงิน (บาท),พนักงาน\r\n");
        for (Payment p : payments) {
            csv.append(cell(p.paidAt().toString())).append(',').append(cell(p.id())).append(',')
               .append(cell(p.memberName())).append(',').append(cell(p.type().toString())).append(',')
               .append(cell(p.description())).append(',').append(p.amountBaht()).append(',')
               .append(cell(p.staffUsername())).append("\r\n");
        }
        Files.writeString(file, csv, StandardCharsets.UTF_8);
    }
    /** แปลงข้อความเป็นช่อง CSV: ครอบด้วย " และเปลี่ยน " ภายในเป็น "" */
    private static String cell(String text) {
        // ข้อความที่ขึ้นต้นด้วย = + - @ Excel จะมองเป็นสูตร จึงเติม ' นำหน้าเพื่อให้เป็นข้อความธรรมดา
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
