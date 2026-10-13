package com.badminton.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * เงินที่รับเข้า 1 รายการ ใช้ทำรายงานรายได้รายวัน/รายเดือน
 * <p>
 * นับตามวันเวลาที่รับเงิน ไม่ใช่วันที่ใช้สนาม เช่น วันนี้รับเงินจองสนามของสัปดาห์หน้า
 * จะนับเป็นรายได้ของวันนี้
 *
 * @param id            เลขที่การรับเงิน
 * @param memberId      รหัสสมาชิกที่จ่ายเงิน
 * @param memberName    ชื่อสมาชิก
 * @param type          ประเภทรายได้ (ค่าสนาม แพ็กเกจ เช่า ขายสินค้า)
 * @param description   รายละเอียดรายการ
 * @param amountBaht    จำนวนเงิน (บาท)
 * @param slipPath      ตำแหน่งสลิป (ว่างได้ถ้าเป็นการรับเงินสดหน้าร้าน)
 * @param staffUsername พนักงานที่รับเงิน
 * @param paidAt        วันเวลาที่รับเงิน
 */
public record Payment(String id, String memberId, String memberName, RevenueType type,
                      String description, long amountBaht, String slipPath,
                      String staffUsername, LocalDateTime paidAt) implements Serializable {}
