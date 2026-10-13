package com.badminton.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * รายการเช่าอุปกรณ์ 1 ครั้ง คิดคู่ละ 50 บาท/ครั้ง/วัน
 *
 * @param id            เลขที่การเช่า เช่น R00001
 * @param memberId      รหัสสมาชิกผู้เช่า
 * @param memberName    ชื่อสมาชิก
 * @param type          ประเภทอุปกรณ์ (รองเท้า หรือ ไม้แบด)
 * @param quantity      จำนวนคู่
 * @param date          วันที่เช่า
 * @param amountBaht    ค่าเช่ารวม = จำนวนคู่ x 50 บาท
 * @param staffUsername พนักงานที่บันทึก
 */
public record Rental(String id, String memberId, String memberName, RentalType type,
                     int quantity, LocalDate date, long amountBaht,
                     String staffUsername) implements Serializable {}
