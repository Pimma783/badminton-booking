package com.badminton.repository;

import com.badminton.model.ClubData;
import java.io.*;
import java.nio.file.*;

/** เก็บข้อมูลลงไฟล์ data/club-data.bin ด้วย Java Serialization */
public final class FileClubRepository implements ClubRepository {
    private final Path file;
    public FileClubRepository(Path directory) { this.file = directory.resolve("club-data.bin"); }
    @Override public ClubData load() throws IOException {
        // เปิดโปรแกรมครั้งแรกยังไม่มีไฟล์ ให้เริ่มจากข้อมูลว่าง
        if (!Files.exists(file)) return ClubData.emptyClub();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            // ตัวกรองความปลอดภัย: ยอมให้อ่านเฉพาะคลาสของโปรแกรมนี้และคลาสพื้นฐานของ Java
            // และจำกัดขนาด/ความลึกของข้อมูล ป้องกันไฟล์ที่ถูกดัดแปลงมาโจมตีโปรแกรม
            input.setObjectInputFilter(info -> {
                if (info.depth() > 30 || info.references() > 1_000_000 || info.arrayLength() > 1_000_000)
                    return ObjectInputFilter.Status.REJECTED;
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                while (type.isArray()) type = type.getComponentType();
                String name = type.getName();
                return type.isPrimitive() || name.startsWith("com.badminton.model.")
                        || name.startsWith("java.lang.") || name.startsWith("java.time.")
                        || name.startsWith("java.util.")
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            return (ClubData) input.readObject();
        } catch (ClassNotFoundException | ClassCastException ex) {
            throw new IOException("ข้อมูลไม่ตรงกับเวอร์ชันโปรแกรม", ex);
        }
    }
    @Override public void save(ClubData data) throws IOException {
        Files.createDirectories(file.getParent());
        // เขียนลงไฟล์ชั่วคราวก่อน แล้วค่อยย้ายไปแทนไฟล์จริง
        // ถ้าไฟดับหรือดิสก์เต็มระหว่างเขียน ไฟล์ข้อมูลเดิมจะยังอยู่ครบ
        Path temporary = Files.createTempFile(file.getParent(), "club-", ".tmp");
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(data);
            }
            // ATOMIC_MOVE แทนที่ไฟล์ในขั้นตอนเดียว ถ้าระบบไฟล์ไม่รองรับก็ใช้การย้ายแบบปกติ
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
