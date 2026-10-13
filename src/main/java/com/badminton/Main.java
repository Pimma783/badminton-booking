package com.badminton;

/**
 * คลาสเริ่มต้นโปรแกรม (main class) ที่ระบุใน pom.xml
 * <p>
 * แยกออกจาก BadmintonApp เพราะถ้าคลาสที่มี main สืบทอด javafx.application.Application โดยตรง
 * การรันไฟล์ JAR ผ่าน classpath จะแจ้งว่าไม่พบ JavaFX runtime
 */
public final class Main {
    public static void main(String[] args) { BadmintonApp.main(args); }
}
