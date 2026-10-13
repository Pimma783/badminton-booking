package com.badminton.repository;

import com.badminton.model.ClubData;
import java.io.IOException;

/**
 * ช่องทางอ่าน/เขียนข้อมูลของสโมสร — เป็น interface ตามข้อกำหนดโจทย์
 * <p>
 * ClubService รู้จักแค่ interface นี้ ไม่รู้ว่าข้อมูลเก็บที่ไหน ถ้าอนาคตเปลี่ยนไปใช้ฐานข้อมูล
 * ก็แค่เขียนคลาสใหม่ที่ implements ClubRepository โดยไม่ต้องแก้กติกาธุรกิจหรือหน้าจอ
 */
public interface ClubRepository {
    /** อ่านข้อมูลทั้งหมด ถ้ายังไม่เคยบันทึกให้คืนข้อมูลเริ่มต้น */
    ClubData load() throws IOException;
    /** บันทึกข้อมูลทั้งหมด ถ้าไม่สำเร็จต้องโยน IOException */
    void save(ClubData data) throws IOException;
}
